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

App.Section = DS.Model.extend({
  id: DS.attr('string'),
  name: DS.attr('string'),
  displayName: DS.attr('string'),
  rowIndex: DS.attr('number', {defaultValue: 1}),
  rowSpan: DS.attr('number', {defaultValue: 1}),
  columnIndex: DS.attr('number', {defaultValue: 1}),
  columnSpan: DS.attr('number', {defaultValue: 1}),
  sectionColumns: DS.attr('number', {defaultValue: 1}),
  sectionRows: DS.attr('number', {defaultValue: 1}),
  subSections: DS.hasMany('App.SubSection'),
  tab: DS.belongsTo('App.Tab')
});


App.Section.FIXTURES = [];

