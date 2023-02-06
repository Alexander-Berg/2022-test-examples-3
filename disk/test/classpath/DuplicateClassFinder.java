package ru.yandex.chemodan.test.classpath;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nonnull;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ResourceList;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectionF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.misc.regex.Pattern2;

/**
 * @author Dmitriy Amelin (lemeh)
 */
@Value
@RequiredArgsConstructor
public class DuplicateClassFinder {
    private static final Pattern2 LIB_NAME_PATTERN = Pattern2.compile("(contrib/java/)(.+)$");

    ListF<DuplicatedComponentPair> duplicates;

    DuplicateClassFinder() {
        this(buildDuplicatePairs());
    }

    DuplicateClassFinder excludeComponents(CollectionF<ComponentMatcher> excludes) {
        return new DuplicateClassFinder(duplicates.filterNot(duplicate -> duplicate.matches(excludes)));
    }

    DuplicateClassFinder excludeClasses(CollectionF<String> excludes) {
        return new DuplicateClassFinder(duplicates.filterMap(duplicate -> duplicate.excludeClassesO(excludes)));
    }

    DuplicateClassFinder excludeClassesWithSameSize() {
        return new DuplicateClassFinder(duplicates.filterMap(DuplicatedComponentPair::excludeClassesWithSameSizeO));
    }

    private static ListF<DuplicatedComponentPair> buildDuplicatePairs() {
        ListF<ClassAggregate> aggregates =
                getClassAndComponentList()
                        .groupBy2()
                        .mapEntries(Component::new).map(Component::getClasses)
                        .<ComponentClassInfo>flatten()
                        .groupBy(ComponentClassInfo::getPath)
                        .filterValues(classes -> classes.size() > 1)
                        .values()
                        .map(ClassAggregate::new);
        return new Tuple2List<ComponentPair, ClassAggregate>(aggregates.map(ClassAggregate::getComponentPairs).flatten())
                .groupBy1()
                .mapEntries(DuplicatedComponentPair::new);
    }

    private static Tuple2List<ClassInfo, String> getClassAndComponentList() {
        return Cf.wrap(getClassGraphResourceList())
                .toTuple2List(resource -> new Tuple2<>(
                        new ClassInfo(resource.getPath(), resource.getLength()),
                        resource.getClasspathElementURL().toString()
                ));
    }

    private static ResourceList getClassGraphResourceList() {
        return new ClassGraph()
                .scan()
                .getAllResources()
                .classFilesOnly();
    }

    public boolean hasDuplicates() {
        return getDuplicates().isNotEmpty();
    }

    public String getFailMessage() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);
        printFailMessage(out);
        out.flush();
        return stringWriter.toString();
    }

    private void printFailMessage(PrintWriter out) {
        out.println("Has duplicate classes");

        duplicates.forEach(clash -> out.println(clash.getDescription()));

        out.println();

        duplicates.forEach(clash -> {
            out.println(clash.getDescription());
            clash.getDuplicateClasses()
                    .forEach(classAggregate -> out.println("\t" + classAggregate.getPath()));
        });
    }

    @Value
    private static class ClassInfo {
        String path;

        long size;

        public ComponentClassInfo withComponent(Component component) {
            return new ComponentClassInfo(this, component);
        }
    }

    @Value
    private static class ComponentClassInfo {
        ClassInfo classInfo;

        Component component;

        String getPath() {
            return classInfo.getPath();
        }

        long getSize() {
            return classInfo.getSize();
        }

        private boolean belongsTo(ComponentPair pair) {
            return component.belongsTo(pair);
        }
    }

    @Value
    public static class ClassAggregate {
        ListF<ComponentClassInfo> classes;

        public String getPath() {
            return classes.first()
                    .getPath();
        }

        public Tuple2List<ComponentPair, ClassAggregate> getComponentPairs() {
            return ComponentPair.getPermutations(classes.map(ComponentClassInfo::getComponent))
                    .zipWith(pair -> this);
        }

        @Override
        public int hashCode() {
            return getPath().hashCode();
        }

        public boolean matches(CollectionF<String> patterns) {
            return patterns.exists(this::matches);
        }

        public boolean matches(String pattern) {
            return getPath().matches(pattern) || getPath().contains(pattern);
        }

        public boolean hasSameSize(ComponentPair pair) {
            return classes.filter(cls -> cls.belongsTo(pair))
                    .map(ComponentClassInfo::getSize)
                    .unique()
                    .filter(size -> size > 0)
                    .size() == 1;
        }

    }

    @Value
    public static class Component implements Comparable<Component> {
        String path;

        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        ListF<ComponentClassInfo> classes;

        Component(String path, ListF<ClassInfo> classes) {
            this.path = path;
            this.classes = classes.map(cls -> cls.withComponent(this));
        }

        public String getShortUrl() {
            return LIB_NAME_PATTERN.findNthGroup(path, 2)
                    .getOrElse(path);
        }

        @Override
        public int compareTo(@Nonnull Component o) {
            return path.compareTo(o.path);
        }

        public int getClassCount() {
            return classes.size();
        }

        boolean matches(String pattern) {
            return path.matches(pattern) || path.contains(pattern);
        }

        private boolean belongsTo(ComponentPair pair) {
            return equals(pair.component1) || equals(pair.component2);
        }
    }

    @Value
    public static class ComponentPair {
        Component component1;

        Component component2;

        ComponentPair(Component component1, Component component2) {
            if (component1.compareTo(component2) <= 0) {
                this.component1 = component1;
                this.component2 = component2;
            } else {
                this.component1 = component2;
                this.component2 = component1;
            }
        }

        private static ListF<ComponentPair> getPermutations(ListF<Component> components) {
            ListF<ComponentPair> pairs = Cf.arrayList();
            for (int i = 0; i < components.size() - 1; i++) {
                for (int j = i + 1; j < components.size(); j++) {
                    pairs.add(new ComponentPair(components.get(i), components.get(j)));
                }
            }
            return pairs.unmodifiable();
        }
    }

    @Value
    public static class DuplicatedComponentPair {
        public ComponentPair pair;

        @ToString.Exclude
        ListF<ClassAggregate> duplicateClasses;

        static Option<DuplicatedComponentPair> consO(ComponentPair pair, ListF<ClassAggregate> duplicateClasses) {
            return Option.when(duplicateClasses.isNotEmpty(), () -> new DuplicatedComponentPair(pair, duplicateClasses));
        }

        Option<DuplicatedComponentPair> excludeClassesO(CollectionF<String> excludes) {
            return consO(pair, duplicateClasses.filterNot(aggregates -> aggregates.matches(excludes)));
        }

        Option<DuplicatedComponentPair> excludeClassesWithSameSizeO() {
            return consO(pair, duplicateClasses.filterNot(aggregate -> aggregate.hasSameSize(pair)));
        }

        boolean matches(CollectionF<ComponentMatcher> matchers) {
            return matchers.exists(matcher -> matcher.matches(pair));
        }

        String getDescription() {
            return getDescription(pair.component1) + " - " + getDescription(pair.component2);
        }

        private String getDescription(Component component) {
            return String.format("%s [%.1f%% - %d of %d classes]",
                    component.getShortUrl(),
                    (double) getDuplicateCount() / component.getClassCount() * 100,
                    getDuplicateCount(),
                    component.getClassCount()
            );
        }

        private int getDuplicateCount() {
            return duplicateClasses.size();
        }
    }
}
