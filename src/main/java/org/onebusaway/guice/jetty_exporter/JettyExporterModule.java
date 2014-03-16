/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.guice.jetty_exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class JettyExporterModule extends AbstractModule {

  private final List<ServletSource> _sources = new ArrayList<ServletSource>();

  /**
   * Adds the {@link JettyExporterModule} and any other dependent modules to the
   * modules set in order to construct an appropriate Guice Injector module
   * list. The method can be called multiple times and only one instance of
   * {@link JettyExporterModule} will be added.
   * 
   * @param modules
   * @return the singleton instance of the module added to the modules list.
   */
  public static JettyExporterModule addModuleAndDependencies(Set<Module> modules) {
    modules.add(new JettyExporterModule());
    /**
     * We loop over the module list, looking for the JettyExporterModule that
     * might have already been added in a previous call.
     */
    for (Module module : modules) {
      if (module instanceof JettyExporterModule) {
        return (JettyExporterModule) module;
      }
    }
    throw new IllegalStateException("could not find JettyExporterModule");
  }

  public List<ServletSource> getSources() {
    return _sources;
  }

  @Override
  protected void configure() {

    bindListener(Matchers.any(), new TypeListener() {
      @Override
      public <I> void hear(TypeLiteral<I> injectableType,
          TypeEncounter<I> encounter) {

        Class<? super I> type = injectableType.getRawType();

        if (ServletSource.class.isAssignableFrom(type)) {
          encounter.register(new InjectionListenerImpl<I>(_sources));
        }
      }
    });

    bind(JettyExporterServiceImpl.class).toInstance(
        new JettyExporterServiceImpl(_sources));
  }

  /**
   * Implement hashCode() and equals() such that two instances of the module
   * will be equal.
   */
  @Override
  public int hashCode() {
    return this.getClass().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null)
      return false;
    return this.getClass().equals(o.getClass());
  }

  private static class InjectionListenerImpl<I> implements InjectionListener<I> {

    private final List<ServletSource> _sources;

    public InjectionListenerImpl(List<ServletSource> sources) {
      _sources = sources;
    }

    @Override
    public void afterInjection(I injectee) {
      _sources.add((ServletSource) injectee);
    }
  }
}
