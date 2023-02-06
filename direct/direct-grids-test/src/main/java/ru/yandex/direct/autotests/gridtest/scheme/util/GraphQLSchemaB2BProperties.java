package ru.yandex.direct.autotests.gridtest.scheme.util;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

@Resource.Classpath("graphql.schema.b2b.properties")
public class GraphQLSchemaB2BProperties {
    private static GraphQLSchemaB2BProperties properties;

    public static GraphQLSchemaB2BProperties get() {
        if (properties == null) {
            properties = new GraphQLSchemaB2BProperties();
            PropertyLoader.populate(properties);
        }
        return properties;
    }

    @Property("stage.stable")
    protected String stableStage = "8080";

    @Property("stage.prestable")
    protected String prestableStage = "8998";

    @Property("ignore.new.fields")
    protected boolean ignoreNewFields = false;

    @Property("fields.to.ignore")
    protected String fieldsToIgnore = null;

    public String getStableStage() {
        return stableStage;
    }

    public String getPrestableStage() {
        return prestableStage;
    }

    public boolean isIgnoreNewFields() {
        return ignoreNewFields;
    }

    public String getFieldsToIgnore() {
        return fieldsToIgnore;
    }
}