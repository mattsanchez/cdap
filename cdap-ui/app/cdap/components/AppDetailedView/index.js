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

import React, {PropTypes, Component} from 'react';
import {objectQuery} from 'services/helpers';
import {MyAppApi} from 'api/app';
import ExploreTablesStore from 'services/ExploreTables/ExploreTablesStore';
import {fetchTables} from 'services/ExploreTables/ActionCreator';
import OverviewHeader from 'components/Overview/OverviewHeader';
import OverviewMetaSection from 'components/Overview/OverviewMetaSection';
import T from 'i18n-react';
import {MySearchApi} from 'api/search';
import isNil from 'lodash/isNil';
import isEmpty from 'lodash/isEmpty';
import NamespaceStore from 'services/NamespaceStore';
import {parseMetadata} from 'services/metadata-parser';
import AppDetailedViewTab from 'components/AppDetailedView/Tabs';
import shortid from 'shortid';
require('./AppDetailedView.scss');

export default class AppDetailedView extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entityDetail: objectQuery(this.props, 'location', 'state', 'entityDetail') || {
        programs: [],
        datasets: [],
        streams: [],
      },
      loading: true,
      entityMetadata: objectQuery(this.props, 'location', 'state', 'entityMetadata') || {},
      isInvalid: false
    };
  }
  componentWillMount() {
    let {namespace, appId} = this.props.params;
    if (!namespace) {
      namespace = NamespaceStore.getState().selectedNamespace;
    }
    ExploreTablesStore.dispatch(
      fetchTables(namespace)
    );

    if (this.state.entityDetail.programs.length === 0) {
      MyAppApi
        .get({
          namespace,
          appId
        })
        .subscribe(entityDetail => {
          let programs = entityDetail.programs.map(prog => {
            prog.uniqueId = shortid.generate();
            return prog;
          });
          let datasets = entityDetail.datasets.map(dataset => {
            dataset.entityId = {
              id: {
                instanceId: dataset.name
              },
              type: 'datasetinstance'
            };
            dataset.uniqueId = shortid.generate();
            return dataset;
          });
          let streams = entityDetail.streams.map(stream => {
            stream.entityId = {
              id: {
                streamName: stream.name
              },
              type: 'stream'
            };
            stream.uniqueId = shortid.generate();
            return stream;
          });
          entityDetail.streams = streams;
          entityDetail.datasets = datasets;
          entityDetail.programs = programs;
          this.setState({
            entityDetail
          });
        });
    }
    if (
      isNil(this.state.entityMetadata) ||
      isEmpty(this.state.entityMetadata)
    ) {
      // FIXME: This is NOT the right way. Need to figure out a way to be more efficient and correct.
      MySearchApi
        .search({
          namespace,
          query: this.props.params.appId
        })
        .map(res => res.results.map(parseMetadata))
        .subscribe(entityMetadata => {
          this.setState({
            entityMetadata: entityMetadata[0],
            loading: false
          });
        });
    }

    if (
      isNil(this.state.entityMetadata) ||
      isEmpty(this.state.entityMetadata) ||
      isNil(this.state.entity) ||
      isEmpty(this.state.entity)
    ) {
      this.setState({
        loading: true
      });
    }
  }
  render() {
    let title = this.state.entityDetail.isHydrator ?
      T.translate('commons.entity.cdap-data-pipeline.singular')
    :
      T.translate('commons.entity.application.singular');

    if (this.state.loading) {
      return (
        <div className="app-detailed-view">
          <div className="fa fa-spinner fa-spin fa-3x"></div>
        </div>
      );
    }
    return (
      <div className="app-detailed-view">
        <OverviewHeader
          icon="icon-fist"
          title={title}
        />
        <OverviewMetaSection entity={this.state.entityMetadata} />
        <AppDetailedViewTab
          params={this.props.params}
          pathname={this.props.location.pathname}
          entity={this.state.entityDetail}/
        >
      </div>
    );
  }

}

const entityDetailType = PropTypes.shape({
  artifact: PropTypes.shape({
    name: PropTypes.string,
    scope: PropTypes.string,
    version: PropTypes.string
  }),
  artifactVersion: PropTypes.string,
  configuration: PropTypes.string,
  // Need to expand on these
  datasets: PropTypes.arrayOf(PropTypes.object),
  streams: PropTypes.arrayOf(PropTypes.object),
  plugins: PropTypes.arrayOf(PropTypes.object),
  programs: PropTypes.arrayOf(PropTypes.object),
});


AppDetailedView.propTypes = {
  entity: PropTypes.object,
  params: PropTypes.shape({
    appId: PropTypes.string,
    namespace: PropTypes.string
  }),
  location: PropTypes.shape({
    hash: PropTypes.string,
    pathname: PropTypes.string,
    query: PropTypes.any,
    search: PropTypes.string,
    state: PropTypes.shape({
      entityDetail: entityDetailType,
      entityMetadata: PropTypes.object
    })
  })
};
