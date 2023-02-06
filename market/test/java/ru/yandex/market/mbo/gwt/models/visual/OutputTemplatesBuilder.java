package ru.yandex.market.mbo.gwt.models.visual;

import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplate;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplates;
import ru.yandex.market.mbo.gwt.models.visual.templates.RangeFieldList;

/**
 * @author dmserebr
 * @date 20.11.18
 */
public final class OutputTemplatesBuilder {
    private OutputTemplate putModelTemplate;
    private OutputTemplate putBriefModelTemplate;
    private OutputTemplate putMicroModelTemplate;
    private OutputTemplate putMicroModelSearchTemplate;
    private OutputTemplate putFriendlyModelTemplate;
    private OutputTemplate seoTemplate;
    private OutputTemplate designGroupParams;
    private RangeFieldList rangeFields;
    private long categoryId;

    private OutputTemplatesBuilder() {
    }

    public static OutputTemplatesBuilder newBuilder() {
        return new OutputTemplatesBuilder();
    }

    public OutputTemplatesBuilder putModelTemplate(OutputTemplate putModelTemplate) {
        this.putModelTemplate = putModelTemplate;
        return this;
    }

    public OutputTemplatesBuilder putBriefModelTemplate(OutputTemplate putBriefModelTemplate) {
        this.putBriefModelTemplate = putBriefModelTemplate;
        return this;
    }

    public OutputTemplatesBuilder putMicroModelTemplate(OutputTemplate putMicroModelTemplate) {
        this.putMicroModelTemplate = putMicroModelTemplate;
        return this;
    }

    public OutputTemplatesBuilder putMicroModelSearchTemplate(OutputTemplate putMicroModelSearchTemplate) {
        this.putMicroModelSearchTemplate = putMicroModelSearchTemplate;
        return this;
    }

    public OutputTemplatesBuilder putFriendlyModelTemplate(OutputTemplate putFriendlyModelTemplate) {
        this.putFriendlyModelTemplate = putFriendlyModelTemplate;
        return this;
    }

    public OutputTemplatesBuilder seoTemplate(OutputTemplate seoTemplate) {
        this.seoTemplate = seoTemplate;
        return this;
    }

    public OutputTemplatesBuilder designGroupParams(OutputTemplate designGroupParams) {
        this.designGroupParams = designGroupParams;
        return this;
    }

    public OutputTemplatesBuilder rangeFields(RangeFieldList rangeFields) {
        this.rangeFields = rangeFields;
        return this;
    }

    public OutputTemplatesBuilder categoryId(long categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public OutputTemplates build() {
        OutputTemplates outputTemplates = new OutputTemplates();
        outputTemplates.setPutModelTemplate(putModelTemplate);
        outputTemplates.setPutBriefModelTemplate(putBriefModelTemplate);
        outputTemplates.setPutMicroModelTemplate(putMicroModelTemplate);
        outputTemplates.setPutMicroModelSearchTemplate(putMicroModelSearchTemplate);
        outputTemplates.setPutFriendlyModelTemplate(putFriendlyModelTemplate);
        outputTemplates.setSeoTemplate(seoTemplate);
        outputTemplates.setDesignGroupParams(designGroupParams);
        outputTemplates.setRangeFields(rangeFields);
        outputTemplates.setCategoryId(categoryId);
        return outputTemplates;
    }
}
