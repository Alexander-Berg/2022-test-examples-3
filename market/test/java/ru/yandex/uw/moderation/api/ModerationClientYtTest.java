package ru.yandex.uw.moderation.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.config.YtStaticClientAutoconfiguration;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.adv.yt.test.configuration.YtTestConfiguration;
import ru.yandex.market.adv.yt.test.extension.YtExtension;
import ru.yandex.uw.moderation.config.autoconfigure.ModerationClientYtConfiguration;
import ru.yandex.uw.moderation.model.ModerationRequest;
import ru.yandex.uw.moderation.model.ModerationRequestRecord;
import ru.yandex.uw.moderation.model.ModerationResult;

@Slf4j
@ExtendWith({
        SpringExtension.class,
        YtExtension.class
})
@SpringBootTest(
        classes = {
                CommonBeanAutoconfiguration.class,
                YtStaticClientAutoconfiguration.class,
                ModerationClientYtConfiguration.class,
                JacksonAutoConfiguration.class,
                YtTestConfiguration.class
        }
)
@TestPropertySource(locations = "/applications.properties")
public class ModerationClientYtTest {

    private static final String TABLE_ID = "523_3123_5132_BUSINESS";
    private static final String MODERATION_REQUEST_PATH = "//tmp/uw-moderation-request-" + TABLE_ID;
    private static final String MODERATION_RESULT_PATH = "//tmp/uw-moderation-request-" + TABLE_ID + "-moderated";

    @Autowired
    private ModerationClient moderationClient;

    @Test
    @DisplayName("Создание новой таблицы с данными для модерации")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_REQUEST_PATH,
                    isDynamic = false
            ),
            create = false,
            after = "json/yt/moderationRequestRecord/" +
                    "enqueue_listWithData_newTable.after.json"
    )
    public void enqueue_listWithData_newTable() {
        moderationClient.enqueue(TABLE_ID, moderationRequest());
    }

    @Test
    @DisplayName("Попытка создания существующей таблицы с данными для модерации")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_REQUEST_PATH,
                    isDynamic = false
            )
    )
    public void enqueue_existTable_Exception() {
        Assertions.assertThatThrownBy(() -> moderationClient.enqueue(TABLE_ID, moderationRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageMatching("The given table .+ already exists");
    }

    @Test
    @DisplayName("Чтение таблицы с результатами модерации.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_RESULT_PATH,
                    isDynamic = false
            ),
            before = "json/yt/moderationRequestRecord/" +
                    "getResult_tableWithData_list.before.json",
            after = "json/yt/moderationRequestRecord/" +
                    "getResult_tableWithData_list.after.json"
    )
    public void getResult_tableWithData_list() {
        Assertions.assertThat(moderationClient.getResult(TABLE_ID))
                .containsExactlyInAnyOrderElementsOf(moderationResult());
    }

    @Test
    @DisplayName("Чтение из пустой таблицы с результатами модерации завершилось исключением.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_RESULT_PATH,
                    isDynamic = false
            )
    )
    public void getResult_emptyTable_exception() {
        Assertions.assertThatThrownBy(() -> moderationClient.getResult(TABLE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageMatching("Table .+ cannot be empty");
    }

    @Test
    @DisplayName("Чтение из отсутствующей таблицы с результатами модерации завершилось исключением.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_RESULT_PATH,
                    isDynamic = false
            ),
            create = false,
            exist = false
    )
    public void getResult_notExistTable_exception() {
        Assertions.assertThatThrownBy(() -> moderationClient.getResult(TABLE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageMatching("Cannot find the table .+");
    }

    @Test
    @DisplayName("Таблица с результатами модерации присутствует.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_RESULT_PATH,
                    isDynamic = false
            )
    )
    public void hasResult_existTable_true() {
        Assertions.assertThat(moderationClient.hasResult(TABLE_ID))
                .isTrue();
    }

    @Test
    @DisplayName("Таблица с результатами модерации отсутствует.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_RESULT_PATH,
                    isDynamic = false
            ),
            create = false,
            exist = false
    )
    public void hasResult_notExistTable_false() {
        Assertions.assertThat(moderationClient.hasResult(TABLE_ID))
                .isFalse();
    }

    @Test
    @DisplayName("Невозможно удалить таблицу, так как она отсутствует.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_RESULT_PATH,
                    isDynamic = false
            ),
            create = false,
            exist = false
    )
    public void removeResult_notExistTable_exception() {
        Assertions.assertThatThrownBy(() -> moderationClient.removeResult(TABLE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageMatching("Cannot remove not existed table .+");
    }

    @Test
    @DisplayName("Успешное удаление таблицы с результатами.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_RESULT_PATH,
                    isDynamic = false
            ),
            exist = false,
            before = "json/yt/moderationRequestRecord/" +
                    "removeResult_existTable_tableRemoved.before.json"
    )
    public void removeResult_existTable_tableRemoved() {
        moderationClient.removeResult(TABLE_ID);
    }

    @Test
    @DisplayName("Удаляем таблицу с данными для единого окна.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_REQUEST_PATH,
                    isDynamic = false
            ),
            exist = false,
            before = "json/yt/moderationRequestRecord/" +
                    "removeRequest_hasTable_remove.before.json"
    )
    public void removeRequest_hasTable_remove() {
        moderationClient.removeRequest(TABLE_ID);
    }

    @Test
    @DisplayName("Удаление таблицы с данными для единого окна ничего не сделало.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ModerationRequestRecord.class,
                    path = MODERATION_REQUEST_PATH,
                    isDynamic = false
            ),
            create = false,
            exist = false
    )
    public void removeRequest_nothing_nothing() {
        moderationClient.removeRequest(TABLE_ID);
    }

    @Nonnull
    private Collection<ModerationRequest> moderationRequest() {
        return List.of(
                ModerationRequest.builder()
                        .id("123")
                        .content("My Random data")
                        .rule(
                                new ModerationRule(
                                        "TEXT",
                                        Map.of("paramKey", "paramVal", "paramKey2", "paramVal2")
                                )
                        )
                        .build(),
                ModerationRequest.builder()
                        .id("135")
                        .content("https://s3.mds.yandex.net/image_1")
                        .rule(
                                new ModerationRule(
                                        "IMAGE",
                                        Map.of()
                                )
                        )
                        .build()
        );
    }

    @Nonnull
    private Collection<ModerationResult> moderationResult() {
        return List.of(
                ModerationResult.builder()
                        .id("123")
                        .result(false)
                        .resultInfos(Map.of("yabrand", "true", "Status", "ERROR"))
                        .build(),
                ModerationResult.builder()
                        .id("135")
                        .result(true)
                        .resultInfos(Map.of())
                        .build()
        );
    }
}
