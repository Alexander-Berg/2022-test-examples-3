package ru.yandex.direct.model;

public class TestBrokenPropertiesClass implements ModelWithId {

    public static final ModelProperty<TestBrokenPropertiesClass, Long> X =
            ModelProperty.create(TestBrokenPropertiesClass.class, "xxx",
                    TestBrokenPropertiesClass::getId, TestBrokenPropertiesClass::setId);

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public void setId(Long id) {
    }
}
