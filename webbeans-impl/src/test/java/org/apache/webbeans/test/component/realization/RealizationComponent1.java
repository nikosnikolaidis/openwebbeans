/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.test.component.realization;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import org.apache.webbeans.annotation.specific.Realizes;
import org.apache.webbeans.test.component.UserComponent;

@SessionScoped
@Realizes
public class RealizationComponent1 extends GenericComponent implements Serializable
{
    public RealizationComponent1()
    {
        
    }
    
    public UserComponent getUserComponent()
    {
        return this.component;
    }
}
