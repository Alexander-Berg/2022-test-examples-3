package ru.yandex.autotests.innerpochta.util;

import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.ISSUE_LINK_TYPE;
import static io.qameta.allure.util.ResultsUtils.TMS_LINK_TYPE;

/**
 * @author gladnik (Nikolai Gladkov)
 */
public class TestCaseLabelsUpdater implements TestLifecycleListener {

    private static final String SUITE_LABEL = "suite";
    private static final String TEST_CLASS_LABEL = "testClass";
    private static final String TEST_METHOD_LABEL = "testMethod";
    private static final String SEVERITY_LABEL = "severity";
    private static final String FEATURE_LABEL = "feature";
    private static final String STORY_LABEL = "story";

    @Override
    public void beforeTestWrite(TestResult result) {
        try {
            String className = result.getLabels().stream()
                .filter(label -> label.getName().equals(TEST_CLASS_LABEL))
                .limit(1).map(Label::getValue).findFirst().orElseThrow(IllegalStateException::new);
            String[] methodNameWithParameters = result.getLabels().stream()
                .filter(label -> label.getName().equals(TEST_METHOD_LABEL))
                .limit(1).map(Label::getValue).findFirst().orElseThrow(IllegalStateException::new)
                .split("\\[");
            String methodName = methodNameWithParameters[0];
            String methodParameters = methodNameWithParameters.length > 1 ? "[" + methodNameWithParameters[1] : "";
            Class<?> testClass = Class.forName(className);
            Method testMethod = Stream.of(testClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst().orElseThrow(IllegalStateException::new);

            result.setName(getTestMethodTitle(testMethod) + methodParameters);
            result.setDescription(getTestMethodDescription(testMethod));
            updateSuiteNameLabel(result, testClass);
            addLabels(result, testClass, testMethod);
            addLinks(result, testClass, testMethod);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to update testcase names for Allure", e);
        }
    }


    private static String getTestMethodTitle(Method testMethod) {
        String title;
        if (testMethod.isAnnotationPresent(Title.class)) {
            title = testMethod.getAnnotation(Title.class).value();
        } else {
            title = testMethod.getName();
        }
        return title;
    }

    private static String getTestMethodDescription(Method testMethod) {
        String description = null;
        if (testMethod.isAnnotationPresent(Description.class)) {
            description = testMethod.getAnnotation(Description.class).value();
        }
        return description;
    }

    private static void updateSuiteNameLabel(TestResult result, Class<?> testClass) {
        if (testClass.isAnnotationPresent(Title.class)) {
            result.getLabels().stream()
                .filter(label -> label.getName().equals(SUITE_LABEL))
                .forEach(label -> label.setValue(testClass.getAnnotation(Title.class).value()));
        }
    }

    private static void addLabels(TestResult result, Class<?> testClass, Method testMethod) {
        List<Label> labels = result.getLabels();
        if (!hasLabel(labels, SEVERITY_LABEL)) {
            if (testMethod.isAnnotationPresent(Severity.class)) {
                labels.addAll(createLabels(testMethod.getAnnotation(Severity.class)));
            }
            if (testClass.isAnnotationPresent(Severity.class)) {
                labels.addAll(createLabels(testClass.getAnnotation(Severity.class)));
            }
        }
        if (!hasLabel(labels, STORY_LABEL)) {
            if (testMethod.isAnnotationPresent(Stories.class)) {
                labels.addAll(createLabels(testMethod.getAnnotation(Stories.class)));
            }
            if (testClass.isAnnotationPresent(Stories.class)) {
                labels.addAll(createLabels(testClass.getAnnotation(Stories.class)));
            }
        }
        if (!hasLabel(labels, FEATURE_LABEL)) {
            if (testMethod.isAnnotationPresent(Features.class)) {
                labels.addAll(createLabels(testMethod.getAnnotation(Features.class)));
            }
            if (testClass.isAnnotationPresent(Features.class)) {
                labels.addAll(createLabels(testClass.getAnnotation(Features.class)));
            }
        }
        result.setLabels(labels);
    }

    private static void addLinks(TestResult result, Class<?> testClass, Method testMethod) {
        List<Link> links = result.getLinks();
        if (!hasLink(links, TMS_LINK_TYPE)) {
            if (testMethod.isAnnotationPresent(TestCaseId.class)) {
                links.addAll(createLinks(testMethod.getAnnotation(TestCaseId.class)));
            }
            if (testClass.isAnnotationPresent(TestCaseId.class)) {
                links.addAll(createLinks(testClass.getAnnotation(TestCaseId.class)));
            }
        }
        if (!hasLink(links, ISSUE_LINK_TYPE)) {
            if (testMethod.isAnnotationPresent(Issues.class)) {
                links.addAll(createLinks(testMethod.getAnnotation(Issues.class)));
            }
            if (testClass.isAnnotationPresent(Issues.class)) {
                links.addAll(createLinks(testClass.getAnnotation(Issues.class)));
            }
            if (testMethod.isAnnotationPresent(Issue.class)) {
                links.addAll(createLinks(testMethod.getAnnotation(Issue.class)));
            }
            if (testClass.isAnnotationPresent(Issue.class)) {
                links.addAll(createLinks(testClass.getAnnotation(Issue.class)));
            }
        }
        result.setLinks(links);
    }

    private static List<Label> createLabels(final Stories stories) {
        return Arrays.stream(stories.value())
            .map(value -> new Label().withName(STORY_LABEL).withValue(value))
            .collect(Collectors.toList());
    }

    private static List<Label> createLabels(final Features features) {
        return Arrays.stream(features.value())
            .map(value -> new Label().withName(FEATURE_LABEL).withValue(value))
            .collect(Collectors.toList());
    }

    private static List<Label> createLabels(final Severity severity) {
        return Collections.singletonList(new Label().withName(SEVERITY_LABEL).withValue(severity.value().value()));
    }

    private static List<Link> createLinks(final TestCaseId issue) {
        return Collections.singletonList(ResultsUtils.createTmsLink(issue.value()));
    }

    private static List<Link> createLinks(final Issues issues) {
        return Arrays.stream(issues.value())
            .map(TestCaseLabelsUpdater::createLink)
            .collect(Collectors.toList());
    }

    private static List<Link> createLinks(final Issue issue) {
        return Collections.singletonList(createLink(issue));
    }

    private static Link createLink(final Issue issue) {
        return ResultsUtils.createIssueLink(issue.value());
    }

    private static boolean hasLabel(List<Label> labels, String name) {
        return labels.stream().map(Label::getName).filter(name::equals).limit(1).count() > 0;
    }

    private static boolean hasLink(List<Link> links, String type) {
        return links.stream().map(Link::getType).filter(type::equals).limit(1).count() > 0;
    }

}
