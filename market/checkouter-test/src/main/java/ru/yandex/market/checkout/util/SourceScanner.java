package ru.yandex.market.checkout.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ru.yandex.common.util.XPathUtils;
import ru.yandex.common.util.XmlUtils;
import ru.yandex.market.checkout.util.ToStringChecker.FunctionWithException;

public final class SourceScanner {

    private static final Logger LOG = LoggerFactory.getLogger(SourceScanner.class);
    private static final TypeSolver[] EMPTY_TYPE_SOLVERS = new TypeSolver[0];
    private static final String[] PATH_PROPERTY = System.getProperty("java.class.path")
            .split(System.getProperty("path.separator"));

    private SourceScanner() {
    }

    public static <T> T findClassesToProcess(
            @Nonnull Predicate<Path> fileFilter,
            @Nonnull Predicate<ClassOrInterfaceDeclaration> classFilter,
            @Nonnull Function<Stream<ClassOrInterfaceDeclaration>, T> streamProcessor
    ) throws IOException {
        Stream<String> paths = Arrays.stream(PATH_PROPERTY); //split the current classpath using the system path
        List<TypeSolver> solvers = paths
                .filter(s -> s.endsWith(".jar"))
                .map(makeExceptionsUnchecked(JarTypeSolver::getJarTypeSolver))
                .collect(Collectors.toList()); //this doesn't compile because of an unhandled IOException, used a
        solvers.add(new ReflectionTypeSolver(false)); //for JRE libraries

        TypeSolver typeSolver = new CombinedTypeSolver(solvers.toArray(EMPTY_TYPE_SOLVERS));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);


        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(symbolSolver);

        JavaParser.setStaticConfiguration(configuration);

        try (Stream<Path> arcadiaStream = Files.walk(ensureSourcePath(), FileVisitOption.FOLLOW_LINKS)) {
            return streamProcessor.apply(arcadiaStream
                    .filter(Files::isReadable)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".java"))
                    .filter(fileFilter)
                    .map(makeExceptionsUnchecked(JavaParser::parse))
                    .flatMap(parse -> parse.findAll(ClassOrInterfaceDeclaration.class).stream())
                    .filter(classFilter)
            );
        }
    }

    @Nonnull
    private static Path ensureSourcePath() {
        if (TestUtils.isYaTest()) {
            final String arcadiaSourcePath = ru.yandex.devtools.test.Paths.getSourcePath(
                    System.getProperty("arcadia.module.source.path"));
            LOG.debug("Processing path: {}", arcadiaSourcePath);
            return Paths.get(arcadiaSourcePath);
        } else {
            //idea context

            try (Stream<Path> walkStream = Files.walk(Paths.get(System.getProperty("user.dir")))) {
                final Path projectFile = walkStream
                        .filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".iml"))
                        .findFirst().orElse(null);
                final Document doc = XmlUtils.parseSource(Files.newInputStream(Objects.requireNonNull(projectFile)));
                final Path arcadiaPath = Path.of(XPathUtils.queryString("/module/component/content/@url", doc)
                        .replace("file:/", ""));

                LOG.debug("Processing path: {}", arcadiaPath);
                return Objects.requireNonNull(arcadiaPath);
            } catch (SAXException | IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public static Stream<Method> requestMethods(String rootPackage) {
        return findClassesByAnnotation(rootPackage, Controller.class)
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods()))
                .filter(SourceScanner::isRequestMethod);
    }

    public static Stream<? extends Class<?>> findAllClasses(String rootPackage) {
        return findClassesByAnnotation(rootPackage);
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
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        for (Class<? extends Annotation> annotation : annotations) {
            provider.addIncludeFilter(new AnnotationTypeFilter(annotation));
        }
        return provider.findCandidateComponents(rootPackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .map(makeExceptionsUnchecked(Class::forName))
                .map(t -> (Class<?>) t)
                .distinct();
    }

    public static <T, R, E extends Throwable> Function<T, R> makeExceptionsUnchecked(
            FunctionWithException<T, R, E> body) {
        return t -> {
            try {
                return body.apply(t);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
}
