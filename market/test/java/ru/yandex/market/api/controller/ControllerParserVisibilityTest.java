package ru.yandex.market.api.controller;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.util.parser.ParserWrapper;

import javax.inject.Inject;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ControllerParserVisibilityTest extends ContainerTestBase {

    @Inject
    private ApplicationContext context;

    @Test
    public void allParserWrapperInsideControllersMustBePublic() throws ClassNotFoundException {
        Map<String, Object> beansWithAnnotation = context.getBeansWithAnnotation(Controller.class);
        FailedFieldsCollection failedFieldsCollection = new FailedFieldsCollection();
        for (String beanName : beansWithAnnotation.keySet()) {

            Object springControllerInstance = context.getBean(beanName);
            String springControllerClassName = springControllerInstance.getClass().getCanonicalName();

            int innerClassSuffixPosition = springControllerClassName.indexOf("$");
            if (innerClassSuffixPosition < 0) {
                continue;
            }
            String controllerClassName = springControllerClassName.substring(0, innerClassSuffixPosition);
            Class<?> controllerClass = Class.forName(controllerClassName);
            for (Class<?> cls : controllerClass.getDeclaredClasses()) {
                if (cls.getSuperclass().getCanonicalName().equals(ParserWrapper.class.getCanonicalName())) {
                    if (!Modifier.isPrivate(cls.getModifiers())) {
                        failedFieldsCollection.addNonPrivate(controllerClassName, cls.getSimpleName());
                    }
                    if (!Modifier.isStatic(cls.getModifiers())) {
                        failedFieldsCollection.addNonStatic(controllerClassName, cls.getSimpleName());
                    }
                }
            }
        }
        failedFieldsCollection.assertEmpty();
    }

    private static class FailedFieldsCollection {

        private enum ProblemType {
            PRIVATE, INSTANCE
        }

        private static class Problem {
            private final ProblemType type;
            private final String member;

            public ProblemType getType() {
                return type;
            }

            public String getMember() {
                return member;
            }

            public Problem(ProblemType type, String member) {

                this.type = type;
                this.member = member;
            }
        }

        private static String END_OF_LINE = "\n";
        private Map<String, List<Problem>> problems = new HashMap<>();

        public void addNonPrivate(String controllerName, String fieldName) {
            addProblem(ProblemType.PRIVATE, controllerName, fieldName);
        }

        public void addNonStatic(String controllerName, String fieldName) {
            addProblem(ProblemType.INSTANCE, controllerName, fieldName);
        }

        private void addProblem(ProblemType type, String controllerName, String fieldName) {
            List<Problem> strings = problems.get(controllerName);
            if (null == strings) {
                ArrayList<Problem> newList = new ArrayList<>();
                problems.put(controllerName, newList);
                strings = newList;
            }
            strings.add(new Problem(type, fieldName));
        }

        public void assertEmpty() {
            Assert.assertTrue(String.format("Парсер в контроллере специфическая, нужная в локальном контексте вещь, она должна быть приватной. Хотите публичности уберите из контроллера, чтобы не переиспользовать части контроллеров в разных местах. %s", getPrettyPrintFailMessage()),
                problems.isEmpty());
        }

        public String getPrettyPrintFailMessage() {
            return problems.keySet()
                .stream()
                .flatMap(x -> problems.get(x)
                    .stream()
                    .map(v -> String.format("Контроллер %s имеет %s приватный парсер %s.", x, getProblemDescription(v.getType()), v.getMember())))
                .collect(Collectors.joining(END_OF_LINE));
        }

        private String getProblemDescription(ProblemType type) {
            switch (type) {
                case PRIVATE:
                    return "private";
                case INSTANCE:
                    return "non static";
                default:
                    throw new IllegalArgumentException(String.format("invalid type %s", type));
            }
        }
    }
}


