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
package org.apache.webbeans.context;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.context.type.ContextTypes;

/**
 * Abstract implementation of the {@link WebBeansContext} interfaces.
 */
public abstract class AbstractContext implements WebBeansContext
{
    protected boolean active;

    protected ContextTypes type;

    protected Map<Contextual<?>, Object> componentInstanceMap = null;

    protected Class<? extends Annotation> scopeType;

    protected AbstractContext()
    {

    }

    protected AbstractContext(Class<? extends Annotation> scopeType)
    {
        this.scopeType = scopeType;
        setComponentInstanceMap();

    }

    protected AbstractContext(ContextTypes type)
    {
        this.type = type;
        configureScopeType(type);
        setComponentInstanceMap();
    }

    private void configureScopeType(ContextTypes type)
    {
        if (type.equals(ContextTypes.APPLICATION))
        {
            this.scopeType = ApplicationScoped.class;
        }
        else if (type.equals(ContextTypes.SESSION))
        {
            this.scopeType = SessionScoped.class;
        }
        else if (type.equals(ContextTypes.REQUEST))
        {
            this.scopeType = RequestScoped.class;
        }
        else if (type.equals(ContextTypes.DEPENDENT))
        {
            this.scopeType = Dependent.class;
        }
        else if (type.equals(ContextTypes.CONVERSATION))
        {
            this.scopeType = ConversationScoped.class;
        }

    }

    @SuppressWarnings("unchecked")
    public <T> T get(Contextual<T> component)
    {
        checkActive();
        
        return (T) componentInstanceMap.get(component);
    }

    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext)
    {
        checkActive();
        
        return getInstance(component, creationalContext);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getInstance(Contextual<T> component, CreationalContext<T> creationalContext)
    {
        T instance = (T)componentInstanceMap.get(component);

        
        if (instance != null)
        {
            return instance;
        }

        else
        {
            if(creationalContext == null)
            {
                return null;
            }
            
            else
            {
                instance = component.create(creationalContext);

                if (instance != null)
                {
                    componentInstanceMap.put(component, instance);
                }
                
            }            
        }

        return  instance;
    }

    public <T> void remove(Contextual<T> component)
    {
        removeInstance(component);
    }

    /**
     * Destroy the given web beans component instance.
     * 
     * @param <T>
     * @param component web beans component
     * @param instance component instance
     */
    private <T> void destroyInstance(Bean<T> component, T instance)
    {
        component.destroy(instance);
    }

    /**
     * Destroys the context.
     * 
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public void destroy()
    {
        Set<Entry<Contextual<?>, Object>> entrySet = componentInstanceMap.entrySet();
        Iterator<Entry<Contextual<?>, Object>> it = entrySet.iterator();

        Contextual<?> component = null;
        while (it.hasNext())
        {
            component = it.next().getKey();
            
            Object instance = componentInstanceMap.get(component);

            destroyInstance((Bean<Object>) component, instance);

        }
        
        //Clear cache
        componentInstanceMap.clear();
    }

    protected <T> void removeInstance(Contextual<T> component)
    {
        if (componentInstanceMap.get(component) != null)
        {
            componentInstanceMap.remove(component);
        }
    }

    /**
     * Gets context active flag.
     * 
     * @return active flag
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * Set component active flag.
     * 
     * @param active active flag
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     * Type of the context
     * 
     * @return type
     */
    public ContextTypes getType()
    {
        return type;
    }

    public Map<Contextual<?>, Object> getComponentInstanceMap()
    {
        return componentInstanceMap;
    }

    public Class<? extends Annotation> getScopeType()
    {

        return this.scopeType;
    }

    public <T> void remove(BeanManager container, Bean<T> component)
    {
        remove(component);
    }

    protected abstract void setComponentInstanceMap();
    
    protected void checkActive()
    {
        if (!active)
        {
            throw new ContextNotActiveException("WebBeans context with scope type annotation @" + getScopeType().getName() + " is not active with respect to the current thread");
        }        
    }

}
