package ru.yandex.travel.workflow;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.travel.hibernate.types.ProtobufEnumType;


@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "protobuf-enum-dictionary.enabled=true",
                "protobuf-enum-dictionary.table-name=PROTO_DICTIONARY_TABLE"
        }
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class ProtoEnumDictionaryServiceTest {
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProtobufEnumDictionaryService protobufEnumDictionaryService;

    @Test
    public void testProtoDictionaryTableCreated() {
        transactionTemplate.execute(ignored -> {
            List<Tuple2<String, Integer>> resultList = jdbcTemplate.query(String.format(
                    "SELECT proto_class_name, count(*) FROM %s GROUP BY proto_class_name",
                    protobufEnumDictionaryService.getTableName()
            ), new RowMapperResultSetExtractor<>((rs, rowNum) -> Tuple2.tuple(rs.getString(1), rs.getInt(2))));

            Map<String, Integer> classMapping = resultList.stream().collect(Collectors.toMap( v -> v._1, v-> v._2));

            ProtobufEnumType.getGlobalMapping().entrySet().forEach(entry -> {
                        String className = entry.getKey().getCanonicalName();
                        Integer count = entry.getValue().size();
                        Assertions.assertThat(count).isEqualTo(classMapping.get(className));
                    }
            );

            return null;
        });
    }
}
