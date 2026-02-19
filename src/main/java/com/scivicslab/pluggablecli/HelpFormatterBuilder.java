package com.scivicslab.pluggablecli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.scivicslab.pluggablecli.HelpFormatter.Section;

/**
 * Builder capturing layout configuration for {@link HelpFormatter}.
 * The builder stores an ordered list of sections and formatting parameters
 * (width, indentation) that are materialised into a formatter instance just before rendering.
 *
 * <p>Ported from {@code UtilityCliHelpFormatterBuilder} (Utility-cli) with package relocation.
 */
public class HelpFormatterBuilder {

    private final List<Section> sections = new ArrayList<>();
    private Integer width;
    private Integer leftPadding;
    private Integer descPadding;

    public HelpFormatterBuilder width(int value) {
        this.width = value;
        return this;
    }

    public HelpFormatterBuilder leftPadding(int value) {
        this.leftPadding = value;
        return this;
    }

    public HelpFormatterBuilder descPadding(int value) {
        this.descPadding = value;
        return this;
    }

    /**
     * Adds a usage section that will render the Commons CLI usage string.
     */
    public HelpFormatterBuilder addUsageSection(String heading) {
        this.sections.add(Section.usage(heading));
        return this;
    }

    /**
     * Adds an options section that renders all options registered for the command.
     */
    public HelpFormatterBuilder addOptionsSection(String heading) {
        this.sections.add(Section.options(heading));
        return this;
    }

    /**
     * Adds a section that prints the command description stored in the repository.
     */
    public HelpFormatterBuilder addCommandDescriptionSection(String heading) {
        this.sections.add(Section.commandDescription(heading));
        return this;
    }

    /**
     * Adds a custom section with arbitrary content. Each {@code line} may contain embedded
     * newlines for multi-line paragraphs.
     */
    public HelpFormatterBuilder addCustomSection(String heading, Collection<String> lines) {
        this.sections.add(Section.custom(heading, lines));
        return this;
    }

    /**
     * Removes any previously configured sections.
     */
    public HelpFormatterBuilder clearSections() {
        this.sections.clear();
        return this;
    }

    /**
     * @return {@code true} when at least one section has been configured
     */
    public boolean hasSections() {
        return !this.sections.isEmpty();
    }

    List<Section> getSections() {
        return List.copyOf(this.sections);
    }

    HelpFormatterBuilder replaceSections(List<Section> newSections) {
        this.sections.clear();
        if (newSections != null) {
            this.sections.addAll(newSections);
        }
        return this;
    }

    public HelpFormatterBuilder mergeFrom(HelpFormatterBuilder other) {
        if (other == null) {
            return this;
        }
        if (other.width != null) {
            this.width = other.width;
        }
        if (other.leftPadding != null) {
            this.leftPadding = other.leftPadding;
        }
        if (other.descPadding != null) {
            this.descPadding = other.descPadding;
        }
        if (!other.sections.isEmpty()) {
            this.sections.clear();
            this.sections.addAll(other.sections);
        }
        return this;
    }

    public HelpFormatterBuilder copy() {
        HelpFormatterBuilder copy = new HelpFormatterBuilder();
        copy.width = this.width;
        copy.leftPadding = this.leftPadding;
        copy.descPadding = this.descPadding;
        copy.sections.addAll(this.sections);
        return copy;
    }

    public HelpFormatter build() {
        HelpFormatter formatter = new HelpFormatter();
        if (this.width != null) {
            formatter.setWidth(this.width);
        }
        if (this.leftPadding != null) {
            formatter.setLeftPadding(this.leftPadding);
        }
        if (this.descPadding != null) {
            formatter.setDescPadding(this.descPadding);
        }
        formatter.sections(this.sections);
        return formatter;
    }
}
