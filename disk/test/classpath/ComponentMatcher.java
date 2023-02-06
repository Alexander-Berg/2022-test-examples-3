package ru.yandex.chemodan.test.classpath;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public abstract class ComponentMatcher {
    abstract boolean matches(DuplicateClassFinder.ComponentPair pair);

    public static ComponentMatcher anyMatch(String pattern) {
        return new Any(pattern);
    }

    public static ComponentMatcher pairMatch(String pattern1, String pattern2) {
        return new Pair(pattern1, pattern2);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class Pair extends ComponentMatcher {
        String pattern1;

        String pattern2;

        @Override
        boolean matches(DuplicateClassFinder.ComponentPair pair) {
            return matches(pair.getComponent1(), pair.getComponent2())
                    || matches(pair.getComponent1(), pair.getComponent2());
        }

        private boolean matches(DuplicateClassFinder.Component component1, DuplicateClassFinder.Component component2) {
            return component1.matches(pattern1) && component2.matches(pattern2);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class Any extends ComponentMatcher {
        String pattern;

        @Override
        boolean matches(DuplicateClassFinder.ComponentPair pair) {
            return matches(pair.getComponent1(), pair.getComponent2());
        }

        private boolean matches(DuplicateClassFinder.Component component1, DuplicateClassFinder.Component component2) {
            return component1.matches(pattern) || component2.matches(pattern);
        }
    }
}
