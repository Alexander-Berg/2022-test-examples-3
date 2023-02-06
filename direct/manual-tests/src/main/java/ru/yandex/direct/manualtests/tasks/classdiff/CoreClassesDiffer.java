package ru.yandex.direct.manualtests.tasks.classdiff;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import one.util.streamex.StreamEx;

import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.multitype.typesupport.TypeSupport;

public class CoreClassesDiffer {
    public static CoreClassesDiffResult diffClasses(
            Class<?> first,
            Class<?> second,
            List<TypeSupport<?>> allTypeSupports,
            boolean recursive,
            boolean withoutPropHolders) {
        Set<ModelProperty<?, ?>> firstProperties = getModelProperties(first);
        Set<ModelProperty<?, ?>> secondProperties = getModelProperties(second);
        Set<ModelProperty<?, ?>> firstPropertiesCopy = new HashSet<>(firstProperties);
        Set<ModelProperty<?, ?>> secondPropertiesCopy = new HashSet<>(secondProperties);
        firstProperties.removeAll(secondPropertiesCopy);
        secondProperties.removeAll(firstPropertiesCopy);

        Set<Class<?>> firstSupers = findAllSupers(first, recursive, withoutPropHolders);
        Set<Class<?>> secondSupers = findAllSupers(second, recursive, withoutPropHolders);

        Set<Class<? extends TypeSupport<?>>> firstTypeSupports = getAvailableTypeSupports(allTypeSupports, firstSupers);
        Set<Class<? extends TypeSupport<?>>> secondTypeSupports = getAvailableTypeSupports(allTypeSupports, secondSupers);
        Set<Class<? extends TypeSupport<?>>> firstTypeSupportsCopy = new HashSet<>(firstTypeSupports);
        Set<Class<? extends TypeSupport<?>>> secondTypeSupportsCopy = new HashSet<>(secondTypeSupports);
        firstTypeSupports.removeAll(secondTypeSupportsCopy);
        secondTypeSupports.removeAll(firstTypeSupportsCopy);

        Set<Class<?>> firstSupersCopy = new HashSet<>(firstSupers);
        Set<Class<?>> secondSupersCopy = new HashSet<>(secondSupers);
        firstSupers.removeAll(secondSupersCopy);
        secondSupers.removeAll(firstSupersCopy);

        firstSupers = collapseClasses(firstSupers);
        secondSupers = collapseClasses(secondSupers);

        return new CoreClassesDiffResult(
                first, second,
                firstSupers, secondSupers,
                firstProperties, secondProperties,
                firstTypeSupports, secondTypeSupports);
    }

    private static Set<Class<? extends TypeSupport<?>>> getAvailableTypeSupports(List<TypeSupport<?>> allTypeSupports, Set<Class<?>> classes) {
        return (Set) StreamEx.of(allTypeSupports)
                .mapToEntry(TypeSupport::getTypeClass, Function.identity())
                .filterKeys(supportClass -> classes.stream().anyMatch(supportClass::isAssignableFrom))
                .values().map(Object::getClass)
                .toSet();
    }

    private static Set<Class<?>> collapseClasses(Set<Class<?>> classes) {
        Set<Class<?>> children = new HashSet<>();
        for (Class<?> child: classes) {
            for (Class<?> parent: classes) {
                if (parent != child && child.isAssignableFrom(parent)) {
                    children.add(child);
                }
            }
        }
        Set<Class<?>> uniqueParents = new HashSet<>(classes);
        uniqueParents.removeAll(children);
        return uniqueParents;
    }

    public static Set<ModelProperty<?, ?>> getModelProperties(Class<?> klass) {
        Set<ModelProperty<?, ?>> properties = new HashSet<>();
        try {
            properties = new HashSet<>((Set) klass.getDeclaredMethod("allModelProperties").invoke(null));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {

        }
        return properties;
    }

    public static Set<Class<?>> findAllSupers(Class<?> klass, boolean recursive, boolean withoutPropHolders) {
        Set<Class<?>> foundSupers = new HashSet<>();
        Queue<Class<?>> classesToCheck = new ArrayDeque<>();
        classesToCheck.add(klass);
        while (!classesToCheck.isEmpty()) {
            Class<?> currentClass = classesToCheck.poll();
            Class<?>[] interfaces = currentClass.getInterfaces();
            Class<?> superClass = currentClass.getSuperclass();
            for (Class<?> pretender : interfaces) {
                if (withoutPropHolders && pretender.getSimpleName().contains("PropHolder")) {
                    continue;
                }
                if (foundSupers.add(pretender)) {
                    classesToCheck.add(pretender);
                }
            }
            if (superClass != null && (!withoutPropHolders || !superClass.getSimpleName().contains("PropHolder")) &&
                    foundSupers.add(superClass)) {
                classesToCheck.add(superClass);
            }
            if (!recursive) {
                break;
            }
        }
        return foundSupers;
    }
}
