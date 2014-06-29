/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.config;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

import org.apache.webbeans.util.Asserts;

public class OwbGenericArrayTypeImpl implements GenericArrayType
{

    private Type componentType;
    
    public OwbGenericArrayTypeImpl(Type componentType)
    {
        Asserts.assertNotNull(componentType, "component type may not be null");
        this.componentType = componentType;
    }

    @Override
    public Type getGenericComponentType()
    {
        return componentType;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
       return componentType.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
       if (this == obj)
       {
          return true;
       }
       else if (obj instanceof GenericArrayType)
       {
           return ((GenericArrayType)obj).getGenericComponentType().equals(componentType);
       }
       else
       {
          return false;
       }
       
    }

    public String toString()
    {
        return componentType + "[]";
    }
}