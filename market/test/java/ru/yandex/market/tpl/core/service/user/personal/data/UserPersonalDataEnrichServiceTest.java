package ru.yandex.market.tpl.core.service.user.personal.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.common.covid.external.TplCovidExternalService;
import ru.yandex.market.tpl.common.covid.external.TplVaccinationInfo;
import ru.yandex.market.tpl.common.util.exception.TplExternalException;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static ru.yandex.market.tpl.core.service.user.personal.data.UserPersonalDataEnrichService.BULK_SIZE;

@RequiredArgsConstructor
class UserPersonalDataEnrichServiceTest extends TplAbstractTest {

    public static final int GENERATED_UID_OFFSET = 7000;
    public static final String LINK_PREFIX = "link_";
    public static final int TOTAL_GENERATED_ITEMS = BULK_SIZE * 2;
    public static final int CORRECT_NOT_EMPTY_ANSWERS_QTY =  TOTAL_GENERATED_ITEMS / 4;
    public static final int CORRECT_EMPTY_ANSWERS_QTY = TOTAL_GENERATED_ITEMS / 4;
    public static final int ERROR_ANSWERS_QTY = TOTAL_GENERATED_ITEMS / 4;
    public static final LocalDate EXPIRED_AT_OFFSET = LocalDate.of(1990, 12, 12);
    private final UserPersonalDataCommandService commandService;
    private final UserPersonalDataRepository userPersonalDataRepository;
    private final UserPersonalDataEnrichService enrichService;
    private final TplCovidExternalService externalService;

    private final TestUserHelper helper;

    @BeforeEach
    void setUp() {
        AtomicInteger index = new AtomicInteger(0);
        List<String> generatedList = Stream.generate(() -> buildCommand(index.incrementAndGet()))
                .map(commandService::createOrUpdate)
                .limit(TOTAL_GENERATED_ITEMS)
                .map(UserPersonalData::getLink)
                .collect(Collectors.toList());

        //Mock correct not empty results
        index.set(0);
        Stream.generate(() -> {
            int i = index.getAndAdd(4);
            configureExternalServiceNotEmptyResult(i, generatedList.get(i));
            return i;
        }).limit(CORRECT_NOT_EMPTY_ANSWERS_QTY)
                .collect(Collectors.toList());

        //Mock correct empty results
        index.set(1);
        Stream.generate(() -> {
            int i = index.getAndAdd(4);
            configureExternalServiceEmptyResult(i, generatedList.get(i));
            return i;
        }).limit(CORRECT_EMPTY_ANSWERS_QTY)
                .collect(Collectors.toList());

        //Mock correct empty results
        index.set(2);
        Stream.generate(() -> {
            int i = index.getAndAdd(4);
            configureExternalServiceErrorResult(i, generatedList.get(i));
            return i;
        }).limit(ERROR_ANSWERS_QTY)
                .collect(Collectors.toList());


    }

    @AfterEach
    void after() {
        Mockito.reset(externalService);
    }

    private void configureExternalServiceNotEmptyResult(int i, String link) {
        Mockito.when(externalService.getVaccinationInfo(link)).thenReturn(TplVaccinationInfo
                .builder()
                .expiredAt(EXPIRED_AT_OFFSET.plusDays(i))
                .build());
    }

    private void configureExternalServiceEmptyResult(int i, String link) {
        Mockito.when(externalService.getVaccinationInfo(link)).thenReturn(TplVaccinationInfo
                .builder()
                .build());
    }

    private void configureExternalServiceErrorResult(int i, String link) {
        Mockito.when(externalService.getVaccinationInfo(link))
                .thenThrow(new TplExternalException("external error"));
    }

    @Test
    void enrichingDates() {
        //given
        //when
        enrichService.enrichAllWithExpiredAt();

        //then
        List<UserPersonalData> allUPD = userPersonalDataRepository.findAll();
        Assertions.assertThat(allUPD).hasSize(TOTAL_GENERATED_ITEMS);

        Assertions.assertThat(allUPD.stream().map(UserPersonalData::getExpiredAt).filter(Objects::nonNull).distinct().collect(Collectors.toList())).hasSize(CORRECT_NOT_EMPTY_ANSWERS_QTY);

    }

    private UserPersonalDataCommand.CreateOrUpdate buildCommand(int i) {

        User user = helper.findOrCreateUser(GENERATED_UID_OFFSET + i);

        return UserPersonalDataCommand.CreateOrUpdate
                .builder()
                .userId(user.getId())
                .birthdayDate(LocalDate.of(1990, 1, 1))
                .firstVaccinationDate(LocalDate.of(2021, 5, 1))
                .secondVaccinationDate(LocalDate.of(2021, 5, 21))
                .hasVaccination(true)
                .link(LINK_PREFIX + i)
                .nationality("nationality")
                .passport("passport")
                .build();
    }
}
