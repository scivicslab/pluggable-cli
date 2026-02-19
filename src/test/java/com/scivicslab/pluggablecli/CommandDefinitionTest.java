package com.scivicslab.pluggablecli;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;

class CommandDefinitionTest {

    @Test
    void constructWithAllFields() {
        Options opts = new Options();
        CommandDefinition def = new CommandDefinition(
                "test", opts, "A test command", "MyCategory", cl -> {}, "myPlugin");

        assertEquals("test", def.name());
        assertSame(opts, def.options());
        assertEquals("A test command", def.description());
        assertEquals("MyCategory", def.category());
        assertNotNull(def.action());
        assertEquals("myPlugin", def.pluginName());
    }

    @Test
    void nullCategoryDefaultsToZzOther() {
        CommandDefinition def = new CommandDefinition(
                "cmd", new Options(), "desc", null, null, null);
        assertEquals(CommandDefinition.DEFAULT_CATEGORY, def.category());
    }

    @Test
    void blankCategoryDefaultsToZzOther() {
        CommandDefinition def = new CommandDefinition(
                "cmd", new Options(), "desc", "  ", null, null);
        assertEquals(CommandDefinition.DEFAULT_CATEGORY, def.category());
    }

    @Test
    void nullNameThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> new CommandDefinition(null, new Options(), null, null, null, null));
    }

    @Test
    void nullOptionsThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> new CommandDefinition("cmd", null, null, null, null, null));
    }

    @Test
    void descriptionAndPluginNameCanBeNull() {
        CommandDefinition def = new CommandDefinition(
                "cmd", new Options(), null, null, null, null);
        assertNull(def.description());
        assertNull(def.pluginName());
    }

    @Test
    void recordEquality() {
        Options opts = new Options();
        CommandDefinition a = new CommandDefinition("cmd", opts, "desc", "Cat", null, null);
        CommandDefinition b = new CommandDefinition("cmd", opts, "desc", "Cat", null, null);
        assertEquals(a, b);
    }
}
