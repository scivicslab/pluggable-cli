package com.scivicslab.pluggablecli;

/**
 * Service provider interface for plugin-based CLI command registration.
 *
 * <p>Plugins implement this interface and register themselves using Java's
 * {@link java.util.ServiceLoader} mechanism. This enables automatic command
 * discovery and registration at runtime.
 *
 * <h2>Usage in Plugin</h2>
 * <pre>{@code
 * public class MyPlugin implements CliPlugin {
 *     @Override
 *     public String getPluginName() {
 *         return "my-plugin";
 *     }
 *
 *     @Override
 *     public void registerCommands(CommandRepository repo) {
 *         Options opts = new Options();
 *         opts.addOption("n", "name", true, "Name to greet");
 *         repo.addCommand(new CommandDefinition(
 *             "greet", opts, "Greet someone", "Greetings",
 *             cl -> System.out.println("Hello, " + cl.getOptionValue("n")),
 *             getPluginName()));
 *     }
 * }
 * }</pre>
 *
 * <h2>META-INF/services registration</h2>
 * Create file {@code META-INF/services/com.scivicslab.pluggablecli.CliPlugin}
 * containing the fully-qualified class name of the implementation.
 *
 * @see PluginLoader
 * @see java.util.ServiceLoader
 */
public interface CliPlugin {

    /**
     * Registers commands provided by this plugin into the given repository.
     *
     * @param repository the command repository to register commands into
     */
    void registerCommands(CommandRepository repository);

    /**
     * Returns the unique name of this plugin.
     * This value should match the {@code pluginName} field in {@link CommandDefinition}
     * for commands registered by this plugin.
     *
     * @return the plugin name
     */
    String getPluginName();

    /**
     * Returns the version of this plugin.
     *
     * @return the plugin version, or "unknown" if not overridden
     */
    default String getPluginVersion() {
        return "unknown";
    }

    /**
     * Returns a human-readable description of this plugin.
     *
     * @return the plugin description
     */
    default String getDescription() {
        return getPluginName() + " CLI plugin";
    }
}
