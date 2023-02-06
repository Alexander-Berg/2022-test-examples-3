package ru.yandex.direct.grid.core.entity.banner.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerCalloutsReplaceInstruction;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerCalloutsReplaceInstructionAction;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.core.entity.banner.service.GridFindAndReplaceCalloutsService.replaceCalloutIds;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(Parameterized.class)
public class GridFindAndReplaceCalloutsServiceTest {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public GdiFindAndReplaceBannerCalloutsReplaceInstructionAction action;

    @Parameterized.Parameter(2)
    public List<Integer> existedCalloutIds;

    @Parameterized.Parameter(3)
    public List<Integer> searchCalloutIds;

    @Parameterized.Parameter(4)
    public List<Integer> replaceCalloutIds;

    @Parameterized.Parameter(5)
    public List<Integer> expectedCalloutIds;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // добавление
                {
                        "ничего не добавляем в пустой список -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.ADD,
                        of(), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "ничего не добавляем в непустой список -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.ADD,
                        of(1), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "добавляем в пустой список -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.ADD,
                        of(), // existed
                        of(), // search
                        of(2), // replace
                        of(2), // expected
                },
                {
                        "добавляем в непустой список -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.ADD,
                        of(1), // existed
                        of(), // search
                        of(2), // replace
                        of(1, 2), // expected
                },
                {
                        "добавляем дубликат -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.ADD,
                        of(1), // existed
                        of(), // search
                        of(1), // replace
                        null, // expected
                },
                {
                        "добавляем дубликат и не дубликат -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.ADD,
                        of(1), // existed
                        of(), // search
                        of(2, 1), // replace
                        of(1, 2), // expected
                },
                {
                        "добавляем больше разнородных значений -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.ADD,
                        of(1, 3, 5, 7), // existed
                        of(), // search
                        of(1, 2, 3, 4, 5, 6, 7), // replace
                        of(1, 3, 5, 7, 2, 4, 6), // expected
                },

                // удаление всех
                {
                        "удаляем все из пустого списка -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_ALL,
                        of(), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "удаляем все из непустого списка -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_ALL,
                        of(1, 2, 3), // existed
                        of(), // search
                        of(), // replace
                        of(), // expected
                },

                // удаление по id
                {
                        "удаляем из пустого списка 'ничего' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_BY_IDS,
                        of(), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "удаляем из пустого списка 'значение' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_BY_IDS,
                        of(), // existed
                        of(1), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "удаляем из непустого списка 'ничего' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_BY_IDS,
                        of(1), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "удаляем из непустого списка 'другое значение' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_BY_IDS,
                        of(1), // existed
                        of(2), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "удаляем из непустого списка 'существующее значение' -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_BY_IDS,
                        of(1), // existed
                        of(1), // search
                        of(), // replace
                        of(), // expected
                },
                {
                        "удаляем из непустого списка 'все значения' -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(1, 2, 3), // search
                        of(), // replace
                        of(), // expected
                },
                {
                        "удаляем из непустого списка 'не все значения' -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(1, 3), // search
                        of(), // replace
                        of(2), // expected
                },
                {
                        "удаляем из непустого списка 'все значения + другие' -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REMOVE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(4, 2, 1, 3, 5), // search
                        of(), // replace
                        of(), // expected
                },

                // замена целиком
                {
                        "проставляем в пустом списке пустой список -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_ALL,
                        of(), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "проставляем в пустом списке новый список -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_ALL,
                        of(), // existed
                        of(), // search
                        of(1, 2, 3), // replace
                        of(1, 2, 3), // expected
                },
                {
                        "проставляем в непустом списке новый список -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_ALL,
                        of(4, 5, 6), // existed
                        of(), // search
                        of(1, 2, 3), // replace
                        of(1, 2, 3), // expected
                },
                {
                        "проставляем в непустом списке пустой -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_ALL,
                        of(4, 5, 6), // existed
                        of(), // search
                        of(), // replace
                        of(), // expected
                },
                {
                        "проставляем в непустом списке новый такой же список -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_ALL,
                        of(4, 5, 6), // existed
                        of(), // search
                        of(4, 5, 6), // replace
                        null, // expected
                },

