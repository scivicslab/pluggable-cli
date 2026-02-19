package com.scivicslab.pluggablecli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;

class HelpFormatterTest {

    @Test
    void defaultFormatterRendersUsageAndOptions() {
        HelpFormatter fmt = new HelpFormatter();
        Options opts = new Options();
        opts.addOption(Option.builder("v").longOpt("verbose").desc("Enable verbose output").build());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        fmt.printCommandHelp(pw, "myapp", opts, null);

        String output = sw.toString();
        assertTrue(output.contains("Usage:"), "should contain Usage heading");
        assertTrue(output.contains("myapp"), "should contain command name");
        assertTrue(output.contains("--verbose"), "should contain option");
    }

    @Test
    void descriptionSectionRendered() {
        HelpFormatter fmt = new HelpFormatterBuilder()
                .addUsageSection("Usage")
                .addCommandDescriptionSection("Description")
                .build();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        fmt.printCommandHelp(pw, "cmd", new Options(), "This is a test description.");

        String output = sw.toString();
        assertTrue(output.contains("Description:"), "should contain Description heading");
        assertTrue(output.contains("This is a test description."), "should contain description text");
    }

    @Test
    void customSectionRendered() {
        HelpFormatter fmt = new HelpFormatterBuilder()
                .addCustomSection("Examples", List.of("example1", "example2"))
                .build();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        fmt.printCommandHelp(pw, "cmd", new Options(), null);

        String output = sw.toString();
        assertTrue(output.contains("Examples:"), "should contain custom heading");
        assertTrue(output.contains("example1"), "should contain custom line 1");
        assertTrue(output.contains("example2"), "should contain custom line 2");
    }

    @Test
    void builderWidthApplied() {
        HelpFormatter fmt = new HelpFormatterBuilder().width(120).build();
        assertEquals(120, fmt.getWidth());
    }

    @Test
    void builderCopy() {
        HelpFormatterBuilder original = new HelpFormatterBuilder()
                .width(80)
                .addUsageSection("Usage");
        HelpFormatterBuilder copy = original.copy();
        copy.width(120);

        HelpFormatter origFmt = original.build();
        HelpFormatter copyFmt = copy.build();

        assertEquals(80, origFmt.getWidth());
        assertEquals(120, copyFmt.getWidth());
    }

    @Test
    void emptyCommandSkipsUsage() {
        HelpFormatter fmt = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        fmt.printCommandHelp(pw, "", new Options(), null);

        String output = sw.toString();
        // With empty command and no options, output should be minimal
        assertFalse(output.contains("usage:"));
    }
}
