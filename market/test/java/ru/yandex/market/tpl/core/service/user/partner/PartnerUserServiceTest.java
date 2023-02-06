package ru.yandex.market.tpl.core.service.user.partner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.company.PartnerUserCompanyDto;
import ru.yandex.market.tpl.api.model.user.UserMode;
import ru.yandex.market.tpl.api.model.user.UserRegistrationStatus;
import ru.yandex.market.tpl.api.model.user.UserRole;
import ru.yandex.market.tpl.api.model.user.UserStatus;
import ru.yandex.market.tpl.api.model.user.UserType;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleColorDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleDataDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleRegistrationNumberCountry;
import ru.yandex.market.tpl.api.model.vehicle.VehicleInstanceTypeDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.dsm.client.api.CourierApi;
import ru.yandex.market.tpl.common.dsm.client.model.CourierDto;
import ru.yandex.market.tpl.common.dsm.client.model.CourierPersonalDataDto;
import ru.yandex.market.tpl.common.dsm.client.model.CourierRegistrationStatusDto;
import ru.yandex.market.tpl.common.dsm.client.model.CourierTypeDto;
import ru.yandex.market.tpl.common.dsm.client.model.CourierUpsertDto;
import ru.yandex.market.tpl.common.dsm.client.model.CouriersSearchItemDto;
import ru.yandex.market.tpl.common.dsm.client.model.CouriersSearchResultDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierRegistrationStatusDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierStatus;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierTypeDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerPersonalDataDto;
import ru.yandex.market.tpl.common.dsm.client.model.PersonalDataUpsertDto;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.core.domain.user.UserPropertyEntity;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.core.domain.usershift.additional_data.UserShiftAdditionalDataRepository;
import ru.yandex.market.tpl.core.domain.vehicle.Vehicle;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.color.VehicleColor;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderSupport;
import ru.yandex.market.tpl.core.service.UserIdToBindingType;
import ru.yandex.market.tpl.core.service.binding.BindingType;
import ru.yandex.market.tpl.core.service.user.UserPropsType;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleRegistrationNumberCountry.OTHER;
import static ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleRegistrationNumberCountry.RUS;

@RequiredArgsConstructor
@Sql("classpath:geobase/RegionDaoTest.sql")
public class PartnerUserServiceTest extends TplAbstractTest {

    private final TestUserHelper testUserHelper;

    private final PartnerUserService partnerUserService;
    private final UserCommandService userCommandService;
    private final UserRepository userRepository;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final VehicleGenerateService vehicleGenerateService;
    private final UserShiftAdditionalDataRepository userShiftAdditionalDataRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final CourierApi courierApi;
    private final CompanyRepository companyRepository;

    private final JdbcTemplate jdbcTemplate;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final int CRIMEA_REGION_ID = 977; // в тестовой базе имеет только один дочерний регион
    private final int SEVASTOPOL_ID = 959;
    private final int MOSCOW_ID = 213;
    private final int ROSTOV_NA_DONU_ID = 39;
    @MockBean
    YandexGoOrderSupport yandexGoOrderSupport;
    @MockBean
    BlackboxClient blackboxClient;
    @Autowired
    PartnerUserDtoMapper partnerUserDtoMapper;
    private User user;
    private User anotherUser;

    @BeforeEach
    void init() {
        Mockito.reset(courierApi);
        LocalDate date = LocalDate.now();
        user = testUserHelper.findOrCreateUser(955L, date);
        anotherUser = testUserHelper.findOrCreateUser(956L, date);
    }

    @AfterEach
    void after() {
        Mockito.reset(configurationProviderAdapter);
    }

    @Test
    void addNewRegionId() {
        partnerUserService.updateUserRegionIds(user.getId(), List.of(MOSCOW_ID));

        assertRegionHasSize(MOSCOW_ID);
    }

    @Test
    void updateRegionId() {
        partnerUserService.updateUserRegionIds(user.getId(), List.of(MOSCOW_ID));

        partnerUserService.updateUserRegionIds(user.getId(), List.of(SEVASTOPOL_ID));

        assertRegionHasSize(SEVASTOPOL_ID);
    }

    @Test
    void deleteRegionId() {
        partnerUserService.updateUserRegionIds(user.getId(), List.of(MOSCOW_ID));

        partnerUserService.updateUserRegionIds(user.getId(), List.of());

        assertThat(user.getRegionIds()).hasSize(0);
    }

    @Test
    void addRegionIdTwice() {
        partnerUserService.updateUserRegionIds(user.getId(), List.of(SEVASTOPOL_ID));

        partnerUserService.updateUserRegionIds(user.getId(), List.of(SEVASTOPOL_ID));

        assertRegionHasSize(SEVASTOPOL_ID);
    }

    private void assertRegionHasSize(int regionId) {
        transactionTemplate.execute(ts -> {
            user = userRepository.findByIdOrThrow(user.getId());
            assertThat(user.getRegionIds()).hasSize(1);
            assertThat(user.getRegionIds()).containsExactly(regionId);
            return null;
        });
    }

    @Test
    void deletePartialRegionIds() {
        partnerUserService.updateUserRegionIds(user.getId(), List.of(SEVASTOPOL_ID, ROSTOV_NA_DONU_ID));

        transactionTemplate.execute(ts -> {
            user = userRepository.findByIdOrThrow(user.getId());
            assertThat(user.getRegionIds()).hasSize(2);
            assertThat(user.getRegionIds()).containsExactlyInAnyOrder(SEVASTOPOL_ID, ROSTOV_NA_DONU_ID);
            return null;
        });


        partnerUserService.updateUserRegionIds(user.getId(), List.of(SEVASTOPOL_ID));

        assertRegionHasSize(SEVASTOPOL_ID);
    }