                // замена по id
                {
                        "заменяем в пустом списке 'empty' на 'empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "заменяем в пустом списке 'empty' на 'not empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(), // existed
                        of(), // search
                        of(1), // replace
                        null, // expected
                },
                {
                        "заменяем в пустом списке 'not empty' на 'empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(), // existed
                        of(1), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "заменяем в пустом списке 'not empty' на 'not empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(), // existed
                        of(1), // search
                        of(2), // replace
                        null, // expected
                },

                {
                        "заменяем в непустом списке 'empty' на 'empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "заменяем в непустом списке 'empty' на 'not empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1), // existed
                        of(), // search
                        of(1), // replace
                        null, // expected
                },
                {
                        "заменяем в непустом списке 'существующее значение' на 'empty' -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1), // existed
                        of(1), // search
                        of(), // replace
                        of(), // expected
                },
                {
                        "заменяем в непустом списке 'существующее значение' на 'другое значение' -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1), // existed
                        of(1), // search
                        of(2), // replace
                        of(2), // expected
                },

                {
                        "заменяем в непустом списке 'empty' на 'empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(123), // existed
                        of(), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "заменяем в непустом списке 'empty' на 'not empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(123), // existed
                        of(), // search
                        of(1), // replace
                        null, // expected
                },
                {
                        "заменяем в непустом списке 'несуществующее значение' на 'empty' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(123), // existed
                        of(1), // search
                        of(), // replace
                        null, // expected
                },
                {
                        "заменяем в непустом списке 'несуществующее значение' на 'другое значение' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(123), // existed
                        of(1), // search
                        of(2), // replace
                        null, // expected
                },

                // еще кейсы
                {
                        "заменяем в непустом списке 'несуществующее значение' на 'другое значение' -> изменений нет",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(4), // search
                        of(5), // replace
                        null, // expected
                },
                {
                        "заменяем в непустом списке, 'существующие значения' на 'другое значение' -> изменения " +
                                "есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(2, 3), // search
                        of(5), // replace
                        of(1, 5), // expected
                },
                {
                        "заменяем в непустом списке, 'существующие и несуществующие значения' на 'другое значение' ->" +
                                " изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(3, 4, 5), // search
                        of(5), // replace
                        of(1, 2, 5), // expected
                },
                {
                        "заменяем целиком -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(1, 2, 3), // search
                        of(6, 5, 4), // replace
                        of(6, 5, 4), // expected
                },
                {
                        "заменяем один элемент на один -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(2), // search
                        of(4), // replace
                        of(1, 3, 4), // expected
                },
                {
                        "заменяем один элемент на несколько -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(2), // search
                        of(4, 5), // replace
                        of(1, 3, 4, 5), // expected
                },
                {
                        "заменяем несколько элементов на один -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(1, 3), // search
                        of(4), // replace
                        of(2, 4), // expected
                },
                {
                        "заменяем несколько элементов на несколько -> изменения есть",
                        GdiFindAndReplaceBannerCalloutsReplaceInstructionAction.REPLACE_BY_IDS,
                        of(1, 2, 3), // existed
                        of(1, 3), // search
                        of(4, 5), // replace
                        of(2, 4, 5), // expected
                },
        });
    }

    @Test
    public void test() {
        GdiFindAndReplaceBannerCalloutsReplaceInstruction replaceInstruction =
                new GdiFindAndReplaceBannerCalloutsReplaceInstruction()
                        .withAction(action)
                        .withSearchCalloutIds(mapList(searchCalloutIds, Integer::longValue))
                        .withReplaceCalloutIds(mapList(replaceCalloutIds, Integer::longValue));

        List<Long> actualCalloutIds = replaceCalloutIds(
                mapList(existedCalloutIds, Integer::longValue),
                replaceInstruction
        );

        assertThat(actualCalloutIds)
                .isEqualTo(mapList(expectedCalloutIds, Integer::longValue));
    }
}
