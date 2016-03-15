/*
 * Copyright © 2016 Cask Data, Inc.
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

package co.cask.cdap.etl.planner;

import co.cask.cdap.etl.proto.Connection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

/**
 */
public class DagTest {

  @Test
  public void testLinearize() {
    // n1 -> n2 -> n3 -> n4
    Dag dag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"), new Connection("n2", "n3"), new Connection("n3", "n4")));
    Assert.assertEquals(ImmutableList.of("n1", "n2", "n3", "n4"), dag.linearize());

    /*
             |--- n2 ---|
        n1 --|          |-- n4
             |--- n3 ---|
     */
    dag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"),
      new Connection("n1", "n3"),
      new Connection("n2", "n4"),
      new Connection("n3", "n4")));
    // could be n1 -> n2 -> n3 -> n4
    // or it could be n1 -> n3 -> n2 -> n4
    List<String> linearized = dag.linearize();
    Assert.assertEquals("n1", linearized.get(0));
    Assert.assertEquals("n4", linearized.get(3));
    assertBefore(linearized, "n1", "n2");
    assertBefore(linearized, "n1", "n3");

    /*
        n1 --|
             |--- n3
        n2 --|
     */
    dag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n3"),
      new Connection("n2", "n3")));
    // could be n1 -> n2 -> n3
    // or it could be n2 -> n1 -> n3
    linearized = dag.linearize();
    Assert.assertEquals("n3", linearized.get(2));
    assertBefore(linearized, "n1", "n3");
    assertBefore(linearized, "n2", "n3");

    /*
                                     |--- n3
             |--- n2 ----------------|
        n1 --|       |               |--- n5
             |--------- n4 ----------|
             |              |        |
             |---------------- n6 ---|

        vertical arrows are pointing down
     */
    dag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"),
      new Connection("n1", "n4"),
      new Connection("n1", "n6"),
      new Connection("n2", "n3"),
      new Connection("n2", "n4"),
      new Connection("n2", "n5"),
      new Connection("n4", "n3"),
      new Connection("n4", "n5"),
      new Connection("n4", "n6"),
      new Connection("n6", "n3"),
      new Connection("n6", "n5")));
    linearized = dag.linearize();
    Assert.assertEquals("n1", linearized.get(0));
    Assert.assertEquals("n2", linearized.get(1));
    Assert.assertEquals("n4", linearized.get(2));
    Assert.assertEquals("n6", linearized.get(3));
    assertBefore(linearized, "n6", "n3");
    assertBefore(linearized, "n6", "n5");
  }

  private void assertBefore(List<String> list, String a, String b) {
    int aIndex = list.indexOf(a);
    int bIndex = list.indexOf(b);
    Assert.assertTrue(aIndex < bIndex);
  }

  @Test(expected = IllegalStateException.class)
  public void testCycle() {
    /*
             |---> n2 ----|
             |      ^     v
        n1 --|      |     n3 --> n5
             |---> n4 <---|
     */
    Dag dag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"),
      new Connection("n2", "n3"),
      new Connection("n3", "n4"),
      new Connection("n4", "n2"),
      new Connection("n3", "n5")));
    dag.linearize();
  }

  @Test
  public void testRemoveSource() {
    /*
             |--- n2 ---|
        n1 --|          |-- n4
             |--- n3 ---|
     */
    Dag dag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"),
      new Connection("n1", "n3"),
      new Connection("n2", "n4"),
      new Connection("n3", "n4")));
    Assert.assertEquals("n1", dag.removeSource());
    Assert.assertEquals(ImmutableSet.of("n2", "n3"), dag.getSources());

    Set<String> removed = ImmutableSet.of(dag.removeSource(), dag.removeSource());
    Assert.assertEquals(ImmutableSet.of("n2", "n3"), removed);
    Assert.assertEquals(ImmutableSet.of("n4"), dag.getSources());
    Assert.assertEquals("n4", dag.removeSource());
    Assert.assertTrue(dag.getSources().isEmpty());
    Assert.assertTrue(dag.getSinks().isEmpty());
    Assert.assertNull(dag.removeSource());
  }

  @Test
  public void testIslands() {
    /*
        n1 -- n2

        n3 -- n4
     */
    try {
      Dag.fromConnections(ImmutableSet.of(
        new Connection("n1", "n2"),
        new Connection("n3", "n4")));
      Assert.fail();
    } catch (IllegalStateException e) {
      // expected
      Assert.assertTrue(e.getMessage().startsWith("Invalid DAG. There is an island"));
    }

    /*
        n1 -- n2
              |
              v
        n3 -- n4
              ^
        n5----|   n6 -- n7
     */
    try {
      Dag.fromConnections(ImmutableSet.of(
        new Connection("n1", "n2"),
        new Connection("n2", "n4"),
        new Connection("n3", "n4"),
        new Connection("n5", "n4"),
        new Connection("n6", "n7")));
      Assert.fail();
    } catch (IllegalStateException e) {
      // expected
      Assert.assertTrue(e.getMessage().startsWith("Invalid DAG. There is an island"));
    }
  }

  @Test
  public void testAccessibleFrom() {
    /*
        n1 -- n2
              |
              v
        n3 -- n4 --- n8
              ^
              |
        n5-------- n6 -- n7
     */
    Dag dag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"),
      new Connection("n2", "n4"),
      new Connection("n3", "n4"),
      new Connection("n4", "n8"),
      new Connection("n5", "n4"),
      new Connection("n5", "n6"),
      new Connection("n6", "n7")));
    Assert.assertEquals(ImmutableSet.of("n1", "n2", "n4", "n8"), dag.accessibleFrom("n1"));
    Assert.assertEquals(ImmutableSet.of("n2", "n4", "n8"), dag.accessibleFrom("n2"));
    Assert.assertEquals(ImmutableSet.of("n3", "n4", "n8"), dag.accessibleFrom("n3"));
    Assert.assertEquals(ImmutableSet.of("n4", "n8"), dag.accessibleFrom("n4"));
    Assert.assertEquals(ImmutableSet.of("n5", "n4", "n8", "n6", "n7"), dag.accessibleFrom("n5"));
    Assert.assertEquals(ImmutableSet.of("n6", "n7"), dag.accessibleFrom("n6"));
    Assert.assertEquals(ImmutableSet.of("n7"), dag.accessibleFrom("n7"));
    Assert.assertEquals(ImmutableSet.of("n8"), dag.accessibleFrom("n8"));

    // this stop node isn't accessible from n5 anyway, shouldn't have any effect
    Assert.assertEquals(ImmutableSet.of("n5", "n4", "n8", "n6", "n7"),
                        dag.accessibleFrom("n5", ImmutableSet.of("n1")));
    // these stop nodes cut off all paths, though the stop nodes themselves should show up in accessible set
    Assert.assertEquals(ImmutableSet.of("n5", "n4", "n6"), dag.accessibleFrom("n5", ImmutableSet.of("n4", "n6")));
    // these stop nodes cut off some paths
    Assert.assertEquals(ImmutableSet.of("n5", "n4", "n6", "n7"),
                        dag.accessibleFrom("n5", ImmutableSet.of("n4")));
    // these stop nodes cut off some paths
    Assert.assertEquals(ImmutableSet.of("n5", "n4", "n6", "n8"),
                        dag.accessibleFrom("n5", ImmutableSet.of("n6", "n1")));
  }

  @Test
  public void testSubset() {
    /*
        n1 -- n2
              |
              v
        n3 -- n4 --- n8
              ^
              |
        n5-------- n6 -- n7
     */
    Dag fulldag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"),
      new Connection("n2", "n4"),
      new Connection("n3", "n4"),
      new Connection("n4", "n8"),
      new Connection("n5", "n4"),
      new Connection("n5", "n6"),
      new Connection("n6", "n7")));

    Dag expected = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"),
      new Connection("n2", "n4"),
      new Connection("n4", "n8")));
    Dag actual = fulldag.subsetFrom("n1");
    Assert.assertEquals(expected, actual);

    expected = Dag.fromConnections(ImmutableSet.of(
      new Connection("n2", "n4"),
      new Connection("n4", "n8")));
    actual = fulldag.subsetFrom("n2");
    Assert.assertEquals(expected, actual);

    expected = Dag.fromConnections(ImmutableSet.of(
      new Connection("n3", "n4"),
      new Connection("n4", "n8")));
    actual = fulldag.subsetFrom("n3");
    Assert.assertEquals(expected, actual);

    expected = Dag.fromConnections(ImmutableSet.of(
      new Connection("n4", "n8"),
      new Connection("n5", "n4"),
      new Connection("n5", "n6"),
      new Connection("n6", "n7")));
    actual = fulldag.subsetFrom("n5");
    Assert.assertEquals(expected, actual);

    expected = Dag.fromConnections(ImmutableSet.of(
      new Connection("n6", "n7")));
    actual = fulldag.subsetFrom("n6");
    Assert.assertEquals(expected, actual);

    // test subsets with stop nodes
    expected = Dag.fromConnections(ImmutableSet.of(new Connection("n1", "n2")));
    actual = fulldag.subsetFrom("n1", ImmutableSet.of("n2"));
    Assert.assertEquals(expected, actual);

    expected = Dag.fromConnections(ImmutableSet.of(
      new Connection("n5", "n4"),
      new Connection("n5", "n6")));
    actual = fulldag.subsetFrom("n5", ImmutableSet.of("n4", "n6"));
    Assert.assertEquals(expected, actual);


    /*
             |--- n2 ----------|
             |                 |                              |-- n10
        n1 --|--- n3 --- n5 ---|--- n6 --- n7 --- n8 --- n9 --|
             |                 |                              |-- n11
             |--- n4 ----------|

     */
    fulldag = Dag.fromConnections(ImmutableSet.of(
      new Connection("n1", "n2"),
      new Connection("n1", "n3"),
      new Connection("n1", "n4"),
      new Connection("n2", "n6"),
      new Connection("n3", "n5"),
      new Connection("n4", "n6"),
      new Connection("n5", "n6"),
      new Connection("n6", "n7"),
      new Connection("n7", "n8"),
      new Connection("n8", "n9"),
      new Connection("n9", "n10"),
      new Connection("n9", "n11")));

    expected = Dag.fromConnections(ImmutableSet.of(
      new Connection("n3", "n5"),
      new Connection("n5", "n6"),
      new Connection("n6", "n7"),
      new Connection("n7", "n8"),
      new Connection("n8", "n9")));
    actual = fulldag.subsetFrom("n3", ImmutableSet.of("n4", "n9"));
    Assert.assertEquals(expected, actual);

    expected = Dag.fromConnections(ImmutableSet.of(
      new Connection("n2", "n6"),
      new Connection("n6", "n7"),
      new Connection("n7", "n8")));
    actual = fulldag.subsetFrom("n2", ImmutableSet.of("n4", "n8", "n1"));
    Assert.assertEquals(expected, actual);
  }
}