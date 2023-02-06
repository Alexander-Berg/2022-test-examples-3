package ru.yandex.market.dao;

import ru.yandex.market.CategoryTree;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Отображает Entry в значение какого-либо поля {@link CategoryTree.CategoryTreeNode}.
 * Фильтрует: например, нет смысла сравнивать Entry на предмет {@link CategoryTree.CategoryTreeNode#guruLightCategoryId},
 * если категории кластеризуемы.
 * Содержит имя своего поля.
 *
 * @param <R>
 */
class NamedFn<R> implements Function<Map.Entry<CategoryTree.CategoryTreeNode, Integer>, R>, Predicate<Map.Entry<CategoryTree.CategoryTreeNode, Integer>> {
    private final String name;
    private final Function<Map.Entry<CategoryTree.CategoryTreeNode, Integer>, R> function;
    private final Predicate<? super Map.Entry<CategoryTree.CategoryTreeNode, Integer>> predicate;

    NamedFn(String name, Function<Map.Entry<CategoryTree.CategoryTreeNode, Integer>, R> function) {
        this(name, function, x -> true);
    }

    NamedFn(String name, Function<Map.Entry<CategoryTree.CategoryTreeNode, Integer>, R> function, Predicate<? super Map.Entry<CategoryTree.CategoryTreeNode, Integer>> predicate) {
        this.name = name;
        this.function = function;
        this.predicate = predicate;
    }

    String getName() {
        return name;
    }

    @Override
    public R apply(Map.Entry<CategoryTree.CategoryTreeNode, Integer> categoryTreeNode) {
        return function.apply(categoryTreeNode);
    }

    @Override
    public String toString() {
        return "NamedFn{" +
            "name='" + name + '\'' +
            ", function=" + function +
            '}';
    }

    @Override
    public boolean test(Map.Entry<CategoryTree.CategoryTreeNode, Integer> entry) {
        return predicate.test(entry);
    }
}
