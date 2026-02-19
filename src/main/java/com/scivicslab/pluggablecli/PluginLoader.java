package com.scivicslab.pluggablecli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Discovers and manages {@link CliPlugin} instances via {@link ServiceLoader}.
 *
 * <p>Plugins are discovered from the classpath (or a custom {@link ClassLoader})
 * and registered into a {@link CommandRepository}. Plugins can be loaded at startup
 * or dynamically at runtime (e.g. after adding JARs to a custom classloader).
 */
public class PluginLoader {

    private final CommandRepository repository;
    private final LinkedHashMap<String, CliPlugin> loadedPlugins = new LinkedHashMap<>();

    /**
     * Creates a loader bound to the given repository.
     *
     * @param repository the command repository that plugins will register commands into
     */
    public PluginLoader(CommandRepository repository) {
        this.repository = repository;
    }

    /**
     * Discovers and loads all {@link CliPlugin} implementations visible on the
     * thread-context classloader's classpath.
     *
     * @return the number of newly loaded plugins
     */
    public int loadPlugins() {
        return loadPlugins(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Discovers and loads all {@link CliPlugin} implementations visible to the
     * given classloader.
     *
     * @param classLoader the classloader to scan for plugin implementations
     * @return the number of newly loaded plugins
     */
    public int loadPlugins(ClassLoader classLoader) {
        int count = 0;
        ServiceLoader<CliPlugin> serviceLoader = ServiceLoader.load(CliPlugin.class, classLoader);
        for (CliPlugin plugin : serviceLoader) {
            if (!loadedPlugins.containsKey(plugin.getPluginName())) {
                loadPlugin(plugin);
                count++;
            }
        }
        return count;
    }

    /**
     * Manually loads a single plugin.
     *
     * @param plugin the plugin to load
     * @throws IllegalStateException if a plugin with the same name is already loaded
     */
    public void loadPlugin(CliPlugin plugin) {
        String name = plugin.getPluginName();
        if (loadedPlugins.containsKey(name)) {
            throw new IllegalStateException("Plugin already loaded: " + name);
        }
        plugin.registerCommands(repository);
        loadedPlugins.put(name, plugin);
    }

    /**
     * Unloads a plugin by name, removing all commands it registered.
     *
     * @param pluginName the name of the plugin to unload
     * @return the unloaded plugin, or {@code null} if not found
     */
    public CliPlugin unloadPlugin(String pluginName) {
        CliPlugin removed = loadedPlugins.remove(pluginName);
        if (removed != null) {
            repository.removeCommandsByPlugin(pluginName);
        }
        return removed;
    }

    /**
     * Returns an unmodifiable view of the currently loaded plugins.
     *
     * @return map of plugin name to plugin instance
     */
    public Map<String, CliPlugin> getLoadedPlugins() {
        return Map.copyOf(loadedPlugins);
    }

    /**
     * Returns the plugin with the given name, or {@code null} if not loaded.
     */
    public CliPlugin getPlugin(String pluginName) {
        return loadedPlugins.get(pluginName);
    }

    /**
     * Returns the number of currently loaded plugins.
     */
    public int getPluginCount() {
        return loadedPlugins.size();
    }
}
