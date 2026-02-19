package com.scivicslab.pluggablecli;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.cli.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginLoaderTest {

    private CommandRepository repo;
    private PluginLoader loader;

    @BeforeEach
    void setUp() {
        repo = new CommandRepository();
        loader = new PluginLoader(repo);
    }

    // -----------------------------------------------------------------------
    // ServiceLoader discovery
    // -----------------------------------------------------------------------

    @Test
    void loadPluginsDiscoversTestPlugin() {
        int count = loader.loadPlugins();
        assertEquals(1, count, "should discover exactly one plugin from test resources");
        assertEquals(1, loader.getPluginCount());

        // Commands registered by TestPlugin
        assertTrue(repo.hasCommand("test-hello"));
        assertTrue(repo.hasCommand("test-goodbye"));
    }

    @Test
    void loadPluginsWithCustomClassLoader() {
        int count = loader.loadPlugins(Thread.currentThread().getContextClassLoader());
        assertEquals(1, count);
        assertTrue(repo.hasCommand("test-hello"));
    }

    @Test
    void loadPluginsTwiceDoesNotDuplicate() {
        loader.loadPlugins();
        int secondCount = loader.loadPlugins();
        assertEquals(0, secondCount, "second load should find no new plugins");
        assertEquals(1, loader.getPluginCount());
    }

    // -----------------------------------------------------------------------
    // Manual load / unload
    // -----------------------------------------------------------------------

    @Test
    void loadPluginManually() {
        CliPlugin manual = new CliPlugin() {
            @Override
            public String getPluginName() { return "manual-plugin"; }
            @Override
            public void registerCommands(CommandRepository repository) {
                repository.addCommand(new CommandDefinition(
                        "manual-cmd", new Options(), "Manual command", null, null, "manual-plugin"));
            }
        };

        loader.loadPlugin(manual);
        assertEquals(1, loader.getPluginCount());
        assertTrue(repo.hasCommand("manual-cmd"));
        assertNotNull(loader.getPlugin("manual-plugin"));
    }

    @Test
    void loadDuplicatePluginThrows() {
        CliPlugin plugin = new CliPlugin() {
            @Override
            public String getPluginName() { return "dup"; }
            @Override
            public void registerCommands(CommandRepository repository) {}
        };

        loader.loadPlugin(plugin);
        assertThrows(IllegalStateException.class, () -> loader.loadPlugin(plugin));
    }

    @Test
    void unloadPluginRemovesCommands() {
        loader.loadPlugins();
        assertTrue(repo.hasCommand("test-hello"));

        CliPlugin unloaded = loader.unloadPlugin("test-plugin");
        assertNotNull(unloaded);
        assertEquals("test-plugin", unloaded.getPluginName());
        assertFalse(repo.hasCommand("test-hello"));
        assertFalse(repo.hasCommand("test-goodbye"));
        assertEquals(0, loader.getPluginCount());
    }

    @Test
    void unloadNonExistentPluginReturnsNull() {
        assertNull(loader.unloadPlugin("no-such-plugin"));
    }

    // -----------------------------------------------------------------------
    // Plugin metadata
    // -----------------------------------------------------------------------

    @Test
    void pluginMetadata() {
        loader.loadPlugins();
        CliPlugin plugin = loader.getPlugin("test-plugin");
        assertNotNull(plugin);
        assertEquals("test-plugin", plugin.getPluginName());
        assertEquals("0.1.0", plugin.getPluginVersion());
        assertEquals("A test plugin for unit tests", plugin.getDescription());
    }

    @Test
    void defaultPluginVersion() {
        CliPlugin minimal = new CliPlugin() {
            @Override
            public String getPluginName() { return "minimal"; }
            @Override
            public void registerCommands(CommandRepository repository) {}
        };
        assertEquals("unknown", minimal.getPluginVersion());
    }

    @Test
    void defaultPluginDescription() {
        CliPlugin minimal = new CliPlugin() {
            @Override
            public String getPluginName() { return "minimal"; }
            @Override
            public void registerCommands(CommandRepository repository) {}
        };
        assertEquals("minimal CLI plugin", minimal.getDescription());
    }

    // -----------------------------------------------------------------------
    // getLoadedPlugins
    // -----------------------------------------------------------------------

    @Test
    void getLoadedPluginsReturnsUnmodifiableMap() {
        loader.loadPlugins();
        var plugins = loader.getLoadedPlugins();
        assertEquals(1, plugins.size());
        assertTrue(plugins.containsKey("test-plugin"));
        assertThrows(UnsupportedOperationException.class,
                () -> plugins.put("x", null));
    }
}
