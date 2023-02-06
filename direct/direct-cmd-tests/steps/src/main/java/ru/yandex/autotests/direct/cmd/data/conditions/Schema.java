package ru.yandex.autotests.direct.cmd.data.conditions;

import com.google.gson.annotations.SerializedName;

public class Schema {

    @SerializedName("definitions")
    private Definitions definitions;

    public Definitions getDefinitions() {
        return definitions;
    }

    public Schema withCheckedStruct(Definitions definitions) {
        this.definitions = definitions;
        return this;
    }
}
