/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

App.WidgetWizardView = Em.View.extend(App.WizardMenuMixin, {

  templateName: require('templates/main/service/widgets/create/wizard'),

  /**
   * @type {App.Widget}
   */
  previewWidgetClass: function () {
    switch (this.get('controller.content.widgetType')) {
      case 'GRAPH':
        return App.GraphWidgetView.extend(App.WidgetPreviewMixin, {
          MOCK_VALUE: function () {
            var nowTime = App.dateTime();
            var mock = [];

            for (var i = 0; i < 240; i++) {
              mock.push([
                1,
                (nowTime + (15 * i))
              ])
            }
            return mock;
          }.property()
        });
      case 'TEMPLATE':
        return App.TemplateWidgetView.extend(App.WidgetPreviewMixin, {
          MOCK_VALUE: 50
        });
      case 'NUMBER':
        return App.NumberWidgetView.extend(App.WidgetPreviewMixin, {
          MOCK_VALUE: 50
        });
      case 'GAUGE':
        return App.GaugeWidgetView.extend(App.WidgetPreviewMixin, {
          MOCK_VALUE: 0.5
        });
      default:
        return Em.View;
    }
  }.property('controller.content.widgetType'),

  /**
   * Widget preview should be shown on 2nd step of wizard
   * @type {boolean}
   */
  showPreview: function () {
    return this.get('controller.currentStep') == "2";
  }.property('controller.currentStep')
});
