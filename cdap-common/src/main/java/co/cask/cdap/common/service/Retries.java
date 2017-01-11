/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.common.service;

import co.cask.cdap.api.Predicate;
import co.cask.cdap.api.retry.RetriesExhaustedException;
import co.cask.cdap.api.retry.RetryableException;
import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Utilities to perform logic with retries.
 */
public class Retries {
  private static final Logger LOG = LoggerFactory.getLogger(Retries.class);
  private static final Predicate<Throwable> DEFAULT_PREDICATE = new Predicate<Throwable>() {
    @Override
    public boolean apply(Throwable throwable) {
      return throwable instanceof RetryableException;
    }
  };

  private Retries() {

  }

  /**
   * The same as calling {@link #supplyWithRetries(Supplier, RetryStrategy, Predicate)} where a retryable failure
   * is defined as a {@link RetryableException}.
   *
   * @param supplier the callable to run
   * @param retryStrategy the retry strategy to use if the supplier throws a {@link RetryableException}
   * @param <T> the type of object returned by the supplier
   * @return the return value of the supplier
   * @throws InterruptedException if interrupted while sleeping between retries
   * @throws RetriesExhaustedException if the supplier failed in a way that is retryable, but the retry strategy
   *   aborted the operation. The exception cause will be the original throwable thrown by the callable.
   */
  public static <T> T supplyWithRetries(Supplier<T> supplier, RetryStrategy retryStrategy) throws InterruptedException {
    return supplyWithRetries(supplier, retryStrategy, DEFAULT_PREDICATE);
  }

  /**
   * Executes {@link Supplier#get()}, retrying the call if it throws something retryable. This is similar to
   * {@link #callWithRetries(Callable, RetryStrategy, Predicate)}, except it will simply re-throw any non-retryable
   * exception instead of wrapping it in an ExecutionException. If you need to run logic that throws a
   * checked exception, use {@link #callWithRetries(Callable, RetryStrategy, Predicate)} instead.
   *
   * @param supplier the callable to run
   * @param retryStrategy the retry strategy to use if the supplier fails in a retryable way
   * @param isRetryable predicate to determine whether the supplier failure is retryable or not
   * @param <T> the type of object returned by the supplier
   * @return the return value of the supplier
   * @throws InterruptedException if interrupted while sleeping between retries
   * @throws RetriesExhaustedException if the supplier failed in a way that is retryable, but the retry strategy
   *   aborted the operation. The exception cause will be the original throwable thrown by the callable.
   */
  public static <T> T supplyWithRetries(Supplier<T> supplier, RetryStrategy retryStrategy,
                                        Predicate<Throwable> isRetryable) throws InterruptedException {
    int failures = 0;
    long startTime = System.currentTimeMillis();
    while (true) {
      try {
        return supplier.get();
      } catch (Throwable t) {
        if (!isRetryable.apply(t)) {
          throw t;
        }

        long retryTime = retryStrategy.nextRetry(++failures, startTime);
        if (retryTime < 0) {
          throw new RetriesExhaustedException(t);
        }

        LOG.debug("Call failed, retrying again after {} ms.", retryTime, t);
        TimeUnit.MILLISECONDS.sleep(retryTime);
      }
    }
  }

  /**
   * The same as calling {@link #callWithRetries(Callable, RetryStrategy, Predicate)} where a retryable failure
   * is defined as a {@link RetryableException}.
   *
   * @param callable the callable to run
   * @param retryStrategy the retry strategy to use if the callable throws a {@link RetryableException}
   * @param <T> the type of object returned by the callable
   * @return the return value of the callable
   * @throws ExecutionException if the callable failed in a way that is not retryable. The exception cause will
   *   be the original throwable thrown by the callable
   * @throws RetriesExhaustedException if the callable failed in a way that is retryable, but the retry strategy
   *   aborted the operation. The exception cause will be the original throwable thrown by the callable.
   * @throws InterruptedException if interrupted while sleeping between retries
   */
  public static <T> T callWithRetries(Callable<T> callable,
                                      RetryStrategy retryStrategy) throws ExecutionException, InterruptedException {
    return callWithRetries(callable, retryStrategy, DEFAULT_PREDICATE);
  }

  /**
   * Executes a {@link Callable}, retrying the call if it throws something retryable.
   *
   * @param callable the callable to run
   * @param retryStrategy the retry strategy to use if the callable fails in a retryable way
   * @param isRetryable predicate to determine whether the callable failure is retryable or not
   * @param <T> the type of object returned by the callable
   * @return the return value of the callable
   * @throws ExecutionException if the callable failed in a way that is not retryable. The exception cause will
   *   be the original throwable thrown by the callable
   * @throws RetriesExhaustedException if the callable failed in a way that is retryable, but the retry strategy
   *   aborted the operation. The exception cause will be the original throwable thrown by the callable.
   * @throws InterruptedException if interrupted while sleeping between retries
   */
  public static <T> T callWithRetries(Callable<T> callable, RetryStrategy retryStrategy,
                                      Predicate<Throwable> isRetryable)
    throws ExecutionException, InterruptedException {

    int failures = 0;
    long startTime = System.currentTimeMillis();
    while (true) {
      try {
        return callable.call();
      } catch (Throwable t) {
        if (!isRetryable.apply(t)) {
          throw new ExecutionException(t);
        }

        long retryTime = retryStrategy.nextRetry(++failures, startTime);
        if (retryTime < 0) {
          throw new RetriesExhaustedException(t);
        }

        LOG.debug("Call failed, retrying again after {} ms.", retryTime, t);
        TimeUnit.MILLISECONDS.sleep(retryTime);
      }
    }
  }
}