    @Test
    void addCouriersForRegion_whenBinding() {
        //given
        addDeliveryServiceRegion(239, 959, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now());
        addDeliveryServiceRegion(239, 977, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now());

        //when
        partnerUserService.updateCouriersInRegion(SEVASTOPOL_ID,
                List.of(new UserIdToBindingType(user.getId(), BindingType.STRONG),
                        new UserIdToBindingType(anotherUser.getId(), BindingType.SOFT)));

        //then
        assertThat(userRepository.findUsersByRegionIds(List.of(SEVASTOPOL_ID)).size()).isEqualTo(2);
        assertThat(userRepository.findUsersByRegionIds(List.of(SEVASTOPOL_ID)).stream()
                .filter(e -> Optional.ofNullable(e.getBindingType()).orElse(BindingType.SOFT).equals(BindingType.SOFT))
                .count()).isEqualTo(1);

    }

    @Test
    void shouldNotRemoveAnotherRegionForCourier() {
        addDeliveryServiceRegion(239, SEVASTOPOL_ID, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now());
        addDeliveryServiceRegion(239, CRIMEA_REGION_ID, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now());

        partnerUserService.updateUserRegionIds(user.getId(), List.of(ROSTOV_NA_DONU_ID));

        partnerUserService.updateCouriersInRegion(
                SEVASTOPOL_ID,
                List.of(
                        new UserIdToBindingType(user.getId(), BindingType.SOFT),
                        new UserIdToBindingType(anotherUser.getId(), BindingType.SOFT)
                )
        );

        transactionTemplate.execute(ts -> {
            user = userRepository.findByIdOrThrow(user.getId());
            anotherUser = userRepository.findByIdOrThrow(anotherUser.getId());
            assertThat(user.getRegionIds()).hasSize(2);
            assertThat(user.getRegionIds()).containsExactlyInAnyOrder(SEVASTOPOL_ID, ROSTOV_NA_DONU_ID);

            assertThat(anotherUser.getRegionIds()).hasSize(1);
            assertThat(anotherUser.getRegionIds()).containsExactly(SEVASTOPOL_ID);
            return null;
        });

    }

    @Test
    void removeCouriersForRegion() {
        addDeliveryServiceRegion(239, SEVASTOPOL_ID, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now());
        addDeliveryServiceRegion(239, CRIMEA_REGION_ID, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now());

        partnerUserService.updateUserRegionIds(user.getId(), List.of(SEVASTOPOL_ID));

        partnerUserService.updateCouriersInRegion(SEVASTOPOL_ID, List.of());

        assertThat(user.getRegionIds()).isEmpty();
    }

    @Test
    void updateCouriersForRegion() {
        addDeliveryServiceRegion(239, SEVASTOPOL_ID, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now());
        addDeliveryServiceRegion(239, CRIMEA_REGION_ID, LocalDateTime.now(), LocalDateTime.now(), LocalDate.now());

        User thirdUser = testUserHelper.findOrCreateUser(959L, LocalDate.now());

        partnerUserService.updateCouriersInRegion(
                SEVASTOPOL_ID,
                List.of(
                        new UserIdToBindingType(user.getId(), BindingType.SOFT),
                        new UserIdToBindingType(anotherUser.getId(), BindingType.SOFT)
                )
        );

        partnerUserService.updateCouriersInRegion(
                SEVASTOPOL_ID,
                List.of(
                        new UserIdToBindingType(thirdUser.getId(), BindingType.SOFT)
                )
        );

        transactionTemplate.execute(ts -> {
            user = userRepository.findByIdOrThrow(user.getId());
            anotherUser = userRepository.findByIdOrThrow(anotherUser.getId());
            var thirdUserTs = userRepository.findByIdOrThrow(thirdUser.getId());

            assertThat(user.getRegionIds()).isEmpty();
            assertThat(anotherUser.getRegionIds()).isEmpty();
            assertThat(thirdUserTs.getRegionIds()).hasSize(1);
            return null;
        });
    }

    @Test
    void shouldRunEnsureYandexGoUsersTags_whenCreateUser_IfUserIsYandexGoCourier() {
        // given
        String expectedUserLastName = "user-expectedUserName-for-test";
        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        PartnerUserDto userDto = partnerUserDtoMapper.toPartnerUser(user);
        userDto.setUid(null);
        userDto.setLastName(expectedUserLastName);
        Mockito.when(yandexGoOrderSupport.matchesUser(any())).thenReturn(true);

        userDto.setPhone("9998881133");
        // when
        partnerUserService.createUser(userDto, company);

        // then
        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(yandexGoOrderSupport).ensureUserTags(userArgumentCaptor.capture());
        assertThat(userArgumentCaptor.getValue().getLastName()).isEqualTo(expectedUserLastName);
    }

    @Test
    void shouldNotRunEnsureYandexGoUsersTags_whenCreateUser_IfUserIsNotYandexGoCourier() {
        // given
        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        PartnerUserDto userDto = partnerUserDtoMapper.toPartnerUser(user);
        userDto.setUid(null);
        Mockito.when(yandexGoOrderSupport.matchesUser(any())).thenReturn(false);
        userDto.setPhone("9998881139");
        // when
        partnerUserService.createUser(userDto, company);

        // then
        verify(yandexGoOrderSupport, never()).ensureUserTags(any());
    }

