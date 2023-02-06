package ru.yandex.market.partner.content.common.db.dao.goodcontent;

import org.junit.Test;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.enums.MessageLevel;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.entity.goodcontent.ValidationWithMessages;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class GcSkuTicketDaoTest extends DBDcpStateGenerator {

    @Test
    public void testReadValidationMessagesErrors() {
        List<Long> ticketIds = generateDBInitialStateAndGetIds(3);
        List<TicketValidationResult> results = ticketIds.stream()
                        .map(ticketId ->
                                    TicketValidationResult.invalid(ticketId, Messages.get().pictureInvalid("url",false, true, false, false, "")))
                .collect(Collectors.toList());
        gcSkuValidationDao.saveValidationResults(results, GcSkuValidationType.PICTURE_MBO_VALIDATION);

        Map<Long, List<ValidationWithMessages>> ticketsValidationMessagesMap = gcSkuTicketDao.getTicketsValidationMessagesMap(ticketIds);
        assertThat(ticketsValidationMessagesMap).hasSize(3);
        assertThat(ticketsValidationMessagesMap.keySet()).containsExactlyInAnyOrderElementsOf(ticketIds);
        assertThat(ticketsValidationMessagesMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()))
                .allMatch(v -> !v.getValidation().getIsOk());
        assertThat(ticketsValidationMessagesMap.values().stream()
                .flatMap(Collection::stream)
                .map(ValidationWithMessages::getMessages)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()))
                .hasSize(3)
                .allMatch(m -> m.getLevel() == MessageLevel.ERROR);
    }

    @Test
    public void testReadValidationMessagesWarnings() {
        List<Long> ticketIds = generateDBInitialStateAndGetIds(3);
        List<TicketValidationResult> results = ticketIds.stream()
                        .map(ticketId ->
                                    TicketValidationResult.validWithWarnings(ticketId, Arrays.asList(Messages.get(MessageInfo.Level.WARNING).pictureInvalid("url",false, true, false, false, ""))))
                .collect(Collectors.toList());
        gcSkuValidationDao.saveValidationResults(results, GcSkuValidationType.PICTURE_MBO_VALIDATION);

        Map<Long, List<ValidationWithMessages>> ticketsValidationMessagesMap = gcSkuTicketDao.getTicketsValidationMessagesMap(ticketIds);
        assertThat(ticketsValidationMessagesMap.keySet()).containsExactlyInAnyOrderElementsOf(ticketIds);
        assertThat(ticketsValidationMessagesMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()))
                .allMatch(v -> v.getValidation().getIsOk());
        assertThat(ticketsValidationMessagesMap.values().stream()
                .flatMap(Collection::stream)
                .map(ValidationWithMessages::getMessages)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()))
                .hasSize(3)
                .allMatch(m -> m.getLevel() == MessageLevel.WARNING);
    }

    @Test
    public void testReadValidationMessagesErrorsWarnings() {
        List<Long> ticketIds = generateDBInitialStateAndGetIds(3);
        List<TicketValidationResult> results = ticketIds.stream()
                        .map(ticketId ->
                                    TicketValidationResult.invalid(ticketId,
                                            Arrays.asList(
                                                    Messages.get().pictureInvalid("url",false, true, false, false, ""),
                                                    Messages.get(MessageInfo.Level.WARNING).pictureInvalid("url",false, true, false, false, "")
                                            )
                                    )
                        )
                .collect(Collectors.toList());
        gcSkuValidationDao.saveValidationResults(results, GcSkuValidationType.PICTURE_MBO_VALIDATION);

        Map<Long, List<ValidationWithMessages>> ticketsValidationMessagesMap = gcSkuTicketDao.getTicketsValidationMessagesMap(ticketIds);
        assertThat(ticketsValidationMessagesMap.keySet()).containsExactlyInAnyOrderElementsOf(ticketIds);
        assertThat(ticketsValidationMessagesMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()))
                .allMatch(v -> !v.getValidation().getIsOk());
        assertThat(ticketsValidationMessagesMap.values().stream()
                .flatMap(Collection::stream)
                .map(ValidationWithMessages::getMessages)
                .allMatch(ms -> ms.size() == 2
                        && ms.stream().anyMatch(m -> m.getLevel()==MessageLevel.ERROR)
                        && ms.stream().anyMatch(m -> m.getLevel()==MessageLevel.WARNING))).isTrue();
    }

    @Test
    public void testReadValidationMessagesDifferentValidationsErrorsWarnings() {
        List<Long> ticketIds = generateDBInitialStateAndGetIds(3);
        List<TicketValidationResult> titleFailResults = ticketIds.stream()
                        .map(ticketId ->
                                    TicketValidationResult.invalid(ticketId,
                                            Arrays.asList(Messages.get().invalidTitleLength("sku",100,
                                                    ParameterValueComposer.NAME_ID))
                                    )
                        )
                .collect(Collectors.toList());
        List<TicketValidationResult> mboPictureResults = ticketIds.stream()
                        .map(ticketId ->
                                    TicketValidationResult.validWithWarnings(ticketId,
                                            Arrays.asList(Messages.get(MessageInfo.Level.WARNING).pictureInvalid("url",false, true, false, false, ""))
                                    )
                        )
                .collect(Collectors.toList());
        gcSkuValidationDao.saveValidationResults(titleFailResults, GcSkuValidationType.TITLE_LENGTH_VALIDATION);
        gcSkuValidationDao.saveValidationResults(mboPictureResults, GcSkuValidationType.PICTURE_MBO_VALIDATION);

        Map<Long, List<ValidationWithMessages>> ticketsValidationMessagesMap = gcSkuTicketDao.getTicketsValidationMessagesMap(ticketIds);
        assertThat(ticketsValidationMessagesMap.keySet()).containsExactlyInAnyOrderElementsOf(ticketIds);
        assertThat(ticketsValidationMessagesMap.values().stream()
                .allMatch(vs -> vs.size()==2
                        && vs.stream().anyMatch(v -> v.getValidation().getIsOk())
                        && vs.stream().anyMatch(v -> !v.getValidation().getIsOk())
                )).isTrue();
        assertThat(ticketsValidationMessagesMap.values().stream()
                .flatMap(Collection::stream)
                .filter(v -> !v.getValidation().getIsOk())
                .map(ValidationWithMessages::getMessages)
                .allMatch(ms -> ms.size() == 1
                        && ms.stream().allMatch(m -> m.getLevel()==MessageLevel.ERROR))).isTrue();
        assertThat(ticketsValidationMessagesMap.values().stream()
                .flatMap(Collection::stream)
                .filter(v -> v.getValidation().getIsOk())
                .map(ValidationWithMessages::getMessages)
                .allMatch(ms -> ms.size() == 1
                        && ms.stream().anyMatch(m -> m.getLevel()==MessageLevel.WARNING))).isTrue();
    }
}
