package com.scivicslab.pluggablecli.testplugin;

import org.apache.commons.cli.Options;

import com.scivicslab.pluggablecli.CliPlugin;
import com.scivicslab.pluggablecli.CommandDefinition;
import com.scivicslab.pluggablecli.CommandRepository;

/**
 * A test implementation of {@link CliPlugin} used by unit tests.
 * Registered via META-INF/services for ServiceLoader discovery.
 */
public class TestPlugin implements CliPlugin {

    @Override
    public String getPluginName() {
        return "test-plugin";
    }

    @Override
    public String getPluginVersion() {
        return "0.1.0";
    }

    @Override
    public String getDescription() {
        return "A test plugin for unit tests";
    }

    @Override
    public void registerCommands(CommandRepository repository) {
        Options opts = new Options();
        repository.addCommand(new CommandDefinition(
                "test-hello", opts, "Test hello command", "Testing",
                cl -> System.out.println("Hello from test plugin"),
                getPluginName()));

        repository.addCommand(new CommandDefinition(
                "test-goodbye", opts, "Test goodbye command", "Testing",
                cl -> System.out.println("Goodbye from test plugin"),
                getPluginName()));
    }
}
