package ru.yandex.market.billing.agency_reward.program;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.agency_reward.AgencyScale;
import ru.yandex.market.billing.agency_reward.program.cut_price.CutPriceRewardControlService;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.mbi.yt.YtUtilTest.intNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.longNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.stringNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.treeMapNode;

/**
 * Тесты для {@link ProgramAgencyInfoDao}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class ProgramAgencyInfoDaoTest {
    private static final LocalDate LD_2019_01_10 = LocalDate.of(2019, 1, 10);
    private static final String YT_PATH = "some/yt/path";
    private static final long AGENCY_CLID = 1000L;
    private static final long CONTRACT_ID = 20L;
    private static final String CONTRACT_EID = "20/80";

    private ProgramAgencyInfoDao programAgencyInfoDao;

    @Mock
    private Yt ytMock;

    @Mock
    private CutPriceRewardControlService cutPriceRewardControlService;

    static Stream<Arguments> filterArgs() {
        return Stream.of(
                Arguments.of(
                        "Отфильтровывыаем кривую шкалу",
                        treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("agency_id", longNode(AGENCY_CLID))
                                .put("contract_id", longNode(CONTRACT_ID))
                                .put("contract_eid", stringNode(CONTRACT_EID))
                                .put("scale_code", intNode(1))
                                .put("contract_end_dt", stringNode("2019-01-10"))
                                .put("program_sign_status", intNode(1))
                                .build()
                        )
                ),
                Arguments.of(
                        "Отфильтровывыаем, если поле со шкалой отсутствует/пустое",
                        treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("agency_id", longNode(AGENCY_CLID))
                                .put("contract_id", longNode(CONTRACT_ID))
                                .put("contract_eid", stringNode(CONTRACT_EID))
                                .put("contract_end_dt", stringNode("2019-01-10"))
                                .put("program_sign_status", intNode(1))
                                .build()
                        )
                ),
                Arguments.of(
                        "Отфильтровываем проблемные записи о контрактах, на основе явно заданного списка",
                        treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("agency_id", longNode(AGENCY_CLID))
                                .put("contract_id", intNode(123))
                                .put("program_sign_status", intNode(1))
                                .build()
                        )
                )
        );
    }

    static Stream<Arguments> programSignStatus() {
        return Stream.of(
                Arguments.of(
                        "Статус подписания ОК",
                        Map.of("program_sign_status", intNode(1)),
                        true
                ),
                Arguments.of(
                        "Статус подписания не ОК",
                        Map.of("program_sign_status", intNode(0)),
                        false
                ),
                Arguments.of(
                        "Статус подписания отсутствует",
                        Map.of(),
                        false
                )
        );
    }

    static Stream<Arguments> filterBadArgs() {
        return Stream.of(
                Arguments.of(
                        "Отсутствует contract_dt",
                        treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("agency_id", longNode(AGENCY_CLID))
                                .put("contract_id", longNode(CONTRACT_ID))
                                .put("contract_eid", stringNode(CONTRACT_EID))
                                .put("scale_code", intNode(12))
                                .put("contract_end_dt", stringNode("2019-01-10"))
                                .put("program_sign_status", intNode(1))
                                .build()
                        ),
                        "Failed to get contract_dt"
                ),
                Arguments.of(
                        "Отсутствует contract_end_dt",
                        treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("agency_id", longNode(AGENCY_CLID))
                                .put("contract_id", longNode(CONTRACT_ID))
                                .put("contract_eid", stringNode(CONTRACT_EID))
                                .put("contract_dt", stringNode("2019-01-01"))
                                .put("scale_code", intNode(12))
                                .put("program_sign_status", intNode(1))
                                .build()
                        ),
                        "Failed to get contract_end_dt"
                ),
                Arguments.of(
                        "Отсутствует contract_eid",
                        treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("agency_id", longNode(AGENCY_CLID))
                                .put("contract_id", longNode(CONTRACT_ID))
                                .put("scale_code", intNode(12))
                                .put("contract_dt", stringNode("2019-01-01"))
                                .put("contract_end_dt", stringNode("2019-01-10"))
                                .put("program_sign_status", intNode(1))
                                .build()
                        ),
                        "Failed to get contract_eid"
                ),
                Arguments.of(
                        "Отсутствует contract_id",
                        treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                                .put("agency_id", longNode(AGENCY_CLID))
                                .put("contract_eid", stringNode(CONTRACT_EID))
                                .put("scale_code", intNode(12))
                                .put("contract_dt", stringNode("2019-01-01"))
                                .put("contract_end_dt", stringNode("2019-01-10"))
                                .put("program_sign_status", intNode(1))
                                .build()
                        ),
                        "Key not found: contract_id"
                )
        );
    }

    @BeforeEach
    void beforeEach() {
        Mockito.when(cutPriceRewardControlService.getIgnoredAgencyContractsOnImport())
                .thenReturn(ImmutableSet.of(123L));

        programAgencyInfoDao = new ProgramAgencyInfoDao(
                ytMock,
                YT_PATH,
                cutPriceRewardControlService
        );
    }

    @DisplayName("Проверка работы фильтров при построении данных об агентстве из YT")
    @MethodSource("filterArgs")
    @ParameterizedTest(name = "{0}")
    void test_loadAgencyInfoWithFilter(String description, YTreeMapNode node) {
        Optional<ProgramAgencyInfo> infoO = programAgencyInfoDao.loadAgencyInfoWithFilter(
                node,
                ImmutableSet.of(123L, 456L)
        );

        assertTrue(infoO.isEmpty());
    }

    @DisplayName("Общее успешное построения данных об агентстве из YT")
    @Test
    void test_loadAgencyInfoWithFilter_ok() {
        Optional<ProgramAgencyInfo> infoO = programAgencyInfoDao.loadAgencyInfoWithFilter(
                treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                        .put("agency_id", longNode(AGENCY_CLID))
                        .put("contract_id", longNode(CONTRACT_ID))
                        .put("contract_eid", stringNode(CONTRACT_EID))
                        .put("scale_code", intNode(12))
                        .put("contract_dt", stringNode("2019-01-01"))
                        .put("contract_end_dt", stringNode("2019-01-10"))
                        .build()
                ),
                ImmutableSet.of(1L, 2L, 3L)
        );

        assertTrue(infoO.isPresent());
        assertThat(
                infoO.get(),
                MbiMatchers.<ProgramAgencyInfo>newAllOfBuilder()
                        .add(ProgramAgencyInfo::getClientId, AGENCY_CLID, "agency_clid")
                        .add(ProgramAgencyInfo::getContractEid, CONTRACT_EID, "contract_eid")
                        .add(ProgramAgencyInfo::getContractId, CONTRACT_ID, "contract_id")
                        .add(ProgramAgencyInfo::getContractFinishDate, LD_2019_01_10, "contract_end_dt")
                        .add(ProgramAgencyInfo::getScale, AgencyScale.MSC_SPB, "scale")
                        .build()
        );
    }

    @MethodSource("programSignStatus")
    @ParameterizedTest(name = "{0}")
    void test_buildFilteredInfo_programSignStatus(
            String description,
            Map<String, YTreeIntegerNodeImpl> programSignStatusNode,
            boolean expectedStatus
    ) {
        Optional<ProgramAgencyInfo> infoO = programAgencyInfoDao.loadAgencyInfoWithFilter(
                treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                        .put("agency_id", longNode(AGENCY_CLID))
                        .put("contract_id", longNode(CONTRACT_ID))
                        .put("contract_eid", stringNode(CONTRACT_EID))
                        .put("scale_code", intNode(12))
                        .put("contract_end_dt", stringNode("2019-01-10"))
                        .put("contract_dt", stringNode("2019-01-01"))
                        .putAll(programSignStatusNode)
                        .build()
                ),
                ImmutableSet.of(1L, 2L, 3L)
        );

        assertTrue(infoO.isPresent());
        assertEquals(expectedStatus, infoO.get().isCurPriceProgramSignStatus());
    }

    @DisplayName("Ошибки при маппинге YT данных")
    @MethodSource("filterBadArgs")
    @ParameterizedTest(name = "{0}")
    void test_buildFilteredInfo_errors(String description, YTreeMapNode node, String expectedExMsg) {
        Exception ex = Assertions.assertThrows(
                Exception.class,
                () -> programAgencyInfoDao.loadAgencyInfoWithFilter(
                        node,
                        ImmutableSet.of(123L, 456L)
                )
        );

        assertThat(ex.getMessage(), Matchers.startsWith(expectedExMsg));
    }
}
