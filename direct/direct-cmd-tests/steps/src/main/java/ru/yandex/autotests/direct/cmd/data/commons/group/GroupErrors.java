package ru.yandex.autotests.direct.cmd.data.commons.group;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by aleran on 01.02.2016.
 */
public class GroupErrors {

    @SerializedName("hierarchical_multipliers")
    private List<String> hierarchicalMultipliersErrors;

    public List<String> getHierarchicalMultipliersErrors() {
        return hierarchicalMultipliersErrors;
    }

}
