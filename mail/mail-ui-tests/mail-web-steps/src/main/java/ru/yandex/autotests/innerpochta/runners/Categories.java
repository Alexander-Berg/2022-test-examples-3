package ru.yandex.autotests.innerpochta.runners;

import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Раннер, который позволяет запускать (или наоборот исключать) только определенные тесты из тестовых классов.
 * Для этого нужно создать отдельный класс категории,
 * например, {@link ru.yandex.autotests.innerpochta.tests.suites.categories.SmokeTests}.
 * <p>
 * Для того, чтобы пометить категорией тест или класс, нужно использовать аннотацию &#064;Category(SmokeTests.class).
 * Тесты по категории запускаются при помощи сьюта, пример можно посмотреть по ссылке:
 * {@link ru.yandex.autotests.innerpochta.tests.suites.SmokeSuite}.
 * <p>
 * Конструктор этого класса переписан так, чтобы не нужно было указывать классы, в которых необходимо искать категории.
 * При инициализации сразу все классы пакета ru.yandex.autotests.innerpochta.tests будут переданы в сьют.
 * <p>
 * В классе изменен только конструктор и добавлен метод {@link Categories#suite()}, работу остальных функций можно
 * посмотреть в официальной документации к Categories в JUnit 4.14
 * <p>
 * @version 4.12
 * @see <a href="https://github.com/junit-team/junit/wiki/Categories">Подробнее на официальной вики JUnit</a>
 */
public class Categories extends Suite {

    /**
     * Конструктор, вызывает {@link Categories#suite()} для поиска всех классов и потом делает из них сьют.
     * После чего отфильтровывает все тесты, которые не имеют соответствующую категорию, оставшиеся тесты
     * попадают в ран.
     */
    public Categories(Class<?> klass) throws InitializationError {
        super(klass, suite());

        try {
            Set<Class<?>> included = getIncludedCategory(klass);
            Set<Class<?>> excluded = getExcludedCategory(klass);
            System.out.println(included.toString());
            System.out.println(excluded.toString());
            boolean isAnyIncluded = isAnyIncluded(klass);
            boolean isAnyExcluded = isAnyExcluded(klass);
            this.filter(CategoryFilter.categoryFilter(isAnyIncluded, included, isAnyExcluded, excluded));
        } catch (NoTestsRemainException var7) {
            throw new InitializationError(var7);
        }

        assertNoCategorizedDescendentsOfUncategorizeableParents(this.getDescription());
    }

    /**
     * Метод, который проходит по ru.yandex.autotests.innerpochta.tests в поисках классов и выбирает только те,
     * которые имеют слово Test в названии.
     *
     * @return список всех тестовых классов пакета.
     */
    private static Class[] suite() {
        String packageName = "classpath:ru/yandex/autotests/innerpochta/tests/**/*";
        List<Class> classes = new LinkedList<Class>();
        PathMatchingResourcePatternResolver scanner = new PathMatchingResourcePatternResolver();
        Resource[] resources = new Resource[0];
        try {
            resources = scanner.getResources(packageName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Resource resource : resources) {
            try {
                String resourceUri = resource.getURI().toString();
                if (resourceUri.contains("Test")) {
                    resourceUri =
                        resourceUri.substring(resourceUri.indexOf("ru/yandex")).replace(".class", "").replace("/", ".");
                    classes.add(Class.forName(resourceUri));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        System.out.println(classes.size());
        System.out.println(classes);
        return classes.toArray(new Class[0]);
    }

    private static Set<Class<?>> getIncludedCategory(Class<?> klass) {
        IncludeCategory annotation = (IncludeCategory) klass.getAnnotation(IncludeCategory.class);
        return createSet(annotation == null ? null : annotation.value());
    }

    private static boolean isAnyIncluded(Class<?> klass) {
        IncludeCategory annotation = (IncludeCategory) klass.getAnnotation(IncludeCategory.class);
        return annotation == null || annotation.matchAny();
    }

    private static Set<Class<?>> getExcludedCategory(Class<?> klass) {
        ExcludeCategory annotation = (ExcludeCategory) klass.getAnnotation(ExcludeCategory.class);
        return createSet(annotation == null ? null : annotation.value());
    }

    private static boolean isAnyExcluded(Class<?> klass) {
        ExcludeCategory annotation = (ExcludeCategory) klass.getAnnotation(ExcludeCategory.class);
        return annotation == null || annotation.matchAny();
    }

    private static void assertNoCategorizedDescendentsOfUncategorizeableParents(Description description) throws InitializationError {
        if (!canHaveCategorizedChildren(description)) {
            assertNoDescendantsHaveCategoryAnnotations(description);
        }

        Iterator i$ = description.getChildren().iterator();

        while (i$.hasNext()) {
            Description each = (Description) i$.next();
            assertNoCategorizedDescendentsOfUncategorizeableParents(each);
        }

    }

    private static void assertNoDescendantsHaveCategoryAnnotations(Description description) throws InitializationError {
        Iterator i$ = description.getChildren().iterator();

        while (i$.hasNext()) {
            Description each = (Description) i$.next();
            if (each.getAnnotation(Category.class) != null) {
                throw new InitializationError(
                    "Category annotations on Parameterized classes are not supported on individual methods.");
            }

            assertNoDescendantsHaveCategoryAnnotations(each);
        }

    }

    private static boolean canHaveCategorizedChildren(Description description) {
        Iterator i$ = description.getChildren().iterator();

        Description each;
        do {
            if (!i$.hasNext()) {
                return true;
            }

            each = (Description) i$.next();
        } while (each.getTestClass() != null);

        return false;
    }

    private static boolean hasAssignableTo(Set<Class<?>> assigns, Class<?> to) {
        Iterator i$ = assigns.iterator();

        Class from;
        do {
            if (!i$.hasNext()) {
                return false;
            }

            from = (Class) i$.next();
        } while (!to.isAssignableFrom(from));

        return true;
    }

    private static Set<Class<?>> createSet(Class<?>... t) {
        Set<Class<?>> set = new HashSet();
        if (t != null) {
            Collections.addAll(set, t);
        }

        return set;
    }

    public static class CategoryFilter extends Filter {
        private final Set<Class<?>> included;
        private final Set<Class<?>> excluded;
        private final boolean includedAny;
        private final boolean excludedAny;

        public static CategoryFilter include(boolean matchAny, Class<?>... categories) {
            if (hasNull(categories)) {
                throw new NullPointerException("has null category");
            } else {
                return categoryFilter(
                    matchAny, createSet(categories), true, (Set) null);
            }
        }

        public static CategoryFilter include(Class<?> category) {
            return include(true, category);
        }

        public static CategoryFilter include(Class<?>... categories) {
            return include(true, categories);
        }

        public static CategoryFilter exclude(boolean matchAny, Class<?>... categories) {
            if (hasNull(categories)) {
                throw new NullPointerException("has null category");
            } else {
                return categoryFilter(
                    true, (Set) null, matchAny, createSet(categories));
            }
        }

        public static CategoryFilter exclude(Class<?> category) {
            return exclude(true, category);
        }

        public static CategoryFilter exclude(Class<?>... categories) {
            return exclude(true, categories);
        }

        public static CategoryFilter categoryFilter(boolean matchAnyInclusions, Set<Class<?>> inclusions,
                                                    boolean matchAnyExclusions, Set<Class<?>> exclusions) {
            return new CategoryFilter(
                matchAnyInclusions, inclusions, matchAnyExclusions, exclusions);
        }

        protected CategoryFilter(boolean matchAnyIncludes, Set<Class<?>> includes, boolean matchAnyExcludes,
                                 Set<Class<?>> excludes) {
            this.includedAny = matchAnyIncludes;
            this.excludedAny = matchAnyExcludes;
            this.included = copyAndRefine(includes);
            this.excluded = copyAndRefine(excludes);
        }

        public String describe() {
            return this.toString();
        }

        /**
         * Returns string in the form <tt>&quot;[included categories] - [excluded categories]&quot;</tt>, where both
         * sets have comma separated names of categories.
         *
         * @return string representation for the relative complement of excluded categories set
         * in the set of included categories. Examples:
         * <ul>
         * <li> <tt>&quot;categories [all]&quot;</tt> for all included categories and no excluded ones;
         * <li> <tt>&quot;categories [all] - [A, B]&quot;</tt> for all included categories and given excluded ones;
         * <li> <tt>&quot;categories [A, B] - [C, D]&quot;</tt> for given included categories and given excluded ones.
         * </ul>
         * @see Class#toString() name of category
         */
        public String toString() {
            StringBuilder description = (new StringBuilder("categories "))
                .append(this.included.isEmpty() ? "[all]" : this.included);
            if (!this.excluded.isEmpty()) {
                description.append(" - ").append(this.excluded);
            }

            return description.toString();
        }

        public boolean shouldRun(Description description) {
            if (this.hasCorrectCategoryAnnotation(description)) {
                return true;
            } else {
                Iterator i$ = description.getChildren().iterator();

                Description each;
                do {
                    if (!i$.hasNext()) {
                        return false;
                    }

                    each = (Description) i$.next();
                } while (!this.shouldRun(each));

                return true;
            }
        }

        private boolean hasCorrectCategoryAnnotation(Description description) {
            Set<Class<?>> childCategories = categories(description);

            // If a child has no categories, immediately return.
            if (childCategories.isEmpty()) {
                return this.included.isEmpty();
            } else {
                if (!this.excluded.isEmpty()) {
                    if (this.excludedAny) {
                        if (this.matchesAnyParentCategories(childCategories, this.excluded)) {
                            return false;
                        }
                    } else if (this.matchesAllParentCategories(childCategories, this.excluded)) {
                        return false;
                    }
                }

                if (this.included.isEmpty()) {
                    // Couldn't be excluded, and with no suite's included categories treated as should run.
                    return true;
                } else {
                    return this.includedAny ? this.matchesAnyParentCategories(childCategories, this.included) : this
                        .matchesAllParentCategories(childCategories, this.included);
                }
            }
        }

        /**
         * @return <tt>true</tt> if at least one (any) parent category match a child, otherwise <tt>false</tt>.
         * If empty <tt>parentCategories</tt>, returns <tt>false</tt>.
         */
        private boolean matchesAnyParentCategories(Set<Class<?>> childCategories, Set<Class<?>> parentCategories) {
            Iterator i$ = parentCategories.iterator();

            Class parentCategory;
            do {
                if (!i$.hasNext()) {
                    return false;
                }

                parentCategory = (Class) i$.next();
            } while (!hasAssignableTo(childCategories, parentCategory));

            return true;
        }

        /**
         * @return <tt>false</tt> if at least one parent category does not match children, otherwise <tt>true</tt>.
         * If empty <tt>parentCategories</tt>, returns <tt>true</tt>.
         */
        private boolean matchesAllParentCategories(Set<Class<?>> childCategories, Set<Class<?>> parentCategories) {
            Iterator i$ = parentCategories.iterator();

            Class parentCategory;
            do {
                if (!i$.hasNext()) {
                    return true;
                }

                parentCategory = (Class) i$.next();
            } while (hasAssignableTo(childCategories, parentCategory));

            return false;
        }

        private static Set<Class<?>> categories(Description description) {
            Set<Class<?>> categories = new HashSet();
            Collections.addAll(categories, directCategories(description));
            Collections.addAll(categories, directCategories(parentDescription(description)));
            return categories;
        }

        private static Description parentDescription(Description description) {
            Class<?> testClass = description.getTestClass();
            return testClass == null ? null : Description.createSuiteDescription(testClass);
        }

        private static Class<?>[] directCategories(Description description) {
            if (description == null) {
                return new Class[0];
            } else {
                Category annotation = (Category) description.getAnnotation(Category.class);
                return annotation == null ? new Class[0] : annotation.value();
            }
        }

        private static Set<Class<?>> copyAndRefine(Set<Class<?>> classes) {
            HashSet<Class<?>> c = new HashSet();
            if (classes != null) {
                c.addAll(classes);
            }

            c.remove((Object) null);
            return c;
        }

        private static boolean hasNull(Class<?>... classes) {
            if (classes == null) {
                return false;
            } else {
                Class[] arr$ = classes;
                int len$ = classes.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    Class<?> clazz = arr$[i$];
                    if (clazz == null) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExcludeCategory {
        /**
         * Determines the tests which do not run if they are annotated with categories specified in the
         * value of this annotation or their subtypes regardless of being included in {@link IncludeCategory#value()}.
         */
        Class<?>[] value() default {};

        /**
         * If <tt>true</tt>, the tests annotated with <em>any</em> of the categories in {@link ExcludeCategory#value()}
         * do not run. Otherwise, the tests do not run if and only if annotated with <em>all</em> categories.
         */
        boolean matchAny() default true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface IncludeCategory {
        /**
         * Determines the tests to run that are annotated with categories specified in
         * the value of this annotation or their subtypes unless excluded with {@link ExcludeCategory}.
         */
        Class<?>[] value() default {};

        /**
         * If <tt>true</tt>, runs tests annotated with <em>any</em> of the categories in
         * {@link IncludeCategory#value()}. Otherwise, runs tests only if annotated with <em>all</em> of the categories.
         */
        boolean matchAny() default true;
    }
}

