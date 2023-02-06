package ru.yandex.direct.manualtests.tasks.classdiff;

import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.multitype.typesupport.TypeSupport;

public class CoreClassesDiffResult {
    private final Class<?> first;
    private final Class<?> second;
    private final Set<Class<?>> firstUniqueSupers;
    private final Set<Class<?>> secondUniqueSupers;
    private final Set<ModelProperty<?, ?>> firstUniqueFields;
    private final Set<ModelProperty<?, ?>> secondUniqueFields;
    private final Set<String> firstUniqueFieldsNames;
    private final Set<String> secondUniqueFieldsNames;
    private final Set<Class<? extends TypeSupport<?>>> firstUniqueTypeSupports;
    private final Set<Class<? extends TypeSupport<?>>> secondUniqueTypeSupports;


    public CoreClassesDiffResult(
            Class<?> first, Class<?> second,
            Set<Class<?>> firstUniqueSupers,
            Set<Class<?>> secondUniqueSupers,
            Set<ModelProperty<?, ?>> firstUniqueFields,
            Set<ModelProperty<?, ?>> secondUniqueFields,
            Set<Class<? extends TypeSupport<?>>> firstUniqueTypeSupports,
            Set<Class<? extends TypeSupport<?>>> secondUniqueTypeSupports) {
        this.first = first;
        this.second = second;
        this.firstUniqueSupers = firstUniqueSupers;
        this.secondUniqueSupers = secondUniqueSupers;
        this.firstUniqueFields = firstUniqueFields;
        this.secondUniqueFields = secondUniqueFields;
        this.firstUniqueFieldsNames = firstUniqueFields.stream().map(ModelProperty::name).collect(Collectors.toSet());
        this.secondUniqueFieldsNames = secondUniqueFields.stream().map(ModelProperty::name).collect(Collectors.toSet());
        this.firstUniqueTypeSupports = firstUniqueTypeSupports;
        this.secondUniqueTypeSupports = secondUniqueTypeSupports;
    }

    public Class<?> getFirst() {
        return first;
    }

    public Class<?> getSecond() {
        return second;
    }

    public Set<Class<?>> getFirstUniqueSupers() {
        return firstUniqueSupers;
    }

    public Set<Class<?>> getSecondUniqueSupers() {
        return secondUniqueSupers;
    }

    public Set<ModelProperty<?, ?>> getFirstUniqueFields() {
        return firstUniqueFields;
    }

    public Set<ModelProperty<?, ?>> getSecondUniqueFields() {
        return secondUniqueFields;
    }

    public Set<String> getFirstUniqueFieldsNames() {
        return firstUniqueFieldsNames;
    }

    public Set<String> getSecondUniqueFieldsNames() {
        return secondUniqueFieldsNames;
    }

    public Set<Class<? extends TypeSupport<?>>> getFirstUniqueTypeSupports() {
        return firstUniqueTypeSupports;
    }

    public Set<Class<? extends TypeSupport<?>>> getSecondUniqueTypeSupports() {
        return secondUniqueTypeSupports;
    }
}
