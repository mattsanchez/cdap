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

import React, { Component, PropTypes } from 'react';
import OneStepDeployConfig from 'services/WizardConfigs/OneStepDeployConfig';
import OneStepDeployStore from 'services/WizardStores/OneStepDeploy/OneStepDeployStore';
import WizardModal from 'components/WizardModal';
import Wizard from 'components/Wizard';
import T from 'i18n-react';

export default class OneStepDeployWizard extends Component {
  constructor(props) {
    super(props);

    this.state = {
      showWizard: this.props.isOpen,
    };

    this.publishApp = this.publishApp.bind(this);
  }

  toggleWizard(returnResult) {
    if (this.state.showWizard) {
      this.props.onClose(returnResult);
    }
    this.setState({
      showWizard: !this.state.showWizard
    });
  }

  publishApp() {
    return this.props.onPublish();
  }

  render() {
    let input = this.props.input;
    let pkg = input.package || {};

    const getWizardContent = () => {
      return (
        <Wizard
          wizardConfig={OneStepDeployConfig}
          wizardType="OneStepDeploy"
          store={OneStepDeployStore}
          onSubmit={this.publishApp.bind(this)}
          onClose={this.toggleWizard.bind(this)}
        />
      );
    };

    let wizardModalTitle = (pkg.label ? pkg.label + " | " : '') + T.translate('features.Wizard.OneStepDeploy.headerlabel');
    return (
      <WizardModal
        title={wizardModalTitle}
        isOpen={this.state.showWizard}
        toggle={this.toggleWizard.bind(this, false)}
        className="one-step-deploy-wizard"
      >
        {getWizardContent()}
      </WizardModal>
    );
  }
}

OneStepDeployWizard.propTypes = {
  isOpen: PropTypes.bool,
  input: PropTypes.any,
  onClose: PropTypes.func,
  onPublish: PropTypes.func
};
