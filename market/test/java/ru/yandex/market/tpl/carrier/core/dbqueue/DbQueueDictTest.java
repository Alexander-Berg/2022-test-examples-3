package ru.yandex.market.tpl.carrier.core.dbqueue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.stream.StreamResult;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.DriverQueueType;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTestV2
public class DbQueueDictTest {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Все элементы в enum-е должны совпадать с элементами в справочнике в базе данных")
    void testSyncEnum2Db() {
        List<String> allQueuesFromDb = jdbcTemplate.query(
                "select queue_name from queue",
                (rs, rowNum) -> rs.getString("queue_name")
        );

        List<String> allQueuesFromEnum = Stream.of(QueueType.values(), DriverQueueType.values())
                .flatMap(Arrays::stream)
                .map(Enum::name)
                .collect(Collectors.toList());

        assertThat(allQueuesFromEnum)
                .containsExactlyInAnyOrderElementsOf(allQueuesFromDb);
    }

}
