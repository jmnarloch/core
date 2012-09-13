/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.container.modules;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;

import org.jboss.forge.container.AddonUtil;
import org.jboss.forge.container.AddonUtil.AddonEntry;
import org.jboss.forge.container.exception.ContainerException;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ModuleSpec.Builder;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.modules.filter.PathFilters;

/**
 * TODO See {@link JarModuleLoader} for how to do dynamic dependencies from an XML file within.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class AddonModuleLoader extends ModuleLoader
{
   private static final ModuleIdentifier JAVAX_API = ModuleIdentifier.create("javax.api");
   private static final ModuleIdentifier PLUGIN_CONTAINER_API = ModuleIdentifier.create("org.jboss.forge.api");
   private static final ModuleIdentifier PLUGIN_CONTAINER = ModuleIdentifier.create("org.jboss.forge");
   private static final ModuleIdentifier WELD = ModuleIdentifier.create("org.jboss.weld");

   private ModuleLoader parent;

   public AddonModuleLoader()
   {
      this.parent = Module.getBootModuleLoader();
   }

   @Override
   protected Module preloadModule(ModuleIdentifier identifier) throws ModuleLoadException
   {
      if (findModule(identifier) != null)
      {
         Module pluginModule = super.preloadModule(identifier);
         return pluginModule;
      }
      else
         return preloadModule(identifier, parent);
   }

   @Override
   protected ModuleSpec findModule(ModuleIdentifier id) throws ModuleLoadException
   {
      AddonEntry found = null;
      for (AddonEntry plugin : AddonUtil.listByAPICompatibleVersion("2.0.0-SNAPSHOT"))
      {
         if (plugin.toModuleId().equals(id.toString()))
         {
            found = plugin;
            break;
         }
      }

      if (found != null)
      {
         Builder builder = ModuleSpec.build(id);

         builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                  PathFilters.rejectAll(), parent, JAVAX_API, false));
         builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                  PathFilters.rejectAll(), parent, WELD, false));
         builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                  PathFilters.rejectAll(), parent, PLUGIN_CONTAINER_API, false));
         builder.addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                  PathFilters.rejectAll(), parent, PLUGIN_CONTAINER, false));
         builder.addDependency(DependencySpec.createLocalDependencySpec());

         List<File> resources = AddonUtil.getAddonResources(found);

         for (File file : resources)
         {
            try
            {
               builder.addResourceRoot(
                        ResourceLoaderSpec.createResourceLoaderSpec(
                                 ResourceLoaders.createJarResourceLoader(file.getName(), new JarFile(file)),
                                 PathFilters.acceptAll())
                        );
            }
            catch (IOException e)
            {
               throw new ContainerException("Could not load resources from [" + file.getAbsolutePath() + "]", e);
            }
         }

         return builder.create();
      }
      return null;
   }

   @Override
   public String toString()
   {
      return "AddonModuleLoader";
   }

}