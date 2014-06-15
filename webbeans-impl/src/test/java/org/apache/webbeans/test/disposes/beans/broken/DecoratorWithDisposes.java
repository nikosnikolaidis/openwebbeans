/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.disposes.beans.broken;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Disposes;
import javax.inject.Inject;

import org.apache.webbeans.test.decorators.multiple.IOutputProvider;

/**
 * This class is a standard CDI bean but has a method annotated with
 * &#064;Disposes which is illegal.
 */
@Decorator
public class DecoratorWithDisposes implements IOutputProvider
{
    @Inject
    @Delegate
    private IOutputProvider delegate;


    public void disposerMethodOnDecorator_isBroken(@Disposes String val)
    {

    }

    @Override
    public String getOutput()
    {
        return null;
    }

    @Override
    public String trace()
    {
        return null;
    }

    @Override
    public String otherMethod()
    {
        return null;
    }

    @Override
    public String getDelayedOutput() throws InterruptedException
    {
        return null;
    }

}
