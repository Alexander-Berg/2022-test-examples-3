package ru.yandex.market.loyalty.test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

public class SourceScanner {
    private static final TypeSolver[] EMPTY_TYPE_SOLVERS = new TypeSolver[0];
    private static final String[] PATH_PROPERTY = System.getProperty("java.class.path")
            .split(System.getProperty("path.separator"));

    private SourceScanner() {
    }

    public static <T> T findClassesToProcess(
            Predicate<Path> fileFilter, Predicate<ClassOrInterfaceDeclaration> classFilter,
            Function<Stream<ClassOrInterfaceDeclaration>, T> streamProcessor
    ) throws IOException {
        //split the current classpath using the system path separator (';' on windows)
        Stream<String> paths = Arrays.stream(PATH_PROPERTY);
        List<TypeSolver> solvers = paths
                .filter(s -> s.endsWith(".jar"))
                .map(makeExceptionsUnchecked(JarTypeSolver::getJarTypeSolver))
                .collect(Collectors.toList());  // this doesn't compile because of an unhandled IOException,
        // used a method reference for clarity
        solvers.add(new ReflectionTypeSolver(false)); //for JRE libraries

        TypeSolver typeSolver = new CombinedTypeSolver(solvers.toArray(EMPTY_TYPE_SOLVERS));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);


        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(symbolSolver);

        JavaParser.setStaticConfiguration(configuration);

        try (Stream<Path> walkStream = Files.walk(Paths.get(System.getProperty("user.dir")))) {
            return streamProcessor.apply(walkStream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".java"))
                    .filter(fileFilter)
                    .map(makeExceptionsUnchecked(JavaParser::parse))
                    .flatMap(parse -> parse.findAll(ClassOrInterfaceDeclaration.class).stream())
                    .filter(classFilter)
            );
        }
    }

    public static Stream<Method> requestMethods(String rootPackage) {
        return findClassesByAnnotation(rootPackage, Controller.class)
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods()))
                .filter(SourceScanner::isRequestMethod);
    }

    public static Stream<? extends Class<?>> findAllClasses(String rootPackage) {
        TypeFilter filter = (metadataReader, metadataReaderFactory) -> true;
        return findClassesByTypeFilters(rootPackage, List.of(filter), null);
    }

    public static Stream<? extends Class<?>> findSpringBeans(String rootPackage) {
        return findClassesByAnnotation(rootPackage, Component.class, Service.class, Repository.class);
    }

    private static boolean isRequestMethod(Method m) {
        return m.getDeclaredAnnotationsByType(RequestMapping.class).length > 0 ||
                m.getDeclaredAnnotationsByType(PutMapping.class).length > 0 ||
                m.getDeclaredAnnotationsByType(PostMapping.class).length > 0 ||
                m.getDeclaredAnnotationsByType(GetMapping.class).length > 0 ||
                m.getDeclaredAnnotationsByType(PatchMapping.class).length > 0;
    }

    @SafeVarargs
    public static Stream<? extends Class<?>> findClassesByAnnotation(
            String rootPackage, Class<? extends Annotation>... annotations
    ) {
        var annotationFilters = Arrays.stream(annotations)
                .map(AnnotationTypeFilter::new)
                .collect(Collectors.toList());
        return findClassesByTypeFilters(rootPackage, annotationFilters, null);
    }

    public static Stream<? extends Class<?>> findClassesByTypeFilters(
            String rootPackage,
            List<? extends TypeFilter> includeFilters,
            @Nullable List<? extends TypeFilter> excludeFilters
    ) {
        var provider = new ClassPathScanningCandidateComponentProvider(false);
        for (var filter : includeFilters) {
            provider.addIncludeFilter(filter);
        }
        if (excludeFilters != null && !excludeFilters.isEmpty()) {
            for (var filter : excludeFilters) {
                provider.addIncludeFilter(filter);
            }
        }

        return provider.findCandidateComponents(rootPackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .map(makeExceptionsUnchecked(Class::forName))
                .distinct();
    }
}
