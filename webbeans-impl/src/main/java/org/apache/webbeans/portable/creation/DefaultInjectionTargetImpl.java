/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.portable.creation;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.util.WebBeansAnnotatedTypeUtil;

public class DefaultInjectionTargetImpl<T> implements InjectionTarget<T>
{
    private InjectionTargetProducer<T> target;
    
    public DefaultInjectionTargetImpl(AnnotatedType<T> annotatedType)
    {
        target = new InjectionTargetProducer<T>(WebBeansAnnotatedTypeUtil.defineManagedBean(annotatedType));
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx)
    {
        this.target.inject(instance, ctx);
    }

    @Override
    public void postConstruct(T instance)
    {
        this.target.postConstruct(instance);
    }

    @Override
    public void preDestroy(T instance)
    {
        this.target.preDestroy(instance);
    }

    @Override
    public void dispose(T instance)
    {
        this.target.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return this.target.getInjectionPoints();
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        return this.target.produce(creationalContext);
    }

}
