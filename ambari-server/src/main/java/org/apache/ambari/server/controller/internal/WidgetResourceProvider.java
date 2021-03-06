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
package org.apache.ambari.server.controller.internal;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for repository versions resources.
 */
@StaticallyInject
public class WidgetResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String WIDGET_ID_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "id");
  public static final String WIDGET_CLUSTER_NAME_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "cluster_name");
  public static final String WIDGET_WIDGET_NAME_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "widget_name");
  public static final String WIDGET_DISPLAY_NAME_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "display_name");
  public static final String WIDGET_WIDGET_TYPE_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "widget_type");
  public static final String WIDGET_TIME_CREATED_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "time_created");
  public static final String WIDGET_AUTHOR_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "author");
  public static final String WIDGET_DESCRIPTION_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "description");
  public static final String WIDGET_SCOPE_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "scope");
  public static final String WIDGET_METRICS_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "metrics");
  public static final String WIDGET_VALUES_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "values");
  public static final String WIDGET_PROPERTIES_PROPERTY_ID                 = PropertyHelper.getPropertyId("Widgets", "properties");
  public static enum SCOPE {
    CLUSTER,
    USER
  }

  @SuppressWarnings("serial")
  private static Set<String> pkPropertyIds = new HashSet<String>() {
    {
      add(WIDGET_ID_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  public static Set<String> propertyIds = new HashSet<String>() {
    {
      add(WIDGET_ID_PROPERTY_ID);
      add(WIDGET_WIDGET_NAME_PROPERTY_ID);
      add(WIDGET_DISPLAY_NAME_PROPERTY_ID);
      add(WIDGET_WIDGET_TYPE_PROPERTY_ID);
      add(WIDGET_TIME_CREATED_PROPERTY_ID);
      add(WIDGET_CLUSTER_NAME_PROPERTY_ID);
      add(WIDGET_AUTHOR_PROPERTY_ID);
      add(WIDGET_DESCRIPTION_PROPERTY_ID);
      add(WIDGET_SCOPE_PROPERTY_ID);
      add(WIDGET_METRICS_PROPERTY_ID);
      add(WIDGET_VALUES_PROPERTY_ID);
      add(WIDGET_PROPERTIES_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  public static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Type.Widget, WIDGET_ID_PROPERTY_ID);
      put(Type.Cluster, WIDGET_CLUSTER_NAME_PROPERTY_ID);
      put(Type.User, WIDGET_AUTHOR_PROPERTY_ID);
    }
  };

  @Inject
  private static WidgetDAO widgetDAO;

  @Inject
  private static Gson gson;

  /**
   * Create a new resource provider.
   *
   */
  public WidgetResourceProvider(AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    for (final Map<String, Object> properties : request.getProperties()) {
      createResources(new Command<Void>() {

        @Override
        public Void invoke() throws AmbariException {
          final String[] requiredProperties = {
              WIDGET_CLUSTER_NAME_PROPERTY_ID,
              WIDGET_WIDGET_NAME_PROPERTY_ID,
              WIDGET_DISPLAY_NAME_PROPERTY_ID,
              WIDGET_WIDGET_TYPE_PROPERTY_ID,
              WIDGET_SCOPE_PROPERTY_ID
          };
          for (String propertyName: requiredProperties) {
            if (properties.get(propertyName) == null) {
              throw new AmbariException("Property " + propertyName + " should be provided");
            }
          }
          final WidgetEntity entity = new WidgetEntity();
          String clusterName = properties.get(WIDGET_CLUSTER_NAME_PROPERTY_ID).toString();
          String scope = properties.get(WIDGET_SCOPE_PROPERTY_ID).toString();

          if (!isScopeAllowedForUser(scope)) {
            throw new AmbariException("Only cluster operator can create widgets with cluster scope");
          }

          entity.setDisplayName(properties.get(WIDGET_DISPLAY_NAME_PROPERTY_ID).toString());
          entity.setWidgetName(properties.get(WIDGET_WIDGET_NAME_PROPERTY_ID).toString());
          entity.setWidgetType(properties.get(WIDGET_WIDGET_TYPE_PROPERTY_ID).toString());
          entity.setClusterId(getManagementController().getClusters().getCluster(clusterName).getClusterId());
          entity.setDisplayName(properties.get(WIDGET_DISPLAY_NAME_PROPERTY_ID).toString());
          entity.setScope(scope);

          String metrics = (properties.containsKey(WIDGET_METRICS_PROPERTY_ID)) ?
                  gson.toJson(properties.get(WIDGET_METRICS_PROPERTY_ID)) : null;
          entity.setMetrics(metrics);

          entity.setAuthor(getAuthorName(properties));

          String description = (properties.containsKey(WIDGET_DESCRIPTION_PROPERTY_ID)) ?
                  properties.get(WIDGET_DESCRIPTION_PROPERTY_ID).toString() : null;
          entity.setDescription(description);

          String values = (properties.containsKey(WIDGET_VALUES_PROPERTY_ID)) ?
                  gson.toJson(properties.get(WIDGET_VALUES_PROPERTY_ID)) : null;
          entity.setWidgetValues(values);

          Map<String, Object> widgetPropertiesMap = new HashMap<String, Object>();
          for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (PropertyHelper.getPropertyCategory(entry.getKey()).equals(WIDGET_PROPERTIES_PROPERTY_ID)) {
              widgetPropertiesMap.put(PropertyHelper.getPropertyName(entry.getKey()), entry.getValue());
            }
          }

          String widgetProperties = (widgetPropertiesMap.isEmpty()) ?
                  null : gson.toJson(widgetPropertiesMap);
          entity.setProperties(widgetProperties);

          widgetDAO.create(entity);
          notifyCreate(Type.Widget, request);
          return null;
        }
      });
    }

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<Resource>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    List<WidgetEntity> requestedEntities = new ArrayList<WidgetEntity>();

    for (Map<String, Object> propertyMap: propertyMaps) {
      if (propertyMap.get(WIDGET_ID_PROPERTY_ID) != null) {
        final Long id;
        try {
          id = Long.parseLong(propertyMap.get(WIDGET_ID_PROPERTY_ID).toString());
        } catch (Exception ex) {
          throw new SystemException("WidgetLayout should have numerical id");
        }
        final WidgetEntity entity = widgetDAO.findById(id);
        if (entity == null) {
          throw new NoSuchResourceException("WidgetLayout with id " + id + " does not exists");
        }
        requestedEntities.add(entity);
      } else {
        requestedEntities.addAll(widgetDAO.findAll());
      }
    }

    for (WidgetEntity entity: requestedEntities) {
      final Resource resource = new ResourceImpl(Type.Widget);
      resource.setProperty(WIDGET_ID_PROPERTY_ID, entity.getId());
      resource.setProperty(WIDGET_WIDGET_NAME_PROPERTY_ID, entity.getWidgetName());
      resource.setProperty(WIDGET_WIDGET_TYPE_PROPERTY_ID, entity.getWidgetType());
      setResourceProperty(resource, WIDGET_METRICS_PROPERTY_ID, gson.fromJson(entity.getMetrics(), ArrayList.class), requestedIds);
      setResourceProperty(resource, WIDGET_TIME_CREATED_PROPERTY_ID, entity.getTimeCreated(), requestedIds);
      resource.setProperty(WIDGET_AUTHOR_PROPERTY_ID, entity.getAuthor());
      setResourceProperty(resource, WIDGET_DESCRIPTION_PROPERTY_ID, entity.getDescription(), requestedIds);
      resource.setProperty(WIDGET_DISPLAY_NAME_PROPERTY_ID, entity.getDisplayName());
      resource.setProperty(WIDGET_SCOPE_PROPERTY_ID, entity.getScope());
      setResourceProperty(resource, WIDGET_VALUES_PROPERTY_ID, gson.fromJson(entity.getWidgetValues(), ArrayList.class), requestedIds);
      setResourceProperty(resource, WIDGET_PROPERTIES_PROPERTY_ID, gson.fromJson(entity.getProperties(), Map.class), requestedIds);

      String clusterName = null;
      try {
        clusterName = getManagementController().getClusters().getClusterById(entity.getClusterId()).getClusterName();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage());
      }
      setResourceProperty(resource, WIDGET_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<Map<String, Object>> propertyMaps = request.getProperties();

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        for (Map<String, Object> propertyMap : propertyMaps) {
          final Long id;
          try {
            id = Long.parseLong(propertyMap.get(WIDGET_ID_PROPERTY_ID).toString());
          } catch (Exception ex) {
            throw new AmbariException("Widget should have numerical id");
          }

          final WidgetEntity entity = widgetDAO.findById(id);
          if (entity == null) {
            throw new ObjectNotFoundException("There is no widget with id " + id);
          }

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGET_WIDGET_NAME_PROPERTY_ID)))) {
            entity.setWidgetName(propertyMap.get(WIDGET_WIDGET_NAME_PROPERTY_ID).toString());
          }

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGET_WIDGET_TYPE_PROPERTY_ID)))) {
            entity.setWidgetType(propertyMap.get(WIDGET_WIDGET_TYPE_PROPERTY_ID).toString());
          }

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGET_METRICS_PROPERTY_ID)))) {
            entity.setMetrics(gson.toJson(propertyMap.get(WIDGET_METRICS_PROPERTY_ID)));
          }

          entity.setAuthor(getAuthorName(propertyMap));

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGET_DESCRIPTION_PROPERTY_ID)))) {
            entity.setDescription(propertyMap.get(WIDGET_DESCRIPTION_PROPERTY_ID).toString());
          }

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGET_SCOPE_PROPERTY_ID)))) {
            String scope = propertyMap.get(WIDGET_SCOPE_PROPERTY_ID).toString();
            if (!isScopeAllowedForUser(scope)) {
              throw new AmbariException("Only cluster operator can create widgets with cluster scope");
            }
            entity.setScope(scope);
          }

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGET_VALUES_PROPERTY_ID)))) {
            entity.setWidgetValues(gson.toJson(propertyMap.get(WIDGET_VALUES_PROPERTY_ID)));
          }

          Map<String, Object> widgetPropertiesMap = new HashMap<String, Object>();
          for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
            if (PropertyHelper.getPropertyCategory(entry.getKey()).equals(WIDGET_PROPERTIES_PROPERTY_ID)) {
              widgetPropertiesMap.put(PropertyHelper.getPropertyName(entry.getKey()), entry.getValue());
            }
          }

          if (!widgetPropertiesMap.isEmpty()) {
            entity.setProperties(gson.toJson(widgetPropertiesMap));
          }

          widgetDAO.merge(entity);
        }
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    final List<WidgetEntity> entitiesToBeRemoved = new ArrayList<WidgetEntity>();
    for (Map<String, Object> propertyMap : propertyMaps) {
      final Long id;
      try {
        id = Long.parseLong(propertyMap.get(WIDGET_ID_PROPERTY_ID).toString());
      } catch (Exception ex) {
        throw new SystemException("Widget should have numerical id");
      }
      final WidgetEntity entity = widgetDAO.findById(id);
      if (entity == null) {
        throw new NoSuchResourceException("There is no widget with id " + id);
      }
      entitiesToBeRemoved.add(entity);
    }

    for (WidgetEntity entity: entitiesToBeRemoved) {
      widgetDAO.remove(entity);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private boolean isScopeAllowedForUser(String scope) {
    if (scope.equals(WidgetEntity.USER_SCOPE)) {
      return true;
    }

    // Only cluster operators are allowed to create widgets with cluster scope
    SecurityContext securityContext = SecurityContextHolder.getContext();
    securityContext.getAuthentication().getAuthorities();
    boolean hasPermissionForClusterScope = false;
    for (GrantedAuthority grantedAuthority : securityContext.getAuthentication().getAuthorities()) {
      if (((AmbariGrantedAuthority) grantedAuthority).getPrivilegeEntity().getPermission().getId()
              == PermissionEntity.AMBARI_ADMIN_PERMISSION ||
              ((AmbariGrantedAuthority) grantedAuthority).getPrivilegeEntity().getPermission().getId()
                      == PermissionEntity.CLUSTER_OPERATE_PERMISSION) {
        hasPermissionForClusterScope = true;
      }
    }
    if (hasPermissionForClusterScope) {
      return true;
    } else {
      return false;
    }
  }

  private String getAuthorName(Map<String, Object> properties) {
    if (StringUtils.isNotBlank(ObjectUtils.toString(properties.get(WIDGET_AUTHOR_PROPERTY_ID)))) {
      return properties.get(WIDGET_AUTHOR_PROPERTY_ID).toString();
    }
    return getManagementController().getAuthName();
  }
}
