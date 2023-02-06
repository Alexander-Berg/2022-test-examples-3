package ru.yandex.market.checkout.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public final class ToStringChecker {

    private static final Logger LOG = LoggerFactory.getLogger(ToStringChecker.class);
    private static final Pattern REPLACE_DOLLAR = Pattern.compile("$", Pattern.LITERAL);

    private ToStringChecker() {
    }

    /**
     * Проверит полноту toString для всех классов из того же модуля, из которого вызыван тест (в качастве root
     * берется System.getProperty("user.dir"))
     * <p>
     * Полнота toString подразумевает, что в toString есть упомниания всех полей в виде литералов. Если есть вызов
     * super.toString, то проверяются только поля текущего класса, в противном случае - родителя тоже
     *
     * @param exclusions набор исключений, если хоть одна функция вернет true, то поле не будет рассматриваться
     */
    @SafeVarargs
    public static void checkToStringInSameModule(
            BiPredicate<ResolvedReferenceTypeDeclaration, ResolvedFieldDeclaration>... exclusions
    ) throws IOException {
        assertThat(
                missedFields(exclusions),
                is(empty())
        );
    }

    @SafeVarargs
    static Set<String> missedFields(
            BiPredicate<ResolvedReferenceTypeDeclaration, ResolvedFieldDeclaration>... exclusions
    ) throws IOException {
        return SourceScanner.findClassesToProcess(file -> true, classDeclaration -> true,
                stream -> stream.flatMap(classDeclaration -> {
                    ResolvedReferenceTypeDeclaration resolved = classDeclaration.resolve();
                    return classDeclaration
                            .findAll(MethodDeclaration.class)
                            .stream()
                            .filter(m -> m.getName().asString().equals("toString"))
                            .filter(m -> m.resolve().declaringType().equals(resolved))
                            .findFirst()
                            .map(toString -> {
                                boolean hasSuperCall = toString.findAll(MethodCallExpr.class)
                                        .stream()
                                        .filter(m -> m.getName().asString().equals("toString"))
                                        .anyMatch(m -> m.getScope().map(Expression::isSuperExpr).orElse(false));

                                List<String> stringLiterals = extractStringLiterals(toString);

                                List<ResolvedFieldDeclaration> fieldsToCheck;
                                if (hasSuperCall) {
                                    fieldsToCheck = resolved.getDeclaredFields();
                                } else {
                                    fieldsToCheck = resolved.getAllFields();
                                }

                                return fieldsToCheck
                                        .stream()
                                        .filter(field -> !field.isStatic())
                                        .filter(field -> Stream.of(exclusions).noneMatch(filter ->
                                                filter.test(resolved, field)))
                                        .filter(field -> stringLiterals.stream().noneMatch(literal ->
                                                //whole words only with java variable specific grammar
                                                literal.matches("(.*[^a-zA-Z_$]|^)+" + field.getName() +
                                                        "([^a-zA-Z_$0-9]+.*|$)")
                                        ))
                                        .map(field -> resolved.getQualifiedName() + '#' + field.getName());
                            })
                            .orElseGet(Stream::empty);
                })
                        .collect(Collectors.toSet())
        );
    }

    private static List<String> extractStringLiterals(MethodDeclaration toString) {
        return toString
                .findAll(StringLiteralExpr.class)
                .stream()
                .map(LiteralStringValueExpr::getValue)
                .collect(Collectors.toList());
    }

    public static BiPredicate<ResolvedReferenceTypeDeclaration, ResolvedFieldDeclaration> excludeFunctionInterfaces() {
        return (owner, field) -> {
            if (field.getType().isReferenceType()) {
                ResolvedReferenceTypeDeclaration typeDeclaration =
                        field.getType().asReferenceType().getTypeDeclaration();
                if (typeDeclaration.isInterface()) {
                    return typeDeclaration.isFunctionalInterface();
                }
            }
            return false;
        };
    }

    public static BiPredicate<ResolvedReferenceTypeDeclaration, ResolvedFieldDeclaration> excludeSpringBeansByName() {
        return (owner, field) -> field.getName().endsWith("Service")
                || field.getName().endsWith("Dao")
                || field.getName().equals("clock");
    }

    public static BiPredicate<ResolvedReferenceTypeDeclaration, ResolvedFieldDeclaration> excludeByClasses(
            Class<?>... classes) {
        Set<String> toExclude = Arrays.stream(classes)
                .map(ToStringChecker::toJavaParserQualifier)
                .collect(Collectors.toSet());
        return (owner, field) -> toExclude.contains(owner.getQualifiedName());
    }

    public static BiPredicate<ResolvedReferenceTypeDeclaration, ResolvedFieldDeclaration> excludeByPackages(
            String... packages) {
        Set<String> toExclude = Arrays.stream(packages)
                .collect(Collectors.toSet());
        return (owner, field) -> toExclude.stream().anyMatch(p -> owner.getPackageName().startsWith(p));
    }

    @SafeVarargs
    public static BiPredicate<ResolvedReferenceTypeDeclaration, ResolvedFieldDeclaration> excludeByField(
            Pair<Class<?>, String>... exclusions) {
        Map<String, Set<String>> exclusionsAsMap = Arrays.stream(exclusions)
                .collect(Collectors.groupingBy(
                        pair -> toJavaParserQualifier(pair.getLeft()),
                        Collectors.mapping(Map.Entry::getValue, Collectors.toSet())
                ));

        return (owner, field) -> exclusionsAsMap.getOrDefault(owner.getQualifiedName(), Collections.emptySet())
                .contains(field.getName());
    }

    private static String toJavaParserQualifier(Class<?> aClass) {
        return REPLACE_DOLLAR.matcher(aClass.getName()).replaceAll(".");
    }

    public interface FunctionWithException<T, R, E extends Throwable> {

        R apply(T t) throws E;
    }
}