    @Test
    void shouldSetUserProperties_whenCreateUser_ifUserIsDeliveryBySellerCourier() {
        // given
        PartnerUserDto userDto = partnerUserDtoMapper.toPartnerUser(testUserHelper.createOrFindDbsUser());
        userDto.setPhone("9998881139");
        userDto.setEmail("dropship-by-seller@yandex.ru");
        userDto.setUid(null );
        Mockito.when(blackboxClient.getUidForLogin("dropship-by-seller")).thenReturn(1006360L);

        // when
        partnerUserService.createUser(userDto, mock(CompanyPermissionsProjection.class));

        Long userId = userRepository.findByUid(1006360L).orElseThrow().getId();

        assertAllUserPropertiesEqualTo(
                userId,
                Set.of(
                        UserPropsType.FEATURE_LIFE_POS_ENABLED,
                        UserPropsType.CLOUD_FISCALIZATION_ENABLED,
                        UserPropsType.PICKUP_TASK_CREATING_ENABLED,
                        UserPropsType.RETURN_TASK_CREATING_ENABLED,
                        UserPropsType.REROUTING_ENABLED,
                        UserPropsType.FEATURE_SUPPORT_CHATS_ENABLED,
                        UserPropsType.FEATURE_SUPPORT_HELP_ENABLED,
                        UserPropsType.ARRIVED_TO_RP_DISTANCE_FILTER_ENABLED,
                        UserPropsType.FEATURE_RATE_APP_ENABLED
                ),
                "false"
        );

        assertAllUserPropertiesEqualTo(
                userId,
                Set.of(
                        UserPropsType.CALL_TO_RECIPIENT_ENABLED,
                        UserPropsType.DBS_CALL_TO_RECIPIENT_DISABLED
                ),
                "true"
        );

        assertAllUserPropertiesEqualTo(
                userId,
                Set.of(
                        UserPropsType.USER_MODE
                ),
                UserMode.SOFT_MODE.name()
        );
    }

    @Test
    void shouldUpdateBoxBotUserUid() {
        var userDto = partnerUserDtoMapper.toPartnerUser(user);
        var uid = 95734652L;
        userDto.setEmail("new-user-email@yandex.ru");
        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        doReturn(uid).when(blackboxClient).getUidForLogin(eq(userDto.getLogin()));

        partnerUserService.updateUser(user.getId(), userDto, company);

        var queue = dbQueueTestUtil.getQueue(QueueType.LOCKER_USER);
        assertThat(queue).contains(Long.toString(user.getId()));
    }

    @ParameterizedTest
    @MethodSource("vehicleParameterizedData")
    void shouldAddUserWithVehicle(boolean shouldCreateWithVehicleColor,
                                  PartnerUserVehicleRegistrationNumberCountry registrationNumberCountry) {
        var color = vehicleGenerateService.generateVehicleColor("White");
        var vehicle = vehicleGenerateService.generateVehicle();
        var registrationNumber = "A000AA";
        PartnerUserDto userDto;
        if (shouldCreateWithVehicleColor) {
            userDto = getUserDtoForCreation(color, vehicle, registrationNumber, null, registrationNumberCountry);
        } else {
            userDto = getUserDtoForCreation(null, vehicle, registrationNumber, null, registrationNumberCountry);
        }

        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        userDto = partnerUserService.createUser(userDto, company);

        var createdUser = userRepository.findByIdWithVehicles(userDto.getId()).orElseThrow();

        assertThat(createdUser.getVehicleInstances()).isNotEmpty();
        var vehicleInstance = createdUser.getVehicleInstances().get(0);
        assertThat(vehicleInstance.getVehicle().getId()).isEqualTo(vehicle.getId());
        assertThat(vehicleInstance.getVehicle().getVehicleBrand().getId()).isEqualTo(vehicle.getVehicleBrand().getId());
        assertThat(vehicleInstance.getRegistrationNumber()).isEqualTo(userDto.getVehicles().get(0).getRegistrationNumber());
        assertThat(vehicleInstance.getRegistrationNumberRegion()).isEqualTo(userDto.getVehicles().get(0).getRegistrationNumberRegion());
        assertThat(userDto.getVehicles().get(0).getRegistrationNumberCountry()).isEqualTo(registrationNumberCountry);
        assertThat(createdUser.getVehicleNumber())
                .isEqualTo(vehicleInstance.getRegistrationNumber() + vehicleInstance.getRegistrationNumberRegion());

        if (shouldCreateWithVehicleColor) {
            assertThat(vehicleInstance.getColor().getId()).isEqualTo(color.getId());
        } else {
            assertThat(vehicleInstance.getColor()).isNull();
        }
    }

    @DisplayName("Не добавляем тс если не прошла валидация")
    @Test
    void shouldNotAddUserWithVehicleRegisterNumberValidationFail() {
        var color = vehicleGenerateService.generateVehicleColor("White");
        var vehicle = vehicleGenerateService.generateVehicle();
        var registrationNumber = "A00200AA";
        var userDto = getUserDtoForCreation(color, vehicle, registrationNumber, VehicleInstanceTypeDto.PERSONAL, RUS);

        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);

