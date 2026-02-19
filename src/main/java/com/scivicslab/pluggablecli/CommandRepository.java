package com.scivicslab.pluggablecli;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Central registry for CLI command definitions and their execution logic.
 *
 * <p>Unlike the original Utility-cli {@code CommandRepository} which stored commands across
 * four parallel {@code TreeMap}s, this implementation consolidates all command metadata
 * into a single {@code TreeMap<String, CommandDefinition>}.
 *
 * <p>Category descriptions are kept in a separate map because they are not 1:1 with commands.
 */
public class CommandRepository {

    /** Single data store: command name to definition. */
    private final TreeMap<String, CommandDefinition> commands = new TreeMap<>();

    /** Category name to description (not 1:1 with commands, so kept separate). */
    private final TreeMap<String, String> categoryDescriptionMap = new TreeMap<>();

    /** Options that can be used independently of a specific command. */
    private Options universalOptions;

    /** The command name extracted from the most recent {@link #parse(String[])} call. */
    private String givenCommand;

    /** Whether the most recent parse detected a help flag. */
    private boolean helpRequested;

    /** Help formatter builder (nullable; defaults to standard rendering). */
    private HelpFormatterBuilder helpFormatterBuilder;

    /**
     * Default constructor initializes universal options with a {@code -h/--help} flag.
     */
    public CommandRepository() {
        this.universalOptions = new Options();
        this.universalOptions.addOption(Option.builder()
                .option("h")
                .longOpt("help")
                .hasArg(false)
                .argName("help")
                .desc("Print help message")
                .required(false)
                .build());
    }

    // -----------------------------------------------------------------------
    // addCommand overloads (Utility-cli compatible)
    // -----------------------------------------------------------------------

    /** Adds a command with options only (default category, no description, no action). */
    public void addCommand(String command, Options options) {
        commands.put(command, new CommandDefinition(command, options, null, null, null, null));
    }

    /** Adds a command with options and an action (default category). */
    public void addCommand(String command, Options options, Consumer<CommandLine> action) {
        commands.put(command, new CommandDefinition(command, options, null, null, action, null));
    }

    /** Adds a command with options and a description (default category). */
    public void addCommand(String command, Options options, String description) {
        commands.put(command, new CommandDefinition(command, options, description, null, null, null));
    }

    /** Adds a command with options, description, and action (default category). */
    public void addCommand(String command, Options options, String description,
            Consumer<CommandLine> action) {
        commands.put(command, new CommandDefinition(command, options, description, null, action, null));
    }

    /** Adds a command with options under a specific category. */
    public void addCommand(String category, String command, Options options) {
        commands.put(command, new CommandDefinition(command, options, null, category, null, null));
    }

    /** Adds a command with options and action under a specific category. */
    public void addCommand(String category, String command, Options options,
            Consumer<CommandLine> action) {
        commands.put(command, new CommandDefinition(command, options, null, category, action, null));
    }

    /** Adds a command with options and description under a specific category. */
    public void addCommand(String category, String command, Options options, String description) {
        commands.put(command, new CommandDefinition(command, options, description, category, null, null));
    }

    /** Adds a command with options, description, and action under a specific category. */
    public void addCommand(String category, String command, Options options, String description,
            Consumer<CommandLine> action) {
        commands.put(command, new CommandDefinition(command, options, description, category, action, null));
    }

    /**
     * Adds a fully-specified {@link CommandDefinition}.
     */
    public void addCommand(CommandDefinition definition) {
        commands.put(definition.name(), definition);
    }

    // -----------------------------------------------------------------------
    // Remove
    // -----------------------------------------------------------------------

    /**
     * Removes a single command by name.
     *
     * @param command the command name to remove
     * @return the removed definition, or {@code null} if not found
     */
    public CommandDefinition removeCommand(String command) {
        return commands.remove(command);
    }

