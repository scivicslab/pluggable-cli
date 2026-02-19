package com.scivicslab.pluggablecli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.cli.Options;

/**
 * Custom help formatter that renders help output as an ordered list of sections.
 * Sections can represent the computed usage string, option table, the command description,
 * or any arbitrary custom text supplied by the caller.
 *
 * <p>Ported from {@code UtilityCliHelpFormatter} (Utility-cli) with package relocation.
 */
public class HelpFormatter extends org.apache.commons.cli.HelpFormatter {

    /**
     * Immutable section descriptor rendered by this formatter.
     */
    public static final class Section {

        /** Section kinds supported by the formatter. */
        public enum Kind {
            USAGE,
            OPTIONS,
            COMMAND_DESCRIPTION,
            CUSTOM
        }

        private final Kind kind;
        private final String heading;
        private final List<String> lines;

        private Section(Kind kind, String heading, List<String> lines) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.heading = heading;
            this.lines = lines == null ? List.of() : List.copyOf(lines);
        }

        /**
         * Creates a usage section that will be populated via
         * {@link org.apache.commons.cli.HelpFormatter#printUsage}.
         *
         * @param heading heading label to print before the generated usage text
         * @return usage section descriptor
         */
        public static Section usage(String heading) {
            return new Section(Kind.USAGE, heading, null);
        }

        /**
         * Creates an options section that reuses
         * {@link org.apache.commons.cli.HelpFormatter#printOptions} for rendering.
         *
         * @param heading heading label to print above the options table
         * @return options section descriptor
         */
        public static Section options(String heading) {
            return new Section(Kind.OPTIONS, heading, null);
        }

        /**
         * Creates a section that prints the command description registered in the repository.
         *
         * @param heading heading label to print
         * @return description section descriptor
         */
        public static Section commandDescription(String heading) {
            return new Section(Kind.COMMAND_DESCRIPTION, heading, null);
        }

        /**
         * Creates an arbitrary custom section with caller-defined content lines.
         *
         * @param heading heading label to print (may be {@code null} to suppress the heading)
         * @param lines   content lines; each element can contain embedded newlines
         * @return custom section descriptor
         */
        public static Section custom(String heading, Collection<String> lines) {
            List<String> normalized = lines == null ? List.of() : new ArrayList<>(lines);
            return new Section(Kind.CUSTOM, heading, normalized);
        }

        Kind kind() {
            return this.kind;
        }

        String heading() {
            return this.heading;
        }

        List<String> lines() {
            return this.lines;
        }
    }

    private List<Section> sections = List.of();

    public HelpFormatter() {
        setWidth(100);
        setLeftPadding(4);
        setDescPadding(2);
    }

    HelpFormatter sections(List<Section> sections) {
        this.sections = sections == null ? List.of() : List.copyOf(sections);
        return this;
    }

    List<Section> sections() {
        return this.sections;
    }

    /**
     * Prints the ordered help sections to the supplied writer.
     *
     * @param out                destination writer
     * @param command            command identifier (used for the usage line)
     * @param commandOptions     options registered for the command (never {@code null})
     * @param commandDescription textual description registered for the command, may be {@code null}
     */
    public void printCommandHelp(PrintWriter out, String command, Options commandOptions,
            String commandDescription) {
        Objects.requireNonNull(out, "out");

        Options safeOptions = commandOptions != null ? commandOptions : new Options();
        String safeCommand = command == null ? "" : command;
        String description = commandDescription;

        List<Section> effectiveSections = resolveSections(this.sections, description,
                !safeOptions.getOptions().isEmpty());

        for (int i = 0; i < effectiveSections.size(); i++) {
            Section section = effectiveSections.get(i);
            switch (section.kind()) {
            case USAGE:
                printUsageSection(out, section.heading(), safeCommand, safeOptions);
                break;
            case OPTIONS:
                printOptionsSection(out, section.heading(), safeOptions);
                break;
            case COMMAND_DESCRIPTION:
                printCommandDescriptionSection(out, section.heading(), description);
                break;
            case CUSTOM:
                printCustomSection(out, section.heading(), section.lines());
                break;
            default:
                throw new IllegalStateException("Unhandled section type: " + section.kind());
            }

            if (i < effectiveSections.size() - 1) {
                out.println();
            }
        }

        out.flush();
    }

    private List<Section> resolveSections(List<Section> configuredSections, String description,
            boolean hasOptions) {
        List<Section> result = new ArrayList<>();
        boolean usagePresent = false;
        boolean optionsPresent = false;
        boolean descriptionPlaceholderPresent = false;

        if (configuredSections != null) {
            for (Section section : configuredSections) {
                switch (section.kind()) {
                case USAGE:
                    usagePresent = true;
                    break;
                case OPTIONS:
                    optionsPresent = true;
                    break;
                case COMMAND_DESCRIPTION:
                    descriptionPlaceholderPresent = true;
                    break;
                default:
                    break;
                }
                result.add(section);
            }
        }

        if (!usagePresent) {
            result.add(0, Section.usage("Usage"));
        }

        if (description != null && !description.isBlank() && !descriptionPlaceholderPresent
                && configuredSections != null && configuredSections.isEmpty()) {
            result.add(1, Section.commandDescription("Description"));
            descriptionPlaceholderPresent = true;
        }

        if (hasOptions && !optionsPresent) {
            result.add(Section.options("Options"));
        }

        return result;
    }

    private void printUsageSection(PrintWriter out, String heading, String command, Options options) {
        if (command == null || command.isBlank()) {
            return;
        }

        printHeading(out, heading);
        StringWriter buffer = new StringWriter();
        PrintWriter bufferWriter = new PrintWriter(buffer);
        super.printUsage(bufferWriter, getWidth(), command, options);
        bufferWriter.flush();
        printIndentedBlock(out, buffer.toString());
    }

    private void printOptionsSection(PrintWriter out, String heading, Options options) {
        if (options == null || options.getOptions().isEmpty()) {
            return;
        }
        printHeading(out, heading);
        super.printOptions(out, getWidth(), options, getLeftPadding(), getDescPadding());
    }

    private void printCommandDescriptionSection(PrintWriter out, String heading, String description) {
        if (description == null || description.isBlank()) {
            return;
        }
        printHeading(out, heading);
        printMultilineText(out, description);
    }

    private void printCustomSection(PrintWriter out, String heading, List<String> blocks) {
        if ((blocks == null || blocks.isEmpty()) && (heading == null || heading.isBlank())) {
            return;
        }
        printHeading(out, heading);
        if (blocks != null) {
            for (String block : blocks) {
                printMultilineText(out, block);
            }
        }
    }

    private void printHeading(PrintWriter out, String heading) {
        if (heading != null && !heading.isBlank()) {
            out.println(heading + ":");
        }
    }

    private void printMultilineText(PrintWriter out, String text) {
        if (text == null) {
            return;
        }
        String normalized = normalizeLineEndings(text);
        String[] lines = normalized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                out.println();
            } else {
                printWrapped(out, getWidth(), "  " + line);
            }
        }
    }

    private void printIndentedBlock(PrintWriter out, String text) {
        String normalized = normalizeLineEndings(text);
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            if (!line.isBlank()) {
                out.println("  " + line);
            }
        }
    }

    private String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }
}