        var exception = assertThrows(
                TplIllegalArgumentException.class,
                () -> partnerUserService.createUser(userDto, company)
        );
        assertThat(exception.getMessage()).contains("должен состоять из 6 символов");
    }

    @DisplayName("Не добавляем тс если не заполнен номер тс")
    @Test
    void shouldNotAddUserWithVehicleWithoutRegisterNumber() {
        var color = vehicleGenerateService.generateVehicleColor("White");
        var vehicle = vehicleGenerateService.generateVehicle();
        var userDto = getUserDtoForCreation(color, vehicle, null, VehicleInstanceTypeDto.PUBLIC, RUS);

        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);

        var exception = assertThrows(
                TplIllegalArgumentException.class,
                () -> partnerUserService.createUser(userDto, company)
        );
        assertThat(exception.getMessage()).contains("Регистрационный номер ТС не может быть пустым");
    }

    @DisplayName("Не добавляем юзера если не пришли данные о тс")
    @Test
    void shouldNotUpdateUserWithoutVehicles() {
        var userDto = partnerUserDtoMapper.toPartnerUser(user);
        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.IS_VEHICLE_INSTANCE_REQUIRED_VALIDATION_ENABLED)
        ).thenReturn(true);

        var exception = assertThrows(
                TplIllegalArgumentException.class,
                () -> partnerUserService.updateUser(user.getId(), userDto, company)
        );
        assertThat(exception.getMessage()).contains("Необходимо заполнить данные о тс");
    }

    @DisplayName("Разрешаем обновить юзера, если у него есть связанные тс, а в дто на обновление нет тс")
    @Test
    void shouldUpdateUserWithoutVehiclesInDto() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.IS_VEHICLE_INSTANCE_REQUIRED_VALIDATION_ENABLED)
        ).thenReturn(true);

        var color = vehicleGenerateService.generateVehicleColor("White");
        var vehicle = vehicleGenerateService.generateVehicle();
        var registrationNumber = "A000AA";
        PartnerUserDto userDto = getUserDtoForCreation(color, vehicle, registrationNumber, null, RUS);

        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        userDto = partnerUserService.createUser(userDto, company);

        userDto.setVehicles(List.of());
        var result = partnerUserService.updateUser(userDto.getId(), userDto, company);
        assertThat(result.getVehicles()).isNotEmpty();
    }

    @DisplayName("Разрешаем добавить DBS юзера без тс")
    @Test
    void shouldAddDbsUserWithoutVehiclesInDto() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.IS_VEHICLE_INSTANCE_REQUIRED_VALIDATION_ENABLED)
        ).thenReturn(true);

        var userDto = partnerUserDtoMapper.toPartnerUser(testUserHelper.createOrFindDbsUser());
        userDto.setEmail("new-dbs-user-mail@ya.ru");
        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);

        assertThat(userDto.getVehicles()).isEmpty();

        var result = assertDoesNotThrow(() -> partnerUserService.updateUser(userDto.getId(), userDto, company));

        assertThat(result.getVehicles()).isEmpty();
    }

    @Test
    void shouldAddUserWithMultipleVehicles() {
        var color = vehicleGenerateService.generateVehicleColor("White");
        var vehicle = vehicleGenerateService.generateVehicle();
        var registrationNumber = "A000AA";
        var userDto = getUserDtoForCreation(color, vehicle, registrationNumber, null, RUS);

        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        userDto = partnerUserService.createUser(userDto, company);

        var createdUser = userRepository.findByIdWithVehicles(userDto.getId()).orElseThrow();

        assertThat(createdUser.getVehicleInstances()).isNotEmpty();
        var vehicleInstance = createdUser.getVehicleInstances().get(0);
        assertThat(vehicleInstance.getVehicle().getVehicleBrand().getId()).isEqualTo(vehicle.getVehicleBrand().getId());
        assertThat(vehicleInstance.getRegistrationNumber()).isEqualTo(userDto.getVehicles().get(0).getRegistrationNumber());
        assertThat(vehicleInstance.getRegistrationNumberRegion()).isEqualTo(userDto.getVehicles().get(0).getRegistrationNumberRegion());
        assertThat(vehicleInstance.getColor().getId()).isEqualTo(color.getId());

        var vehicle2 = vehicleGenerateService.generateVehicle("Skoda", "Rapid");
        var registrationNumber2 = "P888PP";

        userDto.setVehicles(List.of(
                userDto.getVehicles().get(0),
                PartnerUserVehicleDto.builder()
                        .vehicleData(PartnerUserVehicleDataDto.builder().id(vehicle2.getId()).build())
                        .color(PartnerUserVehicleColorDto.builder().id(color.getId()).build())
                        .registrationNumber(registrationNumber2)
                        .registrationNumberRegion("111")
                        .build()));

        partnerUserService.updateUser(userDto.getId(), userDto, company);
        createdUser = userRepository.findByIdWithVehicles(userDto.getId()).orElseThrow();

        assertThat(createdUser.getVehicleInstances()).hasSize(2);
    }

    @Test
    void shouldUpdateVehicleData() {
        var color = vehicleGenerateService.generateVehicleColor("White");
        var vehicle = vehicleGenerateService.generateVehicle();
        var registrationNumber = "A000AA";
        var userDto = getUserDtoForCreation(color, vehicle, registrationNumber, null, OTHER);

        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        userDto = partnerUserService.createUser(userDto, company);

        var createdUser = userRepository.findByIdWithVehicles(userDto.getId()).orElseThrow();

        assertThat(createdUser.getVehicleInstances()).isNotEmpty();
        var vehicleInstance = createdUser.getVehicleInstances().get(0);
        assertThat(vehicleInstance.getVehicle().getVehicleBrand().getId()).isEqualTo(vehicle.getVehicleBrand().getId());
        assertThat(vehicleInstance.getRegistrationNumber()).isEqualTo(userDto.getVehicles().get(0).getRegistrationNumber());
        assertThat(vehicleInstance.getRegistrationNumberRegion()).isEqualTo(userDto.getVehicles().get(0).getRegistrationNumberRegion());
        assertThat(vehicleInstance.getColor().getId()).isEqualTo(color.getId());
        assertThat(userDto.getVehicles().get(0).getRegistrationNumberCountry()).isEqualTo(OTHER);

        var color2 = vehicleGenerateService.generateVehicleColor("Black");
        var vehicle2 = vehicleGenerateService.generateVehicle("Skoda", "Rapid");
        var registrationNumber2 = "P888PP";

        userDto.setVehicles(List.of(PartnerUserVehicleDto.builder()
                .vehicleInstanceId(userDto.getVehicles().get(0).getVehicleInstanceId())
                .vehicleData(PartnerUserVehicleDataDto.builder().id(vehicle2.getId()).build())
                .color(PartnerUserVehicleColorDto.builder().id(color2.getId()).build())
                .registrationNumber(registrationNumber2)
                .registrationNumberCountry(RUS)
                .registrationNumberRegion("111")
                .build()));

        var userShift = testUserHelper.createEmptyShift(createdUser, LocalDate.now());

        userDto = partnerUserService.updateUser(userDto.getId(), userDto, company);

        var additionalData = userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId()).orElseThrow();

        assertThat(userDto.getVehicles().get(0).getRegistrationNumberCountry()).isEqualTo(RUS);
        assertThat(additionalData.getVehicle().getId()).isEqualTo(vehicle2.getId());
        assertThat(additionalData.getVehicleRegisterNumber()).contains(registrationNumber2);

        createdUser = userRepository.findByIdWithVehicles(userDto.getId()).orElseThrow();

        assertThat(createdUser.getVehicleInstances()).isNotEmpty();
        var vehicleInstance2 = createdUser.getVehicleInstances().get(0);
        assertThat(vehicleInstance2.getVehicle().getVehicleBrand().getId()).isEqualTo(vehicle2.getVehicleBrand().getId());
        assertThat(vehicleInstance2.getRegistrationNumber()).isEqualTo(registrationNumber2);
        assertThat(vehicleInstance2.getColor().getId()).isEqualTo(color2.getId());
    }

    @Test
    void testAddUserWithoutVehicleMandatoryFields() {
        var color = vehicleGenerateService.generateVehicleColor("White");
        var userDto = getUserDtoForCreation(color, null, null, null, RUS);

        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        var exception = assertThrows(
                TplIllegalArgumentException.class,
                () -> partnerUserService.createUser(userDto, company)
        );

        assertThat(exception.getMessage()).contains("Необходимо выбрать марку и модель ТС");
    }

    @Test
    void createUser_withDsmIntegration() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.USER_DSM_INTEGRATION_FOR_EDIT_ENABLED)
        ).thenReturn(true);

        String dsmCourierId = "test-id";

        var userDto = getUserDtoForCreation(
                vehicleGenerateService.generateVehicleColor("White"),
                vehicleGenerateService.generateVehicle(),
                "A000AA", null, OTHER
        );
        userDto.setUid(1987L);

        when(blackboxClient.getUidForLogin(userDto.getLogin())).thenReturn(userDto.getUid());

        when(
                courierApi.couriersGet(
                        eq(0),
                        eq(1),
                        eq(null),
                        eq(Long.toString(userDto.getUid())),
                        eq(null),
                        eq(null)
                )
        ).thenReturn(
                new CouriersSearchResultDto()
        );

        when(
                courierApi.couriersPutWithHttpInfo(
                        getCourierUpsertDto(null, userDto, user.getCompany())
                )
        ).thenReturn(
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(new CourierDto().id(dsmCourierId))
        );

        partnerUserService.createUser(userDto, mock(CompanyPermissionsProjection.class));

        verify(courierApi, times(1)).couriersPutWithHttpInfo(any());

        Optional<User> userOpt = userRepository.findByUid(userDto.getUid());
        assertThat(userOpt.isEmpty()).isFalse();

        User user = userOpt.get();
        assertThat(user.getDsmExternalId()).isEqualTo(dsmCourierId);
    }

    @Test
    void createUser_withDsmIntegration_whenDsmCourierExists() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.USER_DSM_INTEGRATION_FOR_EDIT_ENABLED)
        ).thenReturn(true);

        String dsmCourierId = "test-id";

        var userDto = getUserDtoForCreation(
                vehicleGenerateService.generateVehicleColor("White"),
                vehicleGenerateService.generateVehicle(),
                "A000AA", null, OTHER
        );
        userDto.setUid(1987L);

        when(blackboxClient.getUidForLogin(userDto.getLogin())).thenReturn(userDto.getUid());

        CouriersSearchResultDto searchDto = new CouriersSearchResultDto();
        CouriersSearchItemDto searchCourierDto = new CouriersSearchItemDto();
        searchCourierDto.setId(dsmCourierId);
        searchDto.setContent(List.of(
                searchCourierDto
        ));

        when(
                courierApi.couriersGet(
                        eq(0),
                        eq(1),
                        eq(null),
                        eq(Long.toString(userDto.getUid())),
                        eq(null),
                        eq(null)
                )
        ).thenReturn(searchDto);

        when(
                courierApi.couriersPutWithHttpInfo(
                        getCourierUpsertDto(dsmCourierId, userDto, user.getCompany())
                )
        ).thenReturn(
                ResponseEntity.status(HttpStatus.OK)
                        .body(new CourierDto().id(dsmCourierId))
        );

        partnerUserService.createUser(userDto, mock(CompanyPermissionsProjection.class));

        verify(courierApi, times(1)).couriersPutWithHttpInfo(any());

        Optional<User> userOpt = userRepository.findByUid(userDto.getUid());
        assertThat(userOpt.isEmpty()).isFalse();

        User user = userOpt.get();
        assertThat(user.getDsmExternalId()).isEqualTo(dsmCourierId);
    }

    @Test
    void createUser_withTransactionFails() {
        var userDto = partnerUserDtoMapper.toPartnerUser(user);
        assertThrows(
                IllegalTransactionStateException.class,
                () -> transactionTemplate.execute((status) -> partnerUserService.createUser(
                        userDto, mock(CompanyPermissionsProjection.class)
                ))
        );
    }

    @Test
    void updateUser_withDsmIntegration() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.USER_DSM_INTEGRATION_FOR_EDIT_ENABLED)
        ).thenReturn(true);

        String dsmCourierId = "test-id";

        var userDto = partnerUserDtoMapper.toPartnerUser(user);
        userDto.setUid(user.getUid());

        when(blackboxClient.getUidForLogin(userDto.getLogin())).thenReturn(userDto.getUid());

        when(
                courierApi.couriersGet(
                        eq(0),
                        eq(1),
                        eq(null),
                        eq(Long.toString(user.getUid())),
                        eq(null),
                        eq(null)
                )
        ).thenReturn(
                new CouriersSearchResultDto()
        );

        when(
                courierApi.couriersPutWithHttpInfo(
                        getCourierUpsertDto(user.getDsmExternalId(), userDto, user.getCompany())
                )
        ).thenReturn(
                ResponseEntity.status(HttpStatus.OK)
                        .body(new CourierDto().id(dsmCourierId))
        );

        partnerUserService.updateUser(user.getId(), userDto, mock(CompanyPermissionsProjection.class));

        verify(courierApi, times(1)).couriersPutWithHttpInfo(any());

        transactionTemplate.execute((status) -> {
            User userUpdated = userRepository.getById(user.getId());
            assertThat(userUpdated.getDsmExternalId()).isEqualTo(dsmCourierId);
            return null;
        });
    }

    @Test
    void updateUser_withTransactionFails() {
        var userDto = partnerUserDtoMapper.toPartnerUser(user);
        assertThrows(
                IllegalTransactionStateException.class,
                () -> transactionTemplate.execute((status) -> partnerUserService.updateUser(
                        user.getId(), userDto, mock(CompanyPermissionsProjection.class)
                ))
        );
    }

    @Test
    void upsertUserByDsm_success() {
        String patronymic = "Иванович";
        String phone = "88006089838";
        String telegram = "COUR";
        var userId = createUserWithVehicle().getId();
        Company company = testUserHelper.findOrCreateCompany("COMPANY457578594", "COMPANY438924778");
        company.setDsmExternalId("fdjkhge07t03507340");
        companyRepository.save(company);

        LogbrokerCourierDto courierDto = transactionTemplate.execute((status) -> {
            var user = userRepository.getById(userId);
            LogbrokerCourierDto dto = new LogbrokerCourierDto();
            String dsmId = user.getDsmExternalId();
            dto.setId(dsmId);
            dto.setVersion(user.getDsmVersion() + 1);
            dto.setUid(Long.toString(user.getUid()));
            dto.setEmployerId(company.getDsmExternalId());
            dto.setCourierRegistrationStatus(LogbrokerCourierRegistrationStatusDto.READY_TO_BE_SELF_EMPLOYED);
            dto.setCourierType(LogbrokerCourierTypeDto.SELF_EMPLOYED);
            dto.setDeleted(true);
            LogbrokerPersonalDataDto logbrokerPersonalDataDto = new LogbrokerPersonalDataDto();
            logbrokerPersonalDataDto.setEmail("test857499@mail.ru");
            logbrokerPersonalDataDto.setName("TEST TESTOV");
            logbrokerPersonalDataDto.setVaccinated(true);
            logbrokerPersonalDataDto.setPhone(phone);
            logbrokerPersonalDataDto.setPatronymicName(patronymic);
            logbrokerPersonalDataDto.setTelegramLogin(telegram);
            dto.setPersonalData(logbrokerPersonalDataDto);

            return dto;
        });

        partnerUserService.upsertUserByDsm(courierDto);

        transactionTemplate.execute((status) -> {
            Optional<User> updatedUserOpt = userRepository.findUserByDsmExternalId(courierDto.getId());
            assertThat(updatedUserOpt.isPresent()).isTrue();

            User user = updatedUserOpt.get();
            assertThat(user.getVehicleInstances()).isNotEmpty();
            assertThat(user.getRegistrationStatus()).isEqualTo(UserRegistrationStatus.READY_TO_BE_SELF_EMPLOYED);
            assertThat(user.getUserType()).isEqualTo(UserType.SELF_EMPLOYED);
            assertThat(user.getTelegram()).isEqualTo(telegram);
            assertThat(user.getPatronymic()).isEqualTo(patronymic);
            assertThat(user.getPhone()).isEqualTo(phone);
            assertThat(user.getCompany().getId()).isEqualTo(company.getId());

            return null;
        });
    }

    @Test
    void upsertUserByDsm_ignoreOldVersions() {
        var userId = createUserWithVehicle().getId();
        Company company = testUserHelper.findOrCreateCompany("COMPANY457578594", "COMPANY438924778");
        company.setDsmExternalId("fdjkhge07t03507340");
        companyRepository.save(company);

        LogbrokerCourierDto courierDto = transactionTemplate.execute((status) -> {
            var user = userRepository.getById(userId);

            LogbrokerCourierDto dto = new LogbrokerCourierDto();
            dto.setId(user.getDsmExternalId());
            dto.setVersion(user.getDsmVersion() - 1);
            dto.setUid(Long.toString(user.getUid()));
            dto.setEmployerId(company.getDsmExternalId());

            return dto;
        });

        partnerUserService.upsertUserByDsm(courierDto);

        transactionTemplate.execute((status) -> {
            Optional<User> updatedUserOpt = userRepository.findUserByDsmExternalId(courierDto.getId());
            assertThat(updatedUserOpt.isPresent()).isTrue();

            User userUpdated = updatedUserOpt.get();
            assertThat(userUpdated.getDsmVersion()).isGreaterThan(courierDto.getVersion());

            return null;
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = LogbrokerCourierStatus.class,
            names = {
                    "REVIEW",
                    "NEWBIE",
                    "ACTIVE"
            }
    )
    void upsertUserByDsm_createSuccess(LogbrokerCourierStatus courierStatus) {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.COURSE_AUTO_ASSIGNMENT_ENABLED)
        ).thenReturn(true);

        String patronymic = "Иванович";
        String phone = "88006089838";
        String telegram = "COUR";
        Company company = testUserHelper.findOrCreateCompany("COMPANY457578594", "COMPANY438924778");
        company.setDsmExternalId("fdjkhge07t03507340");
        companyRepository.save(company);

        LogbrokerCourierDto courierDto = transactionTemplate.execute((status) -> {
            LogbrokerCourierDto dto = new LogbrokerCourierDto();
            dto.setId("random-dsm-id");
            dto.setUid(Long.toString(999999991123L));
            dto.setEmployerId(company.getDsmExternalId());
            dto.setCourierRegistrationStatus(LogbrokerCourierRegistrationStatusDto.READY_TO_BE_SELF_EMPLOYED);
            dto.setCourierType(LogbrokerCourierTypeDto.SELF_EMPLOYED);
            dto.setStatus(courierStatus);
            dto.setDeleted(false);
            LogbrokerPersonalDataDto logbrokerPersonalDataDto = new LogbrokerPersonalDataDto();
            logbrokerPersonalDataDto.setEmail("test857499@mail.ru");
            logbrokerPersonalDataDto.setFirstName("TEST");
            logbrokerPersonalDataDto.setLastName("TESTOV");
            logbrokerPersonalDataDto.setVaccinated(true);
            logbrokerPersonalDataDto.setPhone(phone);
            logbrokerPersonalDataDto.setPatronymicName(patronymic);
            logbrokerPersonalDataDto.setTelegramLogin(telegram);
            dto.setPersonalData(logbrokerPersonalDataDto);

            return dto;
        });

        partnerUserService.upsertUserByDsm(courierDto);

        transactionTemplate.execute((status) -> {
            Optional<User> updatedUserOpt = userRepository.findUserByDsmExternalId(courierDto.getId());
            assertThat(updatedUserOpt.isPresent()).isTrue();

            User user = updatedUserOpt.get();
            assertThat(user.getRegistrationStatus()).isEqualTo(UserRegistrationStatus.READY_TO_BE_SELF_EMPLOYED);
            assertThat(user.getUserType()).isEqualTo(UserType.SELF_EMPLOYED);
            assertThat(user.getTelegram()).isEqualTo(telegram);
            assertThat(user.getPatronymic()).isEqualTo(patronymic);
            assertThat(user.getPhone()).isEqualTo(phone);
            assertThat(user.getCompany().getId()).isEqualTo(company.getId());
            assertThat(user.getStatus()).isEqualTo(partnerUserDtoMapper.toStatus(courierStatus));

            return null;
        });
    }

    @Test
    void upsertUserByDsm_fired_success() {
        var userId = createUserWithVehicle().getId();
        userCommandService.inProcessOfDismissal(new UserCommand.InProcessOfDismissal(userId));

        dbQueueTestUtil.clear(QueueType.LOCKER_USER);

        LogbrokerCourierDto courierDto = transactionTemplate.execute((status) -> {
            var user = userRepository.getById(userId);

            LogbrokerCourierDto dto = new LogbrokerCourierDto();
            String dsmId = user.getDsmExternalId();
            dto.setId(dsmId);
            dto.setVersion(user.getDsmVersion() + 1);
            dto.setUid(Long.toString(user.getUid()));
            dto.setEmployerId(user.getCompany().getDsmExternalId());
            dto.setCourierRegistrationStatus(LogbrokerCourierRegistrationStatusDto.REGISTERED);
            dto.setCourierType(LogbrokerCourierTypeDto.PARTNER);
            dto.setDeleted(true);
            dto.setStatus(LogbrokerCourierStatus.FIRED);
            LogbrokerPersonalDataDto logbrokerPersonalDataDto = new LogbrokerPersonalDataDto();
            logbrokerPersonalDataDto.setEmail(user.getEmail());
            logbrokerPersonalDataDto.setName(user.getName());
            logbrokerPersonalDataDto.setVaccinated(true);
            logbrokerPersonalDataDto.setPhone(user.getPhone());
            logbrokerPersonalDataDto.setPatronymicName(user.getPatronymic());
            logbrokerPersonalDataDto.setTelegramLogin(user.getTelegram());
            dto.setPersonalData(logbrokerPersonalDataDto);

            return dto;
        });

        partnerUserService.upsertUserByDsm(courierDto);

        transactionTemplate.execute((status) -> {
            Optional<User> updatedUserOpt = userRepository.findUserByDsmExternalId(courierDto.getId());
            assertThat(updatedUserOpt.isPresent()).isTrue();

            User user = updatedUserOpt.get();
            assertThat(user.getStatus()).isEqualTo(UserStatus.FIRED);

            dbQueueTestUtil.assertTasksHasSize(QueueType.LOCKER_USER, 1);

            return null;
        });
    }

    @Test
    void getById_withoutDsm() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.USER_DSM_INTEGRATION_FOR_VIEW_ENABLED)
        ).thenReturn(false);

        var result = partnerUserService.getById(user.getId(), mock(CompanyPermissionsProjection.class));

        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getName()).isEqualTo(user.getName());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.isDeleted()).isEqualTo(user.isDeleted());
        assertThat(result.getDsmExternalId()).isEqualTo(user.getDsmExternalId());
        assertThat(result.getDsmVersion()).isEqualTo(user.getDsmVersion());
    }

    @Test
    void getById_withDsm() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.USER_DSM_INTEGRATION_FOR_VIEW_ENABLED)
        ).thenReturn(true);

        when(courierApi.couriersIdGet(user.getDsmExternalId()))
                .thenReturn(
                        new CourierDto()
                                .id(user.getDsmExternalId())
                                .uid(Long.toString(user.getUid()))
                                .name(user.getName())
                                .email(user.getEmail())
                                .employerId(user.getCompany().getDsmExternalId())
                                .deleted(user.isDeleted())
                );
        when(courierApi.couriersIdPersonalDataGet(user.getDsmExternalId()))
                .thenReturn(
                        new CourierPersonalDataDto()
                                .id("personal-data-id")
                                .phone(user.getPhone())
                );

        var result = partnerUserService.getById(user.getId(), mock(CompanyPermissionsProjection.class));

        verify(courierApi, times(1)).couriersIdGet(user.getDsmExternalId());
        verify(courierApi, times(1)).couriersIdPersonalDataGet(user.getDsmExternalId());

        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getName()).isEqualTo(user.getName());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getPhone()).isEqualTo(user.getPhone());
        assertThat(result.isDeleted()).isEqualTo(user.isDeleted());
        assertThat(result.getDsmExternalId()).isEqualTo(user.getDsmExternalId());
        assertThat(result.getDsmVersion()).isEqualTo(user.getDsmVersion());
    }

    private CourierUpsertDto getCourierUpsertDto(
            @Nullable String id,
            PartnerUserDto partnerUserDto,
            Company company
    ) {
        return new CourierUpsertDto()
                .id(id)
                .uid(Long.toString(partnerUserDto.getUid()))
                .employerId(company.getDsmExternalId())
                .deleted(false)
                .courierRegistrationStatus(CourierRegistrationStatusDto.REGISTERED)
                .courierType(null)
                .personalData(
                        new PersonalDataUpsertDto()
                                .email(partnerUserDto.getEmail())
                                .name(partnerUserDto.getName())
                                .lastName(partnerUserDto.getLastName())
                                .firstName(partnerUserDto.getFirstName())
                                .patronymicName(partnerUserDto.getPatronymic())
                                .phone(partnerUserDto.getPhone())
                                .telegramLogin(partnerUserDto.getTelegram())
                );
    }

    private PartnerUserDto getUserDtoForCreation(VehicleColor color, Vehicle vehicle,
                                                 String vehicleRegistrationNumber, VehicleInstanceTypeDto type,
                                                 PartnerUserVehicleRegistrationNumberCountry registrationNumberCountry) {
        String dsmExternalId = UUID.randomUUID().toString();
        return PartnerUserDto.builder()
                .dsmExternalId(dsmExternalId)
                .dsmVersion(-1L)
                .dsmVersion(1L)
                .email("fake-mail@yandex.ru")
                .firstName("AAAA")
                .lastName("BBBB")
                .phone("+79999999999")
                .status(UserStatus.ACTIVE)
                .company(PartnerUserCompanyDto.builder()
                        .id(user.getCompany().getId())
                        .build())
                .role(UserRole.COURIER)
                .vehicles(List.of(
                        PartnerUserVehicleDto.builder()
                                .vehicleData(
                                        Optional.ofNullable(vehicle).map(it -> PartnerUserVehicleDataDto.builder()
                                                        .id(it.getId())
                                                        .build())
                                                .orElse(null))

                                .color(
                                        Optional.ofNullable(color).map(vehicleColor -> PartnerUserVehicleColorDto.builder()
                                                        .id(vehicleColor.getId())
                                                        .build())
                                                .orElse(null)

                                )
                                .registrationNumber(vehicleRegistrationNumber)
                                .registrationNumberRegion("077")
                                .registrationNumberCountry(registrationNumberCountry)
                                .type(type)
                                .build()
                ))
                .build();
    }

    private User createUserWithVehicle() {
        var color = vehicleGenerateService.generateVehicleColor("White");
        var vehicle = vehicleGenerateService.generateVehicle();
        var registrationNumber = "A000AA";
        PartnerUserDto userDto = getUserDtoForCreation(
                color, vehicle, registrationNumber, null, RUS
        );

        CompanyPermissionsProjection company = mock(CompanyPermissionsProjection.class);
        return userRepository.findByIdWithVehicles(
                partnerUserService.createUser(userDto, company).getId()
        ).get();
    }

    private void assertAllUserPropertiesEqualTo(
            Long userId,
            Set<UserPropsType> propertyTypes,
            String expectedValue
    ) {
        List<String> propertyNames = propertyTypes.stream()
                .map(UserPropsType::getName)
                .collect(Collectors.toList());
        List<UserPropertyEntity> properties =
                userPropertyService.findByNameInAndUserIdIn(propertyNames, List.of(userId));
        assertThat(properties).hasSameSizeAs(propertyNames);
        assertThat(properties.stream().map(UserPropertyEntity::getName))
                .containsExactlyInAnyOrderElementsOf(propertyNames);
        assertThat(properties.stream().map(UserPropertyEntity::getValue)).allMatch(expectedValue::equals);
    }

    private void addDeliveryServiceRegion(long dsId, int regionId, LocalDateTime createdAt,
                                          LocalDateTime updatedAt, LocalDate createdDate) {
        jdbcTemplate.update("INSERT into delivery_service_region (delivery_service_id, region_id, " +
                        "has_courier_delivery, has_pickup_delivery, created_at, updated_at, created_date, " +
                        "macro_region_id, data_version) " +
                        "VALUES (?, ?, true, true, ?, ?, ?, 1, 0)",
                dsId, regionId, createdAt, updatedAt, createdDate);
    }

    private static Stream<Arguments> vehicleParameterizedData() {
        return Stream.of(
                Arguments.of(true, RUS),
                Arguments.of(false, OTHER)
        );
    }
}
