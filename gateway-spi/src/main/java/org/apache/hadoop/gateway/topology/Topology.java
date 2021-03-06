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
package org.apache.hadoop.gateway.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Topology {

  private String name;
  private long timestamp;
  private List<Provider> providerList = new ArrayList<Provider>();
  private Map<String,Map<String,Provider>> providerMap = new HashMap<String,Map<String,Provider>>();
  private List<Service> services = new ArrayList<Service>();
  private Map<String, Map<String, Service>> serviceMap = new HashMap<String, Map<String, Service>>();

  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp( long timestamp ) {
    this.timestamp = timestamp;
  }

  public Collection<Service> getServices() {
    return services;
  }

  public Service getService( String role, String name ) {
    Service service = null;
    Map<String, Service> nameMap = serviceMap.get( role );
    if( nameMap != null) {
      service = nameMap.get( name );
      if ( service == null && !nameMap.values().isEmpty() ) {
        service = (Service) nameMap.values().toArray()[0];
      }
    }
    return service;
  }

  public void addService( Service service ) {
    services.add( service );
    String role = service.getRole();
    Map<String, Service> nameMap = serviceMap.get( role );
    if( nameMap == null ) {
      nameMap = new HashMap<String, Service>();
      serviceMap.put( role, nameMap );
    }
    nameMap.put( service.getName(), service );
  }

  public Collection<Provider> getProviders() {
    return providerList;
  }

  public Provider getProvider( String role, String name ) {
    Provider provider = null;
    Map<String,Provider> nameMap = providerMap.get( role );
    if( nameMap != null) { 
      if( name != null ) {
        provider = nameMap.get( name );
      }
      else {
        provider = (Provider) nameMap.values().toArray()[0];
      }
    }
    return provider;
  }

  public void addProvider( Provider provider ) {
    providerList.add( provider );
    String role = provider.getRole();
    Map<String,Provider> nameMap = providerMap.get( role );
    if( nameMap == null ) {
      nameMap = new HashMap<String,Provider>();
      providerMap.put( role, nameMap );
    }
    nameMap.put( provider.getName(), provider );
  }

}
