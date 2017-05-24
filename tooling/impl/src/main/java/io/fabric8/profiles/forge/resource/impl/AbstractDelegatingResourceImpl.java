/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.profiles.forge.resource.impl;

import java.io.InputStream;
import java.util.List;
import javax.validation.constraints.NotNull;

import io.fabric8.profiles.forge.resource.DelegatingResource;
import org.jboss.forge.addon.resource.AbstractResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;

/**
 * Abstract base class for implementations of {@link DelegatingResource}.
 */
public abstract class AbstractDelegatingResourceImpl<R extends Resource<? extends T>, T> extends AbstractResource<T> implements DelegatingResource<R, T> {

    private final R delegate;

    protected AbstractDelegatingResourceImpl(ResourceFactory factory, @NotNull R delegate) {
        super(factory, delegate.getParent());
        this.delegate = delegate;
    }

    @Override
    public R getDelegate() {
        return delegate;
    }

    @Override
    public Resource<?> getChild(String name) {
        return getDelegate().getChild(name);
    }

    @Override
    protected List<Resource<?>> doListResources() {
        return getDelegate().listResources();
    }

    @Override
    public boolean delete() throws UnsupportedOperationException {
        return getDelegate().delete();
    }

    @Override
    public boolean delete(boolean recursive) throws UnsupportedOperationException {
        return getDelegate().delete(recursive);
    }

    @Override
    public T getUnderlyingResourceObject() {
        return getDelegate().getUnderlyingResourceObject();
    }

    @Override
    public boolean exists() {
        return getDelegate().exists();
    }

    @Override
    public <R extends Resource<?>> R reify(Class<R> type) {
        R res = super.reify(type);
        return res != null ? res : getDelegate().reify(type);
    }

    @Override
    public InputStream getResourceInputStream() {
        return getDelegate().getResourceInputStream();
    }

    @Override
    public String toString() {
        return getName();
    }
}
