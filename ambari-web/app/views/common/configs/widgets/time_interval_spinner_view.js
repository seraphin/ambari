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

App.TimeIntervalSpinnerView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/time_interval_spinner'),
  classNames: ['widget-config', 'spinner-input-widget'],

  /**
   * @property isValid
   * @type {Boolean}
   */
  isValid: true,

  /**
   * @property disabled
   * @type {Boolean}
   */
  disabled: false,

  /**
   * @property valueIsChanged
   * @type {Boolean}
   */
  valueIsChanged: false,

  /**
   * Default property value in widget format.
   *
   * @property defaultValue
   * @type {Object[]}
   */
  defaultValue: null,

  /**
   * Maximum property value in widget format.
   *
   * @property maxValue
   * @type {Object[]}
   */
  maxValue: null,

  /**
   * Minimum property value in widget format.
   *
   * @property minValue
   * @type {Object[]}
   */
  minValue: null,

  /**
   * @TODO move it to unit conversion view mixin?
   * Map with maximum value per unit.
   *
   * @property timeMaxValueOverflow
   * @type {Object}
   */
  timeMaxValueOverflow: {
    milliseconds: 999,
    seconds: 59,
    minutes: 59,
    hours: 23,
    days: 365
  },

  didInsertElement: function () {
    Em.run.once(this, 'prepareContent');
    this._super();
    this.toggleWidgetState();
    this.initPopover();
  },

  /**
   * Content setter.
   * Affects to view attributes:
   *  @see propertyUnit
   *  @see defaultValue
   *  @see minValue
   *  @see maxValue
   *       content
   * @method prepareContent
   */
  prepareContent: function() {
    var property = this.get('config');

    this.setProperties({
      propertyUnit: property.get('stackConfigProperty.valueAttributes.unit'),
      minValue: this.generateWidgetValue(property.get('stackConfigProperty.valueAttributes.minimum')),
      maxValue: this.generateWidgetValue(property.get('stackConfigProperty.valueAttributes.maximum')),
      content: this.generateWidgetValue(property.get('value'))
    });
    this.parseIncrement();
  },

  /**
   * If <code>config.stackConfigProperty.valueAttributes.increment_step</code> exists, it should be used as increment
   * for controls. Also if it's value greater than maximum value for last unit, it (unit) should be disabled
   * @method parseIncrement
   */
  parseIncrement: function () {
    var property = this.get('config');
    var type = this.get('content.lastObject.type');
    var step = property.get('stackConfigProperty.valueAttributes.increment_step');
    if (step) {
      step = this.convertValue(step, property.get('stackConfigProperty.valueAttributes.unit'), type);
      this.set('content.lastObject.incrementStep', step);
      if (step >  Em.get(this, 'timeMaxValueOverflow.' + type)) {
        this.set('content.lastObject.enabled', false);
      }
    }
  },

  /**
   * @TODO move it to unit conversion view mixin?
   * Generate formatted value for widget.
   *
   * @param {String|Number} value
   * @returns {Object[]}
   * @method generateWidgetValue
   */
  generateWidgetValue: function(value) {
    return this.widgetValueByConfigAttributes(value, true).map(function(item) {
      item.label = Em.I18n.t('common.' + item.type);
      item.minValue = 0;
      item.maxValue = Em.get(this, 'timeMaxValueOverflow.' + item.type);
      item.incrementStep = 1;
      item.enabled = true;
      item.invertOnOverflow = true;
      return item;
    }, this);
  },

  /**
   * Subscribe for value changes
   * @method valueObserver
   */
  valueObserver: function() {
    if (!this.get('content')) return;
    Em.run.once(this, 'valueObserverCallback');
  }.observes('content.@each.value'),

  valueObserverCallback: function() {
    this.checkModified();
    this.checkErrors();
    this.setConfigValue();
  },

  /**
   * Check for property modification.
   * @method checkModified
   */
  checkModified: function() {
    this.set('valueIsChanged', this.configValueByWidget(this.get('content')) != parseInt(this.get('config.defaultValue')));
  },

  /**
   * Check for validation errors like minimum or maximum required value
   * @method checkErrors
   */
  checkErrors: function() {
    var convertedValue = this.configValueByWidget(this.get('content'));
    var errorMessage = false;
    if (convertedValue < parseInt(this.get('config.stackConfigProperty.valueAttributes.minimum'))) {
      errorMessage = Em.I18n.t('number.validate.lessThanMinumum').format(this.dateToText(this.get('minValue')));
    }
    else if (convertedValue > parseInt(this.get('config.stackConfigProperty.valueAttributes.maximum'))) {
      errorMessage = Em.I18n.t('number.validate.moreThanMaximum').format(this.dateToText(this.get('maxValue')));
    }
    this.setProperties({
      isValid: !errorMessage,
      errorMessage: errorMessage
    });
    this.get('config').set('errorMessage', errorMessage);
  },

  /**
   * set appropriate attribute for configProperty model
   * @method setConfigValue
   */
  setConfigValue: function() {
    this.set('config.value', this.configValueByWidget(this.get('content')));
  },

  /**
   * Convert value to readable format using widget value.
   *
   * @param {Object[]} widgetFormatValue - value formatted for widget @see convertToWidgetUnits
   * @return {String}
   * @method dateToText
   */
  dateToText: function(widgetFormatValue) {
    return widgetFormatValue.map(function(item) {
      if (Em.get(item, 'value') > 0) {
        return Em.get(item, 'value') + ' ' + Em.get(item, 'label');
      }
      else {
        return null;
      }
    }).compact().join(' ');
  },

  /**
   * Restore value to default.
   * @method restoreValue
   */
  restoreValue: function() {
    this._super();
    this.set('content', this.generateWidgetValue(this.get('config.defaultValue')));
    this.parseIncrement();
  }
});
