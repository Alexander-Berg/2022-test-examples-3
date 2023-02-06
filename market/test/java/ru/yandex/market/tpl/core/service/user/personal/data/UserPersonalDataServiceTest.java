package ru.yandex.market.tpl.core.service.user.personal.data;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.user.partner.PartnerUserPersonalDataRequestDto;
import ru.yandex.market.tpl.common.covid.external.TplCovidExternalService;
import ru.yandex.market.tpl.common.covid.external.TplVaccinationInfo;
import ru.yandex.market.tpl.common.dsm.client.api.CourierApi;
import ru.yandex.market.tpl.common.util.exception.TplExternalException;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
class UserPersonalDataServiceTest extends TplAbstractTest {

    private static final Long UID = 1233539457L;
    public static final String PASSPORT = "4518123321";

    private final UserPersonalDataService userPersonalDataService;
    private final UserPersonalDataCommandService userPersonalDataCommandService;
    private final UserPersonalDataRepository userPersonalDataRepository;
    private final UserCommandService userCommandService;
    private final EntityManagerFactory entityManagerFactory;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final TplCovidExternalService covidExternalService;
    private final NationalityService nationalityService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final CourierApi courierApi;
    private final TransactionTemplate transactionTemplate;

    private User user;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        Mockito.reset(covidExternalService);
    }

    @Test
    void hasVaccinatedFalse_whenExpiredData() {
        //given
        TplVaccinationInfo vaccinationInfo = TplVaccinationInfo.builder()
                .expiredAt(LocalDate.now(clock).minusDays(1))
                .passport(PASSPORT)
                .build();
        when(covidExternalService.getVaccinationInfo(any()))
                .thenReturn(vaccinationInfo);

        //when
        userPersonalDataService.createOrUpdatePersonalData(
                user.getId(),
                PartnerUserPersonalDataRequestDto.builder()
                        .passport(PASSPORT)
                        .link("bad_link")
                        .nationality(nationalityService.getNationalities().iterator().next())
                        .build()
        );
        //then
        Optional<UserPersonalData> updO = userPersonalDataRepository.findByUserId(user.getId());

        assertThat(updO.isPresent()).isTrue();
        assertThat(updO.get().getHasVaccination()).isFalse();
    }


    @Test
    void hasVaccinatedFalse_whenCantgetData() {
        //given
        when(covidExternalService.getVaccinationInfo(any())).thenThrow(new TplExternalException("error"));

        //when
        userPersonalDataService.createOrUpdatePersonalData(
                user.getId(),
                PartnerUserPersonalDataRequestDto.builder()
                        .passport(PASSPORT)
                        .link("bad_link")
                        .nationality(nationalityService.getNationalities().iterator().next())
                        .build()
        );
        //then
        Optional<UserPersonalData> updO = userPersonalDataRepository.findByUserId(user.getId());

        assertThat(updO.isPresent()).isTrue();
        assertThat(updO.get().getHasVaccination()).isFalse();
    }

    @Test
    void passportDuplicateValidation() {
        saveUserPersonalData(user.getId());

        User user2 = testUserHelper.findOrCreateUser(UID + 123L);
        //  нельзя сохранить паспорт, если есть не уволенный курьер с таким же номером паспорта
        assertThatThrownBy(
                () -> userPersonalDataService.createOrUpdatePersonalData(
                        user2.getId(),
                        PartnerUserPersonalDataRequestDto.builder()
                                .passport(PASSPORT)
                                .build()
                )
        )
                .hasMessage(UserPersonalDataService.PASSPORT_VALIDATION_ERROR_MESSAGE);

        // после увольнения система может ругаться на другие ошибки валидации
        userCommandService.fire(new UserCommand.Fire(user.getId()));
        assertThatThrownBy(
                () -> userPersonalDataService.createOrUpdatePersonalData(
                        user2.getId(),
                        PartnerUserPersonalDataRequestDto.builder()
                                .passport(PASSPORT)
                                .build()
                )
        )
                .hasMessageNotContaining(UserPersonalDataService.PASSPORT_VALIDATION_ERROR_MESSAGE);

        // при повторной попытки сохранить перс. данные курьера с тем же паспортом не должно быть ошибки
        saveUserPersonalData(user2.getId());
        assertThatThrownBy(
                () -> userPersonalDataService.createOrUpdatePersonalData(
                        user2.getId(),
                        PartnerUserPersonalDataRequestDto.builder()
                                .passport(PASSPORT)
                                .build()
                )
        ).hasMessageNotContainingAny(
                "query did not return a unique result",
                UserPersonalDataService.PASSPORT_VALIDATION_ERROR_MESSAGE
        );
    }

    @Test
    void bulkUpdateCertState() {
        //given
        Long uid1 = testUserHelper.findOrCreateUser(UID + 333L).getId();
        Long uid2 = testUserHelper.findOrCreateUser(UID + 334L).getId();
        Long uid3 = testUserHelper.findOrCreateUser(UID + 335L).getId();
        Long uid4 = testUserHelper.findOrCreateUser(UID + 336L).getId();
        Stream.of(
                buildCommand(uid1, true, LocalDate.now(clock)),
                buildCommand(uid2, true, LocalDate.now(clock).minusDays(10L)),
                buildCommand(uid3, true, LocalDate.now(clock).plusDays(1L)),
                buildCommand(uid4, true, null)

        ).forEach(userPersonalDataCommandService::createOrUpdate);

        //when
        userPersonalDataService.bulkUpdateCertState();

        //then
        List<UserPersonalData> all = userPersonalDataRepository.findAll();
        assertThat(all).hasSize(4);

        assertThat(all.stream().filter(upd -> upd.getUserId().equals(uid1))
                .map(UserPersonalData::getHasVaccination).findFirst().orElseThrow()).isFalse();

        assertThat(all.stream().filter(upd -> upd.getUserId().equals(uid2))
                .map(UserPersonalData::getHasVaccination).findFirst().orElseThrow()).isFalse();

        assertThat(all.stream().filter(upd -> upd.getUserId().equals(uid3))
                .map(UserPersonalData::getHasVaccination).findFirst().orElseThrow()).isTrue();

        assertThat(all.stream().filter(upd -> upd.getUserId().equals(uid4))
                .map(UserPersonalData::getHasVaccination).findFirst().orElseThrow()).isTrue();
    }

    @Test
    void checkPersonalDataAudited() {
        UserPersonalData userPersonalData = saveUserPersonalData(user.getId());

        userPersonalData.setPassport("123213123");
        userPersonalDataRepository.save(userPersonalData);

        AuditReader auditReader = createAuditReader();
        List<Number> revisions = auditReader.getRevisions(
                UserPersonalData.class, userPersonalData.getId());

        assertThat(auditReader.isEntityClassAudited(UserPersonalData.class)).isTrue();
        assertThat(revisions).hasSize(2);
    }

    @Test
    void createOrUpdatePersonalData_withDsmIntegration() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.USER_DSM_INTEGRATION_FOR_EDIT_ENABLED, true);

        userPersonalDataService.createOrUpdatePersonalData(
                user.getId(),
                PartnerUserPersonalDataRequestDto.builder()
                        .passport("passport-that-not-exists")
                        .nationality(nationalityService.getNationalities().iterator().next())
                        .build()
        );

        verify(courierApi, times(1)).couriersIdPersonalDataPut(
                eq(user.getDsmExternalId()), any()
        );
    }

    @Test
    void createOrUpdatePersonalData_withTransactionFails() {
        assertThrows(
                IllegalTransactionStateException.class,
                () -> transactionTemplate.execute((status) -> userPersonalDataService.createOrUpdatePersonalData(
                        user.getId(),
                        PartnerUserPersonalDataRequestDto.builder()
                                .passport("passport-that-not-exists")
                                .nationality(nationalityService.getNationalities().iterator().next())
                                .build()
                ))
        );
    }

    private UserPersonalData saveUserPersonalData(Long userId) {
        return userPersonalDataCommandService.createOrUpdate(buildCommand(userId, false, null));
    }

    private AuditReader createAuditReader() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        return AuditReaderFactory.get(entityManager);
    }

    private UserPersonalDataCommand.CreateOrUpdate buildCommand(long uid,
                                                                boolean hasVaccinated,
                                                                LocalDate expiredAt) {

        return UserPersonalDataCommand.CreateOrUpdate
                .builder()
                .userId(uid)
                .birthdayDate(LocalDate.now(clock))
                .firstVaccinationDate(LocalDate.now(clock))
                .secondVaccinationDate(LocalDate.now(clock))
                .hasVaccination(hasVaccinated)
                .nationality("Российская Федерация")
                .passport(PASSPORT)
                .link("https://lolkek.com")
                .expiredAt(expiredAt)
                .build();
    }
}
