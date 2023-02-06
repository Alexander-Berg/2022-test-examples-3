package ru.yandex.direct.operation.testing.entity;

import java.math.BigDecimal;

import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;

public class Keyword implements Model {
    public static final ModelProperty<Keyword, String> PHRASE =
            ModelProperty.create(Keyword.class, "phrase", Keyword::getPhrase, Keyword::setPhrase);
    public static final ModelProperty<Keyword, BigDecimal> PRICE =
            ModelProperty.create(Keyword.class, "price", Keyword::getPrice, Keyword::setPrice);
    private String phrase;

    private BigDecimal price;

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
