package ru.yandex.direct.model;

import java.math.BigDecimal;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TestClass implements ModelWithId {
    public static final ModelProperty<TestClass, Long> ID = prop("id",
            TestClass::getId, TestClass::setId);
    public static final ModelProperty<TestClass, String> NAME = prop("name",
            TestClass::getName, TestClass::setName);
    public static final ModelProperty<TestClass, String> DESCRIPTION = prop("description",
            TestClass::getDescription, TestClass::setDescription);
    public static final ModelProperty<TestClass, BigDecimal> PRICE = prop("price",
            TestClass::getPrice, TestClass::setPrice);

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    public TestClass() {
    }

    TestClass(Long id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    TestClass(Long id, String name) {
        this(id, name, null);
    }

    private static <V> ModelProperty<TestClass, V> prop(
            String name,
            Function<TestClass, V> getter,
            BiConsumer<TestClass, V> setter
    ) {
        return ModelProperty.create(TestClass.class, name, getter, setter);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
