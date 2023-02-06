package ru.yandex.market.delivery.transport_manager.style;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.delivery.transport_manager.config.SpringApplicationConfig;
import ru.yandex.market.delivery.transport_manager.service.trn.TrnTemplaterService;

public class AnnotationValueIsNotUsedTest {

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    private static List<File> listWithFileNames = new ArrayList<>();

    private static final String FAIL_MSG = "Usage of @Value annotation is unacceptable";

    private static final List<Class> WHITELIST = List.of(SpringApplicationConfig.class, TrnTemplaterService.class);

    @Test
    @DisplayName("Проверяем, что аннотация @Value не используется")
    public void ValueAnnotationIsNotPresentedTest() throws IOException, ClassNotFoundException {
        Path root = FileSystems.getDefault().getPath("").toAbsolutePath();
        Path filePath = Paths.get(root.toString(), "src/main/java/ru/yandex/market/delivery");
        fillListFiles(filePath.toString());

        listWithFileNames.forEach(
            classPath -> {
                try {
                    String path = classPath.getPath();
                    Class clazz = Class.forName(
                        path.substring(path.indexOf("ru/yandex/market/delivery"))
                            .replace(".java", "")
                            .replaceAll("/", ".")
                    );

                    if (!WHITELIST.contains(clazz)) {

                        softly.assertThat(clazz.getConstructor().getAnnotation(Value.class)).isNull();
                        Arrays.stream(clazz.getDeclaredFields())
                            .forEach(field -> softly.assertThat(field.getAnnotation(Value.class)).isNull());
                        Arrays.stream(clazz.getMethods())
                            .forEach(method -> softly.assertThat(method.getAnnotation(Value.class)).isNull());
                        Arrays.stream(clazz.getMethods())
                            .flatMap(method -> Arrays.stream(method.getParameterAnnotations()))
                            .flatMap(Arrays::stream)
                            .forEach(annotation ->
                                softly.assertThat(annotation.toString()
                                        .contains("@org.springframework.beans.factory.annotation.Value"))
                                    .isFalse()
                                    .withFailMessage(FAIL_MSG));

                    }
                } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                }
            }
        );
    }

    private static void fillListFiles(String filePath) {
        File files = new File(filePath);
        Arrays.stream(Optional.ofNullable(files.listFiles()).orElse(new File[0])).forEach(file -> {
            if (file.isFile()) {
                listWithFileNames.add(file);
            } else if (file.isDirectory()) {
                fillListFiles(file.getAbsolutePath());
            }
        });
    }
}
