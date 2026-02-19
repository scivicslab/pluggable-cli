package com.scivicslab.pluggablecli;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandRepositoryTest {

    private CommandRepository repo;

    @BeforeEach
    void setUp() {
        repo = new CommandRepository();
    }

    // -----------------------------------------------------------------------
    // addCommand / hasCommand
    // -----------------------------------------------------------------------

    @Test
    void addCommandWithNameAndOptions() {
        repo.addCommand("greet", new Options());
        assertTrue(repo.hasCommand("greet"));
        assertEquals(1, repo.size());
    }

    @Test
    void addCommandWithAction() {
        AtomicReference<String> called = new AtomicReference<>();
        repo.addCommand("run", new Options(), cl -> called.set("executed"));

        assertTrue(repo.hasCommand("run"));
        CommandDefinition def = repo.getCommand("run");
        assertNotNull(def.action());
    }

    @Test
    void addCommandWithDescription() {
        repo.addCommand("info", new Options(), "Show information");
        CommandDefinition def = repo.getCommand("info");
        assertEquals("Show information", def.description());
        assertEquals(CommandDefinition.DEFAULT_CATEGORY, def.category());
    }

    @Test
    void addCommandWithDescriptionAndAction() {
        repo.addCommand("deploy", new Options(), "Deploy the app", cl -> {});
        CommandDefinition def = repo.getCommand("deploy");
        assertEquals("Deploy the app", def.description());
        assertNotNull(def.action());
    }

    @Test
    void addCommandWithCategory() {
        repo.addCommand("Admin", "reset", new Options());
        CommandDefinition def = repo.getCommand("reset");
        assertEquals("Admin", def.category());
    }

    @Test
    void addCommandWithCategoryAndAction() {
        repo.addCommand("Admin", "purge", new Options(), cl -> {});
        CommandDefinition def = repo.getCommand("purge");
        assertEquals("Admin", def.category());
        assertNotNull(def.action());
    }

    @Test
    void addCommandWithCategoryAndDescription() {
        repo.addCommand("IO", "export", new Options(), "Export data");
        CommandDefinition def = repo.getCommand("export");
        assertEquals("IO", def.category());
        assertEquals("Export data", def.description());
    }

    @Test
    void addCommandWithAllFields() {
        repo.addCommand("IO", "import", new Options(), "Import data", cl -> {});
        CommandDefinition def = repo.getCommand("import");
        assertEquals("IO", def.category());
        assertEquals("Import data", def.description());
        assertNotNull(def.action());
    }

    @Test
    void addCommandDefinitionDirectly() {
        CommandDefinition def = new CommandDefinition(
                "custom", new Options(), "Custom cmd", "Cat", null, "testPlugin");
        repo.addCommand(def);
        assertTrue(repo.hasCommand("custom"));
        assertEquals("testPlugin", repo.getCommand("custom").pluginName());
    }

    // -----------------------------------------------------------------------
    // removeCommand
    // -----------------------------------------------------------------------

    @Test
    void removeExistingCommand() {
        repo.addCommand("rm-me", new Options());
        CommandDefinition removed = repo.removeCommand("rm-me");
        assertNotNull(removed);
        assertEquals("rm-me", removed.name());
        assertFalse(repo.hasCommand("rm-me"));
    }

    @Test
    void removeNonExistentCommandReturnsNull() {
        assertNull(repo.removeCommand("no-such"));
    }

    // -----------------------------------------------------------------------
    // removeCommandsByPlugin
    // -----------------------------------------------------------------------

    @Test
    void removeCommandsByPlugin() {
        repo.addCommand(new CommandDefinition("a", new Options(), null, null, null, "p1"));
        repo.addCommand(new CommandDefinition("b", new Options(), null, null, null, "p1"));
        repo.addCommand(new CommandDefinition("c", new Options(), null, null, null, "p2"));

        int removed = repo.removeCommandsByPlugin("p1");
        assertEquals(2, removed);
        assertFalse(repo.hasCommand("a"));
        assertFalse(repo.hasCommand("b"));
        assertTrue(repo.hasCommand("c"));
    }

    @Test
    void removeCommandsByPluginWithNullReturnsZero() {
        assertEquals(0, repo.removeCommandsByPlugin(null));
    }

    // -----------------------------------------------------------------------
    // parse
    // -----------------------------------------------------------------------

    @Test
    void parseKnownCommand() throws ParseException {
        Options opts = new Options();
        opts.addOption(Option.builder("n").longOpt("name").hasArg().build());
        repo.addCommand("greet", opts);

        CommandLine cl = repo.parse(new String[]{"greet", "-n", "world"});
        assertNotNull(cl);
        assertEquals("greet", repo.getGivenCommand());
        assertEquals("world", cl.getOptionValue("n"));
        assertFalse(repo.isHelpRequested());
    }

    @Test
    void parseDetectsHelpFlag() throws ParseException {
        Options opts = new Options();
        opts.addOption(Option.builder("h").longOpt("help").hasArg(false).required(false).build());
        repo.addCommand("greet", opts);

        repo.parse(new String[]{"greet", "--help"});
        assertTrue(repo.isHelpRequested());
    }

    @Test
    void parseDetectsShortHelpFlag() throws ParseException {
        Options opts = new Options();
        opts.addOption(Option.builder("h").longOpt("help").hasArg(false).required(false).build());
        repo.addCommand("greet", opts);

        repo.parse(new String[]{"greet", "-h"});
        assertTrue(repo.isHelpRequested());
    }

    @Test
    void parseUnknownCommandFallsBackToUniversalOptions() throws ParseException {
        CommandLine cl = repo.parse(new String[]{"unknown", "--help"});
        assertNotNull(cl);
        assertEquals("unknown", repo.getGivenCommand());
        assertTrue(repo.isHelpRequested());
    }

    @Test
    void parseEmptyArgsReturnsNull() throws ParseException {
        CommandLine cl = repo.parse(new String[]{});
        assertNull(cl);
    }

    // -----------------------------------------------------------------------
    // execute
    // -----------------------------------------------------------------------

    @Test
    void executeInvokesAction() throws ParseException {
        AtomicReference<String> result = new AtomicReference<>();
        Options opts = new Options();
        opts.addOption(Option.builder("m").longOpt("msg").hasArg().build());
        repo.addCommand("say", opts, "Say something", cl -> result.set(cl.getOptionValue("m")));

        CommandLine cl = repo.parse(new String[]{"say", "-m", "hello"});
        repo.execute("say", cl);
        assertEquals("hello", result.get());
    }

    @Test
    void executeWithNoActionDoesNothing() throws ParseException {
        repo.addCommand("noop", new Options());
        CommandLine cl = repo.parse(new String[]{"noop"});
        // Should not throw
        repo.execute("noop", cl);
    }

    @Test
    void executeNonExistentCommandDoesNothing() {
        // Should not throw
        repo.execute("nonexistent", null);
    }

    // -----------------------------------------------------------------------
    // printCommandHelp (smoke test — just verify no exception)
    // -----------------------------------------------------------------------

    @Test
    void printCommandHelpDoesNotThrow() {
        Options opts = new Options();
        opts.addOption(Option.builder("v").longOpt("verbose").desc("Verbose output").build());
        repo.addCommand("build", opts, "Build the project");

        // Should not throw
        repo.printCommandHelp("build");
    }

    @Test
    void printCommandHelpForUnknownCommandDoesNotThrow() {
        repo.printCommandHelp("nonexistent");
    }
}
