package ru.yandex.market.mbi.partner_stat.yt;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.RowMapper;

/**
 * Реализация {@link YQLReader} для тестов.
 * Проверяет существования файла с yql и возвращает замоканный результат.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
public class TestYQLReader implements YQLReader {

    private final TestYQLReaderDataSupplier yqlReaderDataSupplier;

    public TestYQLReader(final TestYQLReaderDataSupplier yqlReaderDataSupplier) {
        this.yqlReaderDataSupplier = yqlReaderDataSupplier;
    }

    @Override
    public <T> void read(final Resource yqlFile, final RowMapper<T> mapper, final Consumer<T> consumer) {
        // Проверяем, что файл действительно есть
        Assertions.assertTrue(yqlFile.exists(), "YQL file not found: " + yqlFile);

        yqlReaderDataSupplier.get().stream()
                .map(e -> (T) e)
                .forEach(consumer);
    }
}
