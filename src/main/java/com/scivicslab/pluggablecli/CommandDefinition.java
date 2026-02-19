package com.scivicslab.pluggablecli;

import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Immutable value object holding the complete definition of a CLI command.
 *
 * @param name        command name (non-null)
 * @param options     Commons CLI options (non-null)
 * @param description human-readable description (may be null)
 * @param category    category for grouping; defaults to "zz_Other" when null or blank
 * @param action      callback to execute when the command is invoked (may be null)
 * @param pluginName  name of the plugin that registered this command (null = built-in)
 */
public record CommandDefinition(
        String name,
        Options options,
        String description,
        String category,
        Consumer<CommandLine> action,
        String pluginName) {

    /** Default category for commands that do not specify one. */
    public static final String DEFAULT_CATEGORY = "zz_Other";

    /**
     * Compact constructor that validates required fields and normalizes the category.
     */
    public CommandDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(options, "options must not be null");
        if (category == null || category.isBlank()) {
            category = DEFAULT_CATEGORY;
        }
    }
}
