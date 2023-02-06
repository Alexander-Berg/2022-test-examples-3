package ru.yandex.market.adv.content.manager.mapper.comment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.content.manager.database.entity.moderation.ModerationTaskEntity;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationContentModel;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationContentResultModel;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationTaskDataModel;
import ru.yandex.mj.generated.server.model.ErrorInfo;
import ru.yandex.mj.generated.server.model.ModerationData;

/**
 * Date: 08.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class CommentMapperImplTest extends AbstractContentManagerTest {

    @Autowired
    private CommentMapper commentMapper;

    @DisplayName("Преобразование пустого справочника в null комментарий.")
    @Test
    void map_emptyMap_nullComment() {
        Assertions.assertThat(commentMapper.map(Map.of()))
                .isNull();
    }

    @DisplayName("В справочнике нет комментария.")
    @Test
    void map_singletonMap_withoutRowDelimiterComment() {
        Assertions.assertThat(commentMapper.map(Map.of("1", "Ошибка")))
                .isNull();
    }

    @DisplayName("Получение комментария из справочника.")
    @Test
    void map_twoElementMap_fullComment() {
        Map<String, String> map = new TreeMap<>();
        map.put("comment", "Ошибка");
        map.put("2", "****");

        Assertions.assertThat(commentMapper.map(map))
                .isEqualTo("Ошибка");
    }

    @DisplayName("Преобразование задачи на модерацию без контента в пустой список.")
    @Test
    void map_moderationTaskWithEmptyContent_emptyMap() {
        Assertions.assertThat(commentMapper.map(createModerationTask(Map.of())))
                .containsExactlyInAnyOrderEntriesOf(Map.of());
    }

    @DisplayName("Преобразование задачи на модерацию в справочник с двумя элементами.")
    @Test
    void map_moderationTask_twoElementMap() {
        Map<String, Map<String, String>> map = new HashMap<>();
        map.put("Hi", null);
        map.put("John", Map.of("emptyName", "true", "comment", "Ошибка"));
        map.put("https://yandex.ru", Map.of("invalid", "false"));
        map.put("https://yandex.ru/1", Map.of());

        Assertions.assertThat(commentMapper.map(createModerationTask(map)))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "https://yandex.ru", createModerationData("https://yandex.ru", List.of(), "invalid"),
                                "John", createModerationData("John", List.of("Ошибка"), "emptyName")
                        )
                );
    }

    @Nonnull
    private ModerationData createModerationData(@Nonnull String content, @Nonnull List<String> comments,
                                                @Nullable String errorKey) {
        List<ErrorInfo> errorInfos;
        if (errorKey != null) {
            ErrorInfo errorInfo = new ErrorInfo();
            errorInfo.setKey(errorKey);

            errorInfos = List.of(errorInfo);
        } else {
            errorInfos = List.of();
        }

        ModerationData moderationData = new ModerationData();
        moderationData.setContent(content);
        moderationData.setComments(comments);
        moderationData.setInfos(errorInfos);
        return moderationData;
    }

    @Nonnull
    private ModerationTaskEntity createModerationTask(@Nonnull Map<String, Map<String, String>> map) {
        Map<String, ModerationContentModel> contentMap = new HashMap<>();

        map.forEach((content, params) -> {
            ModerationContentModel moderationContentModel = new ModerationContentModel();
            moderationContentModel.setContent(content);

            if (params != null) {
                ModerationContentResultModel moderationContentResultModel = new ModerationContentResultModel();
                if (params.isEmpty()) {
                    moderationContentResultModel.setResult(true);
                } else {
                    moderationContentResultModel.setResultInfos(params);
                }
                moderationContentModel.setResultModel(moderationContentResultModel);
            }

            contentMap.put(content, moderationContentModel);
        });

        ModerationTaskDataModel moderationTaskDataModel = new ModerationTaskDataModel();
        moderationTaskDataModel.setModerationContentModelMap(contentMap);

        ModerationTaskEntity moderationTask = new ModerationTaskEntity();
        moderationTask.setData(moderationTaskDataModel);

        return moderationTask;
    }
}
