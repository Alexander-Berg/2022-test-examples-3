package ru.yandex.market.mbi.partner_stat.yt;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;

/**
 * Поставщик тестовых данных для {@link YQLReader}.
 * Необходимо устанавливать возвращаемое значение через Mockito.
 * Сделано для того, чтобы данные сбрасывались сами перед каждым тестом.
 * В будущем хотелось бы создать новый тип DataSource в common-test, но пока только так.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class TestYQLReaderDataSupplier implements Supplier<Collection<Object>> {

    @Override
    public Collection<Object> get() {
        Assertions.fail("YQL data supplier must be mocked");
        return Collections.emptyList();
    }
}
