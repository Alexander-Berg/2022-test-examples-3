package ru.yandex.market.billing.pp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.billing.pp.storage.MarketTypeMarker;
import ru.yandex.market.billing.pp.storage.PpDescription;
import ru.yandex.market.billing.pp.storage.PpDescriptionsAll;
import ru.yandex.market.billing.pp.storage.PpGroupsAll;
import ru.yandex.market.billing.pp.storage.PpHierarchyTree;
import ru.yandex.market.billing.pp.storage.PpJsonUtils;
import ru.yandex.market.billing.pp.validation.PpValidationException;

/**
 * Этот класс не проверят логику какого-либоа сервиса!
 * Задача - проверить корректность структуры  файлов, описывающих ПП и группировку в словарях.
 * <p>
 * Проверка структуры json файлов происходит не явным тестом, а прозрачно при использовании
 * методов {@link PpJsonUtils} через javax.validation.
 *
 * @author vbudnev
 */
class DataConsistencyTest {

    private static final String PP_DIR_PATH = "etc/yandex/mbi-billing/pp/";
    private static final String MARKET_PP = PP_DIR_PATH + "market_pp.json";
    private static final String DICT_PLACEMENT = PP_DIR_PATH + "dictionaries/placement.json";
    private static final String DICT_BLUE_GENERAL = PP_DIR_PATH + "dictionaries/blue_pp_general.json";
    private static final String DICT_PLATFORMS = PP_DIR_PATH + "dictionaries/platforms.json";
    private static final String DICT_UNIVERSAL = PP_DIR_PATH + "dictionaries/universal_reports.json";
    private static final String DICT_MARKET_AND_PARTNERS = PP_DIR_PATH + "dictionaries/market_and_partners.json";
    private static final String TRANSLATION_MAP = PP_DIR_PATH + "path_translation_map.json";
    /**
     * Максимальное количество элементов в пути, чтобы корректно отрисовывался в таблице на вики.
     * 4 элемента - колонки структуры (см PpDocumentationTask#Wiki#TABLE_PATH_COLUMNS)
     * 1 - платформа
     * 1 - название пп
     */
    private static final int MAX_PATH_EDGES = 6;

    /**
     * Проверяем структуру и консистетность для словаря мест размещения.
     * <p>
     * Special case: для словаря мест размещения 4ая является аггрегатом неск других, не используется
     * но для обратной совместимости она отражена в структуре
     */
    @Test
    void test_placementDictValidation() throws IOException {
        validateStrictJson(DICT_PLACEMENT);
        allPpCoveredByGroups(DICT_PLACEMENT);
        eachPathContainsPpOnce(DICT_PLACEMENT, groupId -> groupId != 4);
    }

    /**
     * Проверяем структуру и консистетность для словаря универсальных отчетов.
     * В этом словаре, группы представляют собой вложеные множества, потому требования однокртаного ПП между группами
     * не выставляется.
     */
    @Test
    void test_universalDictValidation() throws IOException {
        validateStrictJson(DICT_UNIVERSAL);
        allPpCoveredByGroups(DICT_UNIVERSAL);
    }

    /**
     * Проверяем структуру и консистетность для словаря платформ.
     */
    @Test
    void test_platformDictValidation() throws IOException {
        validateStrictJson(DICT_PLATFORMS);
        allPpCoveredByGroups(DICT_PLATFORMS);
        eachPathContainsPpOnce(DICT_PLATFORMS);
    }

    /**
     * Проверяем структуру и консистетность для словаря общего набора пп для синего.
     */
    @Test
    void test_generalBlueDictValidation() throws IOException {
        validateStrictJson(DICT_BLUE_GENERAL);
        allPpCoveredByGroups(DICT_BLUE_GENERAL);
        eachPathContainsPpOnce(DICT_BLUE_GENERAL);
    }

    /**
     * Проверяем структуру и консистетность для словаря маркет и партнёры.
     */
    @Test
    void test_generalMarketAndPartnersDictValidation() throws IOException {
        validateStrictJson(DICT_MARKET_AND_PARTNERS);
        allPpCoveredByGroups(DICT_MARKET_AND_PARTNERS);
        eachPathContainsPpOnce(DICT_MARKET_AND_PARTNERS);
    }

    /**
     * Проверяем явно количество элементов в пути, чтобы было консистентно с шириной таблички на вики.
     */
    @Test
    void test_checkPathLengths() throws IOException {
        List<Integer> ppWithTooLongPaths = PpJsonUtils.loadPpDescriptions(resourceAsString(MARKET_PP))
                .getPpDescriptionById().values().stream()
                .filter(
                        ppDescription -> ppDescription.getPath().split("/").length > MAX_PATH_EDGES
                ).map(PpDescription::getPpId)
                .collect(Collectors.toList());
        if (ppWithTooLongPaths.size() != 0) {
            throw new PpValidationException("Pps with too long paths for wiki render: " + ppWithTooLongPaths);
        }
    }

