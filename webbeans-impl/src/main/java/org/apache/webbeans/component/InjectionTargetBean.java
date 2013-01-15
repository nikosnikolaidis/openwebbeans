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
package org.apache.webbeans.component;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.InterceptorData;

import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.intercept.webbeans.WebBeansInterceptorBeanPleaseRemove;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.proxy.ProxyFactory;
import org.apache.webbeans.util.Asserts;


/**
 * Abstract class for injection target beans.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean class
 */
public abstract class InjectionTargetBean<T> extends AbstractOwbBean<T>
{    
    /**Annotated type for bean*/
    private AnnotatedType<T> annotatedType;
    
    /**
     * Holds the all of the interceptor related data, contains around-invoke,
     * post-construct and pre-destroy
     * @deprecated old InterceptorData based config
     */
    protected List<InterceptorData> interceptorStack = new ArrayList<InterceptorData>();

    private InjectionTarget<T> injectionTarget = null;

    /**
     * Decorators
     * @deprecated will be replaced by InterceptorResolution logic
     */
    protected List<Decorator<?>> decorators = new ArrayList<Decorator<?>>();
    
    protected InjectionTargetBean(WebBeansContext webBeansContext,
                                  WebBeansType webBeansType,
                                  AnnotatedType<T> annotatedType,
                                  Set<Type> types,
                                  Set<Annotation> qualifiers,
                                  Class<? extends Annotation> scope,
                                  Class<T> beanClass,
                                  Set<Class<? extends Annotation>> stereotypes)
    {
        this(webBeansContext, webBeansType, annotatedType, types, qualifiers, scope, null, beanClass, stereotypes, false);
        setEnabled(true);
    }

    /**
     * Initializes the InjectionTarget Bean part.
     */
    protected InjectionTargetBean(WebBeansContext webBeansContext,
                                  WebBeansType webBeansType,
                                  AnnotatedType<T> annotatedType,
                                  Set<Type> types,
                                  Set<Annotation> qualifiers,
                                  Class<? extends Annotation> scope,
                                  String name,
                                  Class<T> beanClass,
                                  Set<Class<? extends Annotation>> stereotypes,
                                  boolean alternative)
    {
        super(webBeansContext, webBeansType, types, qualifiers, scope, name, false, beanClass, stereotypes, alternative);
        Asserts.assertNotNull(annotatedType, "AnnotatedType may not be null");
        this.annotatedType = annotatedType;
    }

    public InjectionTarget<T> getInjectionTarget()
    {
        return injectionTarget;
    }

    public void setInjectionTarget(InjectionTarget<T> injectionTarget)
    {
        this.injectionTarget = injectionTarget;
    }

    /**
     * {@inheritDoc}
     */
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        //Create actual bean instance
        T instance = getInjectionTarget().produce(creationalContext);

        //X TODO this should not be needed finally!
        //For dependent instance checks
        if(getScope() == Dependent.class && !(this instanceof EnterpriseBeanMarker))
        {
            final ProxyFactory proxyFactory = getWebBeansContext().getProxyFactoryRemove();
            T result = (T) proxyFactory.createDependentScopedBeanProxyRemove(this, instance, creationalContext);
            //Means that Dependent Bean has interceptor/decorator
            if(proxyFactory.isProxyInstanceRemove(result))
            {
                //This is a dependent scoped bean instance,
                //Therefore we inject dependencies of this instance
                //Otherwise we loose injection
                getInjectionTarget().inject(instance, creationalContext);

                //Dependent proxy
                return result;
            }
        }

        getInjectionTarget().inject(instance, creationalContext);

        //Post construct
        if(getWebBeansType().equals(WebBeansType.MANAGED))
        {
            // Call Post Construct
            //X TODO
        }


        return instance;
    }

    /**
     * {@inheritDoc}
     */
    protected void destroyInstance(T instance, CreationalContext<T> creationalContext)
    {
        injectionTarget.preDestroy(instance);
    }

    /**
     * Sub-classes must override this method to destroy bean instance.
     * 
     * @param instance object instance.
     */
    protected void destroyComponentInstance(T instance, CreationalContext<T> creationalContext)
    {
    }




    /**
     * {@inheritDoc}
     */
    public List<InterceptorData> getInterceptorStack()
    {
        return interceptorStack;
    }
    
    public List<Decorator<?>> getDecoratorStack()
    {
        return decorators;
    }

    /**
     * {@inheritDoc}
     */
    public AnnotatedType<T> getAnnotatedType()
    {
        return annotatedType;
    }
    
    /* (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractOwbBean#validatePassivationDependencies()
     */
    @Override
    public void validatePassivationDependencies()
    {        
        super.validatePassivationDependencies();
        
        //Check for interceptors and decorators
        for(int i = 0, size = decorators.size(); i < size; i++)
        {
            Decorator<?> dec = decorators.get(i);
            WebBeansDecorator<?> decorator = (WebBeansDecorator<?>)dec;
            if(!decorator.isPassivationCapable())
            {
                throw new WebBeansConfigurationException(MessageFormat.format(
                        WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0015), toString()));
            }
            else
            {
                decorator.validatePassivationDependencies();
            }
        }
        
        for(int i = 0, size = interceptorStack.size(); i < size; i++)
        {
            InterceptorData interceptorData = interceptorStack.get(i);
            if(interceptorData.isDefinedWithWebBeansInterceptor())
            {
                WebBeansInterceptorBeanPleaseRemove<?> interceptor = (WebBeansInterceptorBeanPleaseRemove<?>)interceptorData.getWebBeansInterceptor();
                if(!interceptor.isPassivationCapable())
                {
                    throw new WebBeansConfigurationException(MessageFormat.format(
                            WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0016), toString()));
                }
                else
                {
                    interceptor.validatePassivationDependencies();
                }
            }
            else
            {
                if(interceptorData.isDefinedInInterceptorClass())
                {
                    Class<?> interceptorClass = interceptorData.getInterceptorClass();
                    if(!Serializable.class.isAssignableFrom(interceptorClass))
                    {
                        throw new WebBeansConfigurationException(MessageFormat.format(
                                WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0016), toString()));
                    }               
                    else
                    {
                        if(!getWebBeansContext().getAnnotationManager().checkInjectionPointForInterceptorPassivation(interceptorClass))
                        {
                            throw new WebBeansConfigurationException(MessageFormat.format(
                                    WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0017), toString(), interceptorClass));
                        }
                    }
                }
            }
        }
    }    
}
