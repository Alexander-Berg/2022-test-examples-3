package ru.yandex.market.mbo.gwt.models.visual;

import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplate;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplateType;

/**
 * @author dmserebr
 * @date 20.11.18
 */
public final class OutputTemplateBuilder {
    private long id;
    private String name;
    private String comment;
    private String content;
    private String draft;
    private OutputTemplateType type;

    private OutputTemplateBuilder() {
    }

    public static OutputTemplateBuilder newBuilder() {
        return new OutputTemplateBuilder();
    }

    public OutputTemplateBuilder id(long id) {
        this.id = id;
        return this;
    }

    public OutputTemplateBuilder name(String name) {
        this.name = name;
        return this;
    }

    public OutputTemplateBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public OutputTemplateBuilder content(String content) {
        this.content = content;
        return this;
    }

    public OutputTemplateBuilder draft(String draft) {
        this.draft = draft;
        return this;
    }

    public OutputTemplateBuilder type(OutputTemplateType type) {
        this.type = type;
        return this;
    }

    public OutputTemplate build() {
        OutputTemplate outputTemplate = new OutputTemplate();
        outputTemplate.setId(id);
        outputTemplate.setName(name);
        outputTemplate.setComment(comment);
        outputTemplate.setContent(content);
        outputTemplate.setDraft(draft);
        outputTemplate.setType(type);
        return outputTemplate;
    }
}
