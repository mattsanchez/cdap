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

import React, {Component, PropTypes} from 'react';
import WranglerStore from 'wrangler/components/Wrangler/Store/WranglerStore';
import WranglerActions from 'wrangler/components/Wrangler/Store/WranglerActions';
import classnames from 'classnames';
import T from 'i18n-react';
require('./WrangleHistory.scss');

export default class WrangleHistory extends Component {
  constructor(props) {
    super(props);

    this.state = {
      showHistory: false
    };

    this.toggleShowHistory = this.toggleShowHistory.bind(this);
  }

  toggleShowHistory() {
    this.setState({showHistory: !this.state.showHistory});
  }

  deleteHistory(index) {
    WranglerStore.dispatch({
      type: WranglerActions.deleteHistory,
      payload: {
        index
      }
    });
  }

  renderHistory() {
    if (!this.state.showHistory) { return null; }

    return (
      <div
        className="history-list"
        onClick={e => e.stopPropagation()}
      >
        {
          this.props.historyArray.map((history) => {
            return (
              <div
                className="history-row"
                key={history.id}
              >
                <span>
                  <span>{T.translate(`features.Wrangler.Actions.${history.action}`)}</span>
                  <span>: {history.payload.activeColumn}</span>
                  {/* Commenting out the delete history button since it may come back */}
                  {/* <span
                    className="fa fa-times-circle float-xs-right history-delete"
                    onClick={this.deleteHistory.bind(this, index)}
                  />*/}
                </span>
              </div>
            );
          })
        }
      </div>
    );
  }

  render() {
    return (
      <div className="wrangler-history">
        <div
          className="transform-item"
          onClick={this.toggleShowHistory}
        >
          <span className="fa fa-list-ol" />
          <span className="transform-item-text">
            {T.translate('features.Wrangler.history')}
          </span>
          <span className={classnames('fa float-xs-right', {
            'fa-chevron-down': !this.state.showHistory,
            'fa-chevron-up': this.state.showHistory
          })} />
        </div>

        {this.renderHistory()}
      </div>
    );
  }
}

WrangleHistory.defaultProps = {
  historyArray: []
};

WrangleHistory.propTypes = {
  historyArray: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string,
    action: PropTypes.string,
  }))
};