    /**
     * Проверяем, что:
     * - словарь перевода содержит ключи, которые действительно есть в путях ПП, чтобы опечатка не повисла.
     * - значения - непустые строки, чтобы не приеахло нигде неявного пустого места.
     */
    @Test
    void test_translationMapStructure() throws IOException {
        validateStrictJson(TRANSLATION_MAP);

        Set<String> pathEdges = PpJsonUtils.loadPpDescriptions(resourceAsString(MARKET_PP))
                .getPpDescriptionById().values().stream()
                .flatMap(
                        x -> Arrays.stream(x.getPath().split("/"))
                )
                .collect(Collectors.toSet());

        PpJsonUtils.loadTranslationMap(resourceAsString(TRANSLATION_MAP))
                .forEach((edge, translation) -> {
                            if (StringUtils.isBlank(translation)) {
                                throw new PpValidationException("Blank translation value for key: " + edge);
                            }
                            if (!pathEdges.contains(edge)) {
                                throw new PpValidationException("Translation key has no usage in paths :" + edge);
                            }
                        }
                );

    }

    /**
     * Чуть закручиваем гайки в защите от опечаток.
     * Такое ограничение накладываем только в тесте на ресурсы, в логике обработки ПП таких ограничений нет -
     * это позволит в случае, чего ослабить/убрать быстро регулярку/заигнорить тест не требуя публикации
     * артифакта с {@link PpHierarchyTree}, который используется в биллинге.
     */
    @Test
    void test_checkPathRegexPattern() throws IOException {
        String expectedPathPattern = "^[a-zA-Z0-9-_/]+$";
        final Predicate<String> INVALID_PATH = Pattern.compile(expectedPathPattern).asPredicate().negate();

        PpJsonUtils.loadPpDescriptions(resourceAsString(MARKET_PP))
                .getPpDescriptionById()
                .values().stream()
                .map(PpDescription::getPath)
                .filter(INVALID_PATH)
                .findAny()
                .ifPresent(
                        (invalidPath) -> {
                            throw new PpValidationException(
                                    "Path \"" + invalidPath + "\" does not match \"" + expectedPathPattern + "\""
                            );
                        }
                );
    }

    /**
     * GSON не совсем строго проверяет формат, например пропускает
     * "paths":["a","b","c",]
     * который в итоге приезжает последним элементом null в массиве строк.
     * Накинуть валидатор на элементы внутри контейнера без оберток тоже нельзя.
     * Или например какая-либо опечатка выдается как failed to parse, хотя можно явно привести ошибку по номеру строки
     * и т.д.
     */
    private void validateStrictJson(String filePath) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.readTree(resourceAsString(filePath));
    }

    /**
     * Проверяем, что группы указанные в словаре покрывают все ПП из {@link #MARKET_PP}.
     * В расчет приниматся только ПП относящиеся к типу {@link MarketTypeMarker}, указанному в словаре.
     */
    private void allPpCoveredByGroups(String dictFilePath) throws IOException {
        PpGroupsAll placementGroups = PpJsonUtils.loadDictionaryDescriptions(resourceAsString(dictFilePath));
        PpDescriptionsAll ppDescriptionsAll = PpJsonUtils.loadPpDescriptions(resourceAsString(MARKET_PP))
                .copyWithRetainMarketType(placementGroups.getMarketTypeMarker());

        PpHierarchyTree ppTree = new PpHierarchyTree(ppDescriptionsAll);
        Set<Integer> ppFromGroups = placementGroups.getGroups().stream()
                .flatMap(group -> group.getPpPaths().stream())
                .flatMap(p -> ppTree.getPpByPath(p).stream())
                .collect(Collectors.toSet());

        Set<Integer> allDescribedPps = new HashSet<>(ppDescriptionsAll.getPpDescriptionById().keySet());
        allDescribedPps.removeAll(ppFromGroups);

        if (allDescribedPps.size() != 0) {
            throw new PpValidationException(dictFilePath + " : dictionary groups do not cover all pps." +
                    " Lost pps: " + allDescribedPps
            );
        }

    }

    /**
     * В указаном словаре места размещения для групп, не попавших в фильтр, должны встречаться только один раз.
     */
    private void eachPathContainsPpOnce(
            String dictFilePath,
            Function<Integer, Boolean> groupFilter
    ) throws IOException {
        PpDescriptionsAll ppDescriptionsAll = PpJsonUtils.loadPpDescriptions(resourceAsString(MARKET_PP));
        PpGroupsAll placementGroups = PpJsonUtils.loadDictionaryDescriptions(resourceAsString(dictFilePath));

        Map<Integer, String> alreadySeenPpsPaths = new HashMap<>();
        PpHierarchyTree ppTree = new PpHierarchyTree(ppDescriptionsAll);
        placementGroups.getGroups().stream()
                .filter(group -> groupFilter.apply(group.getGroupId()))//убираем исключительные
                .flatMap(group -> group.getPpPaths().stream())// все пути в рамках группы
                .forEach(path -> // ошибка, если нашли ПП которое уже попадалось в рамках другой группы
                        ppTree.getPpByPath(path).forEach(
                                pp -> {
                                    String alreadySeenPath = alreadySeenPpsPaths.get(pp);
                                    if (alreadySeenPath != null) {
                                        throw new PpValidationException("Pp " + pp +
                                                " met more then once. " +
                                                " Current: " + path +
                                                " Already seen at: " + alreadySeenPath);
                                    } else {
                                        alreadySeenPpsPaths.put(pp, path);
                                    }
                                }
                        )
                );
    }

    private void eachPathContainsPpOnce(String dictFilePath) throws IOException {
        eachPathContainsPpOnce(dictFilePath, groupId -> true);
    }

    private String resourceAsString(String resourcePath) throws IOException {
        String filename = getClass().getClassLoader().getResource(resourcePath).getFile();
        return FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8);
    }
}
