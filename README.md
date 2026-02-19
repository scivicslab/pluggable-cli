# pluggable-cli

A CLI framework for subcommand-based interfaces with ServiceLoader-based plugin discovery, built on top of Apache Commons CLI.

[![Java Version](https://img.shields.io/badge/java-21+-blue.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-1.0.0-brightgreen.svg)](https://scivicslab.github.io/pluggable-cli/)

pluggable-cli keeps parsing logic structured and manageable regardless of how many subcommands exist. Its key feature is the ability to **extend a CLI application with new commands simply by adding a plugin JAR to the classpath**, without modifying the application code.

## Features

- **Structured CLI code** - Register subcommands with options and actions separately. No giant `main()` method, no giant `if-else`/`switch` statement for dispatching.
- **Thin wrapper over Commons CLI** - Exposes `Options`/`CommandLine` directly. Low learning cost; existing Commons CLI knowledge applies as-is.
- **ServiceLoader-based plugin system** - Drop a JAR on the classpath to add new commands at runtime.
- **Plugin unloading** - Remove all commands registered by a plugin in one call.
- **No reflection** - No annotations or reflection involved, making GraalVM Native Image compilation straightforward.
- **Minimal dependency** - Only depends on Apache Commons CLI (~80KB).

## Installation

```xml
<dependency>
    <groupId>com.scivicslab</groupId>
    <artifactId>pluggable-cli</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### The `main` method

The `main` method follows a fixed structure that stays the same no matter how many subcommands are added.

```java
import com.scivicslab.pluggablecli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

public class App {

    String synopsis = "java -jar my-tool-<VERSION>.jar <command> <options>";
    CommandRepository cmds = new CommandRepository();

    public static void main(String[] args) {
        App app = new App();
        app.setupCommands();

        try {
            CommandLine cl = app.cmds.parse(args);
            String command = app.cmds.getGivenCommand();

            if (command == null) {
                app.cmds.printCommandList(app.synopsis);
            } else if (app.cmds.isHelpRequested()) {
                app.cmds.printCommandHelp(command);
            } else if (app.cmds.hasCommand(command)) {
                app.cmds.execute(command, cl);
            } else {
                System.err.println("Error: Unknown command: " + command);
                app.cmds.printCommandList(app.synopsis);
            }
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            app.cmds.printCommandHelp(app.cmds.getGivenCommand());
        }
    }

    public void setupCommands() {
        greetCommand();
        splitCommand();
    }

    // ... command definitions below
}
```

### Defining a subcommand

Define a subcommand by building an `Options` object and calling `addCommand()`. The execution logic is passed as a `Consumer<CommandLine>` lambda.

```java
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

public void greetCommand() {
    Options opts = new Options();

    opts.addOption(Option.builder("n")
            .longOpt("name")
            .hasArg(true)
            .argName("name")
            .desc("Name to greet.")
            .required(true)
            .build());

    cmds.addCommand("greet", opts,
            "Greets the specified person.",
            (CommandLine cl) -> {
                String name = cl.getOptionValue("name");
                System.out.println("Hello, " + name + "!");
            });
}
```

This can then be invoked as:

```bash
$ my-tool greet -n Alice
Hello, Alice!

$ my-tool greet --name Alice
Hello, Alice!
```

Use the same `Option.builder()` pattern for every option. Copy the boilerplate and fill in the fields.

### `Option.builder()` field reference

| Method | Meaning | Command-line mapping |
|--------|---------|---------------------|
| `Option.builder("n")` | Short option name | `-n value` |
| `.longOpt("name")` | Long option name | `--name value` |
| `.hasArg(true)` | Takes an argument | `-n Alice` (`false` for flags like `-v`) |
| `.argName("name")` | Placeholder in help output | `usage: greet [-n <name>]` |
| `.desc("...")` | Description in help output | |
| `.required(true)` | Required option | Omitting causes a parse error |

### Repeatable options

When the same option is specified multiple times, `getOptionValue()` returns only the first value, while `getOptionValues()` returns all values as an array.

```java
opts.addOption(Option.builder("t")
        .longOpt("tag")
        .hasArg(true)
        .argName("tag")
        .desc("Tag to apply (repeatable).")
        .required(false)
        .build());

cmds.addCommand("label", opts, "Apply tags.",
        (CommandLine cl) -> {
            String first = cl.getOptionValue("tag");       // "foo"
            String[] all  = cl.getOptionValues("tag");     // ["foo", "bar"]
        });
```

```bash
$ my-tool label -t foo -t bar
```

### Categories

Group subcommands into categories by passing a category name as the first argument to `addCommand()`.

```java
cmds.addCommand("jar commands", "jar:listClasses", opts,
        "Lists all classes contained in the specified JAR file.",
        (CommandLine cl) -> { /* ... */ });

cmds.setCategoryDescription("jar commands",
        "Commands for inspecting JAR files.");
```

Commands without an explicit category are placed in the default `"zz_Other"` category and appear at the end of the help listing.

### Separating commands into classes

Split groups of subcommands into separate classes for better organization.

```java
public class JarCommands {

    public void setupCommands(CommandRepository cmds) {
        jarListClassesCommand(cmds);
        jarSearchClassesCommand(cmds);
    }

    private void jarListClassesCommand(CommandRepository cmds) {
        Options opts = new Options();

        opts.addOption(Option.builder("j")
                .longOpt("jar")
                .hasArg(true)
                .argName("jar")
                .desc("The JAR file to list classes from.")
                .required(true)
                .build());

        cmds.addCommand("jar commands", "jar:listClasses", opts,
                "Lists all classes contained in the specified JAR file.",
                (CommandLine cl) -> {
                    String jarFile = cl.getOptionValue("jar");
                    JarClassLister.listClasses(Path.of(jarFile));
                });
    }
}
```

```java
public void setupCommands() {
    greetCommand();
    splitCommand();
    new JarCommands().setupCommands(cmds);
}
```

## Plugin System

The core feature of pluggable-cli: **extend your application with new commands by dropping a JAR on the classpath.**

### Writing a plugin

Implement the `CliPlugin` interface.

```java
import com.scivicslab.pluggablecli.CliPlugin;
import com.scivicslab.pluggablecli.CommandDefinition;
import com.scivicslab.pluggablecli.CommandRepository;
import org.apache.commons.cli.Options;

public class MyPlugin implements CliPlugin {

    @Override
    public String getPluginName() {
        return "my-plugin";
    }

    @Override
    public String getPluginVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Additional commands for data processing";
    }

    @Override
    public void registerCommands(CommandRepository repository) {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                .longOpt("input")
                .hasArg(true)
                .argName("input")
                .desc("Input file path.")
                .required(true)
                .build());

        repository.addCommand(new CommandDefinition(
                "convert", opts, "Converts input data to another format.",
                "Data Processing",
                cl -> {
                    String input = cl.getOptionValue("input");
                    // ... conversion logic
                },
                getPluginName()));
    }
}
```

Pass `getPluginName()` to the `pluginName` field of `CommandDefinition` so that the command can be tracked back to its originating plugin.

### Registering with ServiceLoader

Create the following file in your plugin JAR:

`META-INF/services/com.scivicslab.pluggablecli.CliPlugin`:
```
com.example.myplugin.MyPlugin
```

### Loading plugins in the application

Use `PluginLoader` to discover and register all plugins from the classpath.

```java
import com.scivicslab.pluggablecli.CommandRepository;
import com.scivicslab.pluggablecli.PluginLoader;

