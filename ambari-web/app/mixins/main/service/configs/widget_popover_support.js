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

/**
 * Popover for configs widgets
 * Usage:
 * <code>
 *  didInsertElement: function () {
 *    this._super();
 *    this.initPopover();
 *  }
 *  </code>
 * @type {Em.Mixin}
 */
App.WidgetPopoverSupport = Em.Mixin.create({

  /**
   * Should popover be on the page
   * @type {boolean}
   */
  isPopoverEnabled: true,

  /**
   * Where popover should be displayed - top|left|right|bottom
   * @type {string}
   */
  popoverPlacement: function () {
    // popover to left if config is located at the right most sub-section of the right most section.
    var secCI = this.get('section.columnIndex');
    var secCS = this.get('section.columnSpan');
    var tabCols = this.get('tab.columns');
    var subsecCI = this.get('subSection.columnIndex');
    var subsecCS = this.get('subSection.columnSpan');
    var secCols = this.get('section.sectionColumns');

    return ((secCI + secCS == tabCols) && (subsecCI + subsecCS == secCols))? 'left' : 'right';
  }.property(),

  initPopover: function () {
    // if description for this config not exist, then no need to show popover
    if (this.get('isPopoverEnabled') !== 'false' && this.get('config.description')) {
      App.popover(this.$('.original-widget'), {
        title: Em.I18n.t('installer.controls.serviceConfigPopover.title').format(
          this.get('configLabel'),
          (this.get('config.displayName') == this.get('config.name')) ? '' : this.get('config.name')
        ),
        content: this.get('config.description'),
        placement: this.get('popoverPlacement'),
        trigger: 'hover'
      });
    }
  },

  willDestroyElement: function() {
    this.$().popover('destroy');
  }

});
