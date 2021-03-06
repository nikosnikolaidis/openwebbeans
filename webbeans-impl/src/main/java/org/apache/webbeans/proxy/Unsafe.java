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
package org.apache.webbeans.proxy;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.webbeans.exception.ProxyGenerationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;

public class Unsafe
{
    /**
     * contains the instance of sun.misc.Unsafe.
     * We use it for creating the proxy instance without fully
     * initializing the class.
     */
    private Object unsafe;
    private Object internalUnsafe;
    private Method unsafeAllocateInstance;
    private final AtomicReference<Method> unsafeDefineClass = new AtomicReference<>();

    public Unsafe()
    {
        final Class<?> unsafeClass = getUnsafeClass();

        this.unsafe = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try
            {
                Field field = unsafeClass.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return field.get(null);
            }
            catch (Exception e)
            {
                WebBeansLoggerFacade.getLogger(Unsafe.class)
                        .info("Cannot get sun.misc.Unsafe - will use newInstance() instead!");
                return null;
            }
        });
        this.internalUnsafe = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try // j11, unwrap unsafe, it owns defineClass now and no more theUnsafe
            {
                final Field theInternalUnsafe = unsafeClass.getDeclaredField("theInternalUnsafe");
                theInternalUnsafe.setAccessible(true);
                return theInternalUnsafe.get(null).getClass();
            }
            catch (final Exception notJ11OrMore)
            {
                return unsafe;
            }
        });

        if (unsafe != null)
        {
            unsafeAllocateInstance = AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
                try
                {
                    Method mtd = unsafe.getClass().getDeclaredMethod("allocateInstance", Class.class);
                    mtd.setAccessible(true);
                    return mtd;
                }
                catch (Exception e)
                {
                    return null; // use newInstance()
                }
            });

            try
            {
                final Class<?> rootLoaderClass = Class.forName("java.lang.ClassLoader");
                rootLoaderClass.getDeclaredMethod(
                    "defineClass", new Class[] { String.class, byte[].class, int.class, int.class })
                    .setAccessible(true);
                rootLoaderClass.getDeclaredMethod(
                    "defineClass", new Class[] {
                            String.class, byte[].class, int.class, int.class, ProtectionDomain.class })
                    .setAccessible(true);
            }
            catch (final Exception e)
            {
                try // some j>8, since we have unsafe let's use it
                {
                    final Class<?> rootLoaderClass = Class.forName("java.lang.ClassLoader");
                    final Method objectFieldOffset = unsafe.getClass().getDeclaredMethod("objectFieldOffset", Field.class);
                    final Method putBoolean = unsafe.getClass().getDeclaredMethod("putBoolean", Object.class, long.class, boolean.class);
                    objectFieldOffset.setAccessible(true);
                    final long accOffset = Long.class.cast(objectFieldOffset.invoke(unsafe, AccessibleObject.class.getDeclaredField("override")));
                    putBoolean.invoke(unsafe, rootLoaderClass.getDeclaredMethod("defineClass",
                            new Class[]{String.class, byte[].class, int.class, int.class}),
                            accOffset, true);
                    putBoolean.invoke(unsafe, rootLoaderClass
                                    .getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class,
                                            int.class, int.class, ProtectionDomain.class}),
                            accOffset, true);
                }
                catch (final Exception ex)
                {
                    // no-op
                }
            }
        }
    }

    /**
     * The 'defineClass' method on the ClassLoader is protected, thus we need to invoke it via reflection.
     * @return the Class which got loaded in the classloader
     */
    public <T> Class<T> defineAndLoadClass(ClassLoader classLoader, String proxyName, byte[] proxyBytes)
            throws ProxyGenerationException
    {
        Class<?> clazz = classLoader.getClass();

        Method defineClassMethod = null;
        do
        {
            try
            {
                defineClassMethod = clazz.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            }
            catch (NoSuchMethodException e)
            {
                // do nothing, we need to search the superclass
            }

            clazz = clazz.getSuperclass();
        } while (defineClassMethod == null && clazz != Object.class);

        if (defineClassMethod != null && !defineClassMethod.isAccessible())
        {
            try
            {
                defineClassMethod.setAccessible(true);
            }
            catch (RuntimeException re) // likely j9, let's use unsafe
            {
                defineClassMethod = null;
            }
        }

        try
        {
            Class<T> definedClass;

            if (defineClassMethod != null)
            {
                definedClass = (Class<T>) defineClassMethod.invoke(classLoader, proxyName, proxyBytes, 0, proxyBytes.length);
            }
            else
            {

                definedClass = (Class<T>) unsafeDefineClass().invoke(unsafe, proxyName, proxyBytes, 0, proxyBytes.length, classLoader, null);
            }

            return (Class<T>) Class.forName(definedClass.getName(), true, classLoader);
        }
        catch (InvocationTargetException le) // if concurrent calls are done then ensure to just reload the created one
        {
            if (LinkageError.class.isInstance(le.getCause()))
            {
                try
                {
                    return (Class<T>) Class.forName(proxyName.replace('/', '.'), true, classLoader);
                }
                catch (ClassNotFoundException e)
                {
                    // default error handling
                }
            }
            throw new ProxyGenerationException(le.getCause());
        }
        catch (Throwable e)
        {
            throw new ProxyGenerationException(e);
        }
    }

    private Method unsafeDefineClass()
    {
        Method value = unsafeDefineClass.get();
        if (value == null)
        {
            synchronized (this)
            {
                final Class<?> unsafeClass = internalUnsafe.getClass();
                value = AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
                    try
                    {
                        return unsafeClass.getDeclaredMethod("defineClass",
                                String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
                    }
                    catch (final Exception e)
                    {
                        throw new IllegalStateException("Cannot get Unsafe.defineClass or equivalent", e);
                    }
                });
                unsafeDefineClass.compareAndSet(null, value);
            }
        }
        return value;
    }

    public <T> T unsafeNewInstance(Class<T> clazz)
    {
        try
        {
            if (unsafeAllocateInstance != null)
            {
                return (T) unsafeAllocateInstance.invoke(unsafe, clazz);
            }
            else
            {
                try
                {
                    return clazz.getConstructor().newInstance();
                }
                catch (final Exception e)
                {
                    throw new IllegalStateException("Failed to allocateInstance of Proxy class " + clazz.getName(), e);
                }
            }
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Failed to allocateInstance of Proxy class " + clazz.getName(), e);
        }
        catch (InvocationTargetException e)
        {
            Throwable throwable = e.getTargetException() != null ? e.getTargetException() : e;
            throw new IllegalStateException("Failed to allocateInstance of Proxy class " + clazz.getName(),
                    throwable);
        }
    }

    private Class<?> getUnsafeClass()
    {
        try
        {
            return AccessController.doPrivileged((PrivilegedAction<Class<?>>) () ->
                    Stream.of(Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader())
                            .flatMap(classloader -> Stream.of("sun.misc.Unsafe", "jdk.internal.misc.Unsafe")
                            .flatMap(name ->
                            {
                                try
                                {
                                    return Stream.of(classloader.loadClass(name));
                                }
                                catch (final ClassNotFoundException e)
                                {
                                    return Stream.empty();
                                }
                            }))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Cannot get Unsafe")));
        }
        catch (final Exception e)
        {
            throw new IllegalStateException("Cannot get Unsafe class", e);
        }
    }
}