public class App {

    String synopsis = "my-tool <command> <options>";
    CommandRepository cmds = new CommandRepository();
    PluginLoader pluginLoader = new PluginLoader(cmds);

    public static void main(String[] args) {
        App app = new App();

        // Built-in commands
        app.setupCommands();

        // Discover and load plugins from classpath
        int loaded = app.pluginLoader.loadPlugins();
        System.err.println(loaded + " plugin(s) loaded.");

        // ... parse and execute (same as before)
    }
}
```

### Unloading plugins

Unload a plugin by name. All commands registered by that plugin are removed.

```java
pluginLoader.unloadPlugin("my-plugin");
// "convert" command is no longer available
```

### Listing loaded plugins

```java
for (var entry : pluginLoader.getLoadedPlugins().entrySet()) {
    CliPlugin p = entry.getValue();
    System.out.println(p.getPluginName() + " " + p.getPluginVersion()
            + " - " + p.getDescription());
}
```

## Building a Fat JAR

Configure `maven-shade-plugin` with `ServicesResourceTransformer` so that `META-INF/services` files from multiple JARs are merged correctly and plugins are discovered even in a fat JAR.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <configuration>
        <transformers>
            <transformer implementation=
                "org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
        </transformers>
    </configuration>
</plugin>
```

## Requirements

- Java 21+
- Apache Commons CLI 1.6.0

## License

Apache License 2.0
