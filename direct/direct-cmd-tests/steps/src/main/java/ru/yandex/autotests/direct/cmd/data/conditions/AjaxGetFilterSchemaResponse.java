package ru.yandex.autotests.direct.cmd.data.conditions;

import com.google.gson.annotations.SerializedName;

public class AjaxGetFilterSchemaResponse {

    @SerializedName("schema")
    private Schema schema;

    public Schema getSchema() {
        return schema;
    }

    public AjaxGetFilterSchemaResponse withSchema(Schema schema) {
        this.schema = schema;
        return this;
    }
}