    /**
     * Removes all commands registered by the given plugin name.
     *
     * @param pluginName the plugin name whose commands should be removed
     * @return the number of commands removed
     */
    public int removeCommandsByPlugin(String pluginName) {
        if (pluginName == null) {
            return 0;
        }
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, CommandDefinition> entry : commands.entrySet()) {
            if (pluginName.equals(entry.getValue().pluginName())) {
                toRemove.add(entry.getKey());
            }
        }
        for (String key : toRemove) {
            commands.remove(key);
        }
        return toRemove.size();
    }

    // -----------------------------------------------------------------------
    // Universal options
    // -----------------------------------------------------------------------

    /** Adds an option that can be used independently of any specific command. */
    public void addUniversalOption(Option option) {
        this.universalOptions.addOption(option);
    }

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    /** Checks whether the given command exists in the repository. */
    public boolean hasCommand(String command) {
        return this.commands.containsKey(command);
    }

    /** Returns the command name extracted from the most recent {@link #parse} call. */
    public String getGivenCommand() {
        return this.givenCommand;
    }

    /** Returns {@code true} if the most recent parse detected a help flag ({@code -h/--help}). */
    public boolean isHelpRequested() {
        return this.helpRequested;
    }

    /** Returns the definition for the given command, or {@code null} if not found. */
    public CommandDefinition getCommand(String command) {
        return this.commands.get(command);
    }

    /** Returns the number of registered commands. */
    public int size() {
        return this.commands.size();
    }

    // -----------------------------------------------------------------------
    // Category descriptions
    // -----------------------------------------------------------------------

    /** Sets a description for a category. */
    public void setCategoryDescription(String category, String description) {
        categoryDescriptionMap.put(category, description);
    }

    // -----------------------------------------------------------------------
    // Help formatter
    // -----------------------------------------------------------------------

    /** Sets a custom help formatter builder for {@link #printCommandHelp}. */
    public void setHelpFormatterBuilder(HelpFormatterBuilder builder) {
        this.helpFormatterBuilder = builder;
    }

    // -----------------------------------------------------------------------
    // Parse
    // -----------------------------------------------------------------------

    /**
     * Parses the given command-line arguments and extracts the specified command.
     * Also detects {@code -h/--help} flags and sets {@link #isHelpRequested()}.
     *
     * @param args the command-line arguments
     * @return the parsed {@code CommandLine} object, or {@code null} if args is empty
     * @throws ParseException if command-line parsing fails
     */
    public CommandLine parse(String[] args) throws ParseException {
        this.helpRequested = false;
        CommandLine cl = null;

        if (args.length > 0) {
            // Check for help flag before parsing
            for (String arg : args) {
                if ("-h".equals(arg) || "--help".equals(arg)) {
                    this.helpRequested = true;
                    break;
                }
            }

            String command = args[0];
            this.givenCommand = command;
            CommandDefinition def = this.commands.get(command);
            CommandLineParser parser = new DefaultParser();

            if (def == null) {
                cl = parser.parse(this.universalOptions, args);
            } else {
                cl = parser.parse(def.options(), args);
            }
        }
        return cl;
    }

    // -----------------------------------------------------------------------
    // Execute
    // -----------------------------------------------------------------------

    /**
     * Executes the action associated with the given command.
     *
     * @param givenCommand the command to execute
     * @param cl           the parsed command-line arguments
     */
    public void execute(String givenCommand, CommandLine cl) {
        CommandDefinition def = this.commands.get(givenCommand);
        if (def != null && def.action() != null) {
            def.action().accept(cl);
        }
    }

    // -----------------------------------------------------------------------
    // Help display
    // -----------------------------------------------------------------------

    /**
     * Displays help for a specific command using the configured {@link HelpFormatter}.
     *
     * @param command the command name whose help should be displayed
     */
    public void printCommandHelp(String command) {
        CommandDefinition def = this.commands.get(command);
        if (def == null) {
            return;
        }

        PrintWriter out = new PrintWriter(System.out, true);
        if (helpFormatterBuilder != null) {
            HelpFormatter fmt = helpFormatterBuilder.build();
            fmt.printCommandHelp(out, command, def.options(), def.description());
        } else {
            HelpFormatter fmt = new HelpFormatter();
            fmt.printCommandHelp(out, command, def.options(), def.description());
        }
        out.println();
    }

    /**
     * Displays a categorized list of available commands along with their descriptions.
     *
     * @param synopsis the usage syntax for the application
     */
    public void printCommandList(String synopsis) {
        System.out.println("\n## Usage\n");
        System.out.println(synopsis);
        System.out.println();

        boolean hasCategory = false;
        TreeMap<String, List<String>> categoryToCommand = calcCategoryToCommand();

        for (Map.Entry<String, List<String>> entry : categoryToCommand.entrySet()) {
            String category = entry.getKey();
            List<String> cmds = entry.getValue();
            Collections.sort(cmds);

            if (category.equals(CommandDefinition.DEFAULT_CATEGORY)) {
                if (hasCategory) {
                    System.out.println("\n## Other Commands");
                } else {
                    System.out.println("\n## Commands");
                }
            } else {
                hasCategory = true;
                System.out.println("\n## " + category);
            }
            System.out.println();

            if (categoryDescriptionMap.containsKey(category)) {
                System.out.println(categoryDescriptionMap.get(category));
            }
            printCommandList(cmds);
            System.out.println();
        }
    }

    /**
     * Displays a brief description for each command in the given list.
     */
    public void printCommandList(List<String> commandNames) {
        for (String cmdName : commandNames) {
            CommandDefinition def = commands.get(cmdName);
            String description = def != null ? def.description() : null;
            String formatted = cmdName;
            if (formatted.length() < 16) {
                formatted = String.format("%-16s", formatted);
            } else {
                int width = ((formatted.length() + 4) / 4) * 4;
                String f = "%-" + width + "s";
                formatted = String.format(f, formatted);
            }
            System.out.println(formatted + extractFirstLine(description));
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private TreeMap<String, List<String>> calcCategoryToCommand() {
        TreeMap<String, List<String>> result = new TreeMap<>();
        for (Map.Entry<String, CommandDefinition> entry : this.commands.entrySet()) {
            String cmdName = entry.getKey();
            String category = entry.getValue().category();
            result.computeIfAbsent(category, k -> new ArrayList<>()).add(cmdName);
        }
        return result;
    }

    private String extractFirstLine(String str) {
        if (str == null) {
            return "";
        }
        return str.split("\n", 2)[0];
    }
}
