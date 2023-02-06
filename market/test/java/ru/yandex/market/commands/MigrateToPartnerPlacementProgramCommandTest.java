package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.CLICK_AND_COLLECT;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.CPC;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.DROPSHIP_BY_SELLER;
import static ru.yandex.market.core.partner.placement.PartnerPlacementProgramType.FULFILLMENT;

@DbUnitDataSet(before = "MigrateToPartnerPlacementProgramCommandTest.before.csv")
class MigrateToPartnerPlacementProgramCommandTest extends FunctionalTest {

    @Autowired
    ProtocolService protocolService;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    PartnerPlacementProgramService partnerPlacementProgramService;

    private PartnerPlacementProgramService partnerPlacementProgramServiceSpy;
    private MigrateToPartnerPlacementProgramCommand command;

    @BeforeEach
    void init() {
        partnerPlacementProgramServiceSpy = spy(partnerPlacementProgramService);
        command = new MigrateToPartnerPlacementProgramCommand(
                protocolService,
                namedParameterJdbcTemplate,
                partnerPlacementProgramServiceSpy
        );
    }

    private static Stream<Arguments> dataDisabled() {
        return Stream.of(
                Arguments.of(8L, DROPSHIP_BY_SELLER),
                Arguments.of(11L, CPC)
        );
    }

    @DisplayName("Миграция не должна выполняться для отключенных")
    @ParameterizedTest
    @MethodSource("dataDisabled")
    void testNotExists(long partnerId, PartnerPlacementProgramType programType) {
        invoke();
        assertThat(partnerPlacementProgramService.getPartnerPlacementProgram(partnerId, programType)).isEmpty();
    }

    /**
     * Проверяет миграцию прграммы DROPSHIP_BY_SELLER в SUCCESS и удаление программы FULFILLMENT, которая в статусе
     * DISABLED.
     */
    @DisplayName("Миграция одного партнера DSBS")
    @Test
    void testOneDropshipBySeller() {
        invoke(5);
        var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(5L);
        assertThat(programs)
                .hasSize(1)
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(DROPSHIP_BY_SELLER, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.SUCCESS);
                    assertThat(program.isEverActivated()).isTrue();
                });
    }

    @DisplayName("Миграция уже подключенного партнера DSBS")
    @Test
    void testEverActivatedDropshipBySeller() {
        invoke(12);
        var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(12L);
        assertThat(programs)
                .hasSize(1)
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(DROPSHIP_BY_SELLER, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.TESTED);
                    assertThat(program.isEverActivated()).isTrue();
                });
    }

    @DisplayName("Миграция DSBS, который пользуется аукционом")
    @Test
    void testDropshipBySellerWithAuction() {
        invoke(15);
        var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(15L);
        assertThat(programs).hasSize(1)
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(DROPSHIP_BY_SELLER, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.SUCCESS);
                    assertThat(program.isEverActivated()).isTrue();
                });
    }

    @DisplayName("Миграция одного партнера FULFILLMENT+CROSSDOCK")
    @Test
    void testFulFillmentAndCrossdock() {
        invoke(7);
        var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(7L);
        assertThat(programs).hasSize(2)
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(FULFILLMENT, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.SUCCESS);
                    assertThat(program.isEverActivated()).isTrue();
                })
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(FULFILLMENT, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.SUCCESS);
                    assertThat(program.isEverActivated()).isTrue();
                });
    }

    @DisplayName("Миграция одного партнера CROSSDOCK configure")
    @Test
    void testDropshipConfigure() {
        invoke(9);
        var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(9L);
        assertThat(programs).hasSize(1)
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(DROPSHIP, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.CONFIGURE);
                    assertThat(program.isEverActivated()).isFalse();
                });
    }

    @DisplayName("Миграция одного партнера DSBS configure")
    @Test
    void testDropshipBySellerConfigure() {
        invoke(10);
        var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(10L);
        assertThat(programs)
                .hasSize(2)
                .as("Program CPC not found")
                .hasEntrySatisfying(CPC, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.SUCCESS);
                })
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(DROPSHIP_BY_SELLER, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.CONFIGURE);
                    assertThat(program.isEverActivated()).isFalse();
                });
    }

    @DisplayName("Миграция одного партнера CLICK_AND_COLLECT")
    @Test
    void testOneClickAndCollect() {
        invoke(4);
        var programs = partnerPlacementProgramService.getPartnerPlacementPrograms(4L);
        assertThat(programs)
                .hasSize(2)
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(CLICK_AND_COLLECT, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.SUCCESS);
                    assertThat(program.isEverActivated()).isTrue();
                })
                .as("Program dropship by seller not found")
                .hasEntrySatisfying(DROPSHIP, program -> {
                    assertThat(program.getStatus()).isEqualTo(PartnerPlacementProgramStatus.SUCCESS);
                    assertThat(program.isEverActivated()).isTrue();
                });
    }

    @DisplayName("Миграция без указания id не должна работать")
    @Test
    void test() {
        invoke();
        verify(partnerPlacementProgramServiceSpy, never()).activatePrograms(anyLong(), any(), anyLong());
    }

    @DisplayName("Миграция доставки не создает программ")
    @Test
    @DbUnitDataSet(after = "MigrateToPartnerPlacementProgramCommandTest.before.csv")
    void testDelivery() {
        invoke(13);
    }

    @DisplayName("Миграция отключенного за качество партнера DSBS")
    @Test
    @DbUnitDataSet(after = "MigrateToPartnerPlacementProgramCommandTest.dsbs.revoke.after.csv")
    void testSwitchedOffDsbs() {
        invoke(14);
    }

    private void invoke(long... partnerId) {
        var terminal = mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(mock(PrintWriter.class));
        when(terminal.areYouSure()).thenReturn(true);
        command.executeCommand(
                new CommandInvocation("migrate-to-partner-placement-program",
                        Arrays.stream(partnerId)
                                .mapToObj(String::valueOf)
                                .toArray(String[]::new),
                        Collections.emptyMap()),
                terminal
        );
    }

}
