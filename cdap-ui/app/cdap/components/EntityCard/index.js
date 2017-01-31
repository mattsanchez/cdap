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
import Card from '../Card';
import EntityCardHeader from './EntityCardHeader';
import ApplicationMetrics from './ApplicationMetrics';
import ArtifactMetrics from './ArtifactMetrics';
import DatasetMetrics from './DatasetMetrics';
import ProgramMetrics from './ProgramMetrics';
import StreamMetrics from './StreamMetrics';
import classnames from 'classnames';
import FastActions from 'components/EntityCard/FastActions';
import JumpButton from 'components/JumpButton';

require('./EntityCard.scss');

export default class EntityCard extends Component {
  constructor(props) {
    super(props);
    this.state = {
      successMessage: false
    };
    this.cardRef = null;
    this.onSuccess = this.onSuccess.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.activeEntity !== this.props.entity.uniqueId) {
      this.setState({
        overviewMode: false
      });
    }
  }

  onSuccess() {
    this.setState({successMessage: true});
    setTimeout(() => {
      this.setState({successMessage: false});
    }, 3000);
  }

  renderEntityStatus() {
    switch (this.props.entity.type) {
      case 'application':
        return <ApplicationMetrics entity={this.props.entity} />;
      case 'artifact':
        return <ArtifactMetrics entity={this.props.entity} />;
      case 'datasetinstance':
        return <DatasetMetrics entity={this.props.entity} />;
      case 'program':
        return <ProgramMetrics entity={this.props.entity} poll={this.props.poll}/>;
      case 'stream':
        return <StreamMetrics entity={this.props.entity} />;
      case 'view':
        return null;
    }
  }

  renderJumpButton() {
    const entity = this.props.entity;
    if (['datasetinstance', 'stream'].indexOf(entity.type) === -1 && !entity.isHydrator) {
      return null;
    }

    return (
      <div className="jump-button-container text-xs-center float-xs-right">
        <JumpButton
          entity={this.props.entity}
        />
      </div>
    );
  }

  onClick() {
    if (this.props.entity.type === 'artifact' || this.props.entity.type === 'program') {
      return;
    }

    if (this.props.onClick) {
      this.props.onClick();
    }
  }

  render() {
    if (!this.props.entity) {
      return null;
    }
    const header = (
      <EntityCardHeader
        onClick={this.onClick.bind(this)}
        className={this.props.entity.isHydrator ? 'datapipeline' : this.props.entity.type}
        entity={this.props.entity}
        systemTags={this.props.entity.metadata.metadata.SYSTEM.tags}
        successMessage={this.state.successMessage}
      />
    );
    return (
      <div
        className={
          classnames(
            'entity-cards',
            this.props.entity.isHydrator ? 'datapipeline' : this.props.entity.type,
            this.props.className)
        }
        ref={(ref) => this.cardRef = ref}
      >
        <Card
          header={header}
          id={
            classnames(
              this.props.entity.isHydrator ?
              `entity-cards-datapipeline` :
              `entity-cards-${this.props.entity.type}`
            )
          }
        >
          <div className="entity-information clearfix">
            <div className="entity-id-container">
              <h4
                className={classnames({'with-version': this.props.entity.version})}
              >
                {this.props.entity.id}
              </h4>
              <small>{
                  this.props.entity.version ?
                    this.props.entity.version
                  :
                    '1.0.0'
                }</small>
            </div>
            {this.renderJumpButton()}
          </div>

          {this.renderEntityStatus()}

          <div className="fast-actions-container">
            <FastActions
              entity={this.props.entity}
              onUpdate={this.props.onUpdate}
              onSuccess={this.onSuccess}
            />
          </div>
        </Card>
      </div>
    );
  }
}

EntityCard.defaultProps = {
  onClick: () => {},
  poll: false
};

EntityCard.propTypes = {
  entity: PropTypes.object,
  poll: PropTypes.bool,
  onUpdate: PropTypes.func,
  className: PropTypes.string,
  onClick: PropTypes.func,
  activeEntity: PropTypes.string
};
