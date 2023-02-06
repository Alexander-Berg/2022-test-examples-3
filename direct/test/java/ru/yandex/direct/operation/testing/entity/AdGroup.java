package ru.yandex.direct.operation.testing.entity;

import java.util.List;

import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.model.ModelWithId;

public class AdGroup implements ModelWithId {
    public static final ModelProperty<AdGroup, Long> ID =
            ModelProperty.create(AdGroup.class, "id", AdGroup::getId, AdGroup::setId);
    public static final ModelProperty<AdGroup, List<String>> MINUS_KEYWORDS =
            ModelProperty.create(AdGroup.class, "minusKeywords", AdGroup::getMinusKeywords, AdGroup::setMinusKeywords);



    private Long id;
    private List<String> minusKeywords;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }


    public List<String> getMinusKeywords() {
        return minusKeywords;
    }

    public void setMinusKeywords(List<String> minusKeywords) {
        this.minusKeywords = minusKeywords;
    }
}
