package ru.yandex.market.tpl.carrier.core.domain.shift;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.company.CompanyCommandService;
import ru.yandex.market.tpl.carrier.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.carrier.core.domain.company.CompanyType;
import ru.yandex.market.tpl.carrier.core.domain.company.requests.CompanyCreateRequest;
import ru.yandex.market.tpl.carrier.core.domain.education.program.Program;
import ru.yandex.market.tpl.carrier.core.domain.education.program.ProgramRepository;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryServiceUtil;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.transport.TestTransportTypeHelper;
import ru.yandex.market.tpl.carrier.core.domain.transport.TestTransportTypeHelper.TransportTypeGenerateParam;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserDraftManager;
import ru.yandex.market.tpl.carrier.core.domain.user.UserQueryService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRole;
import ru.yandex.market.tpl.carrier.core.domain.user.UserSource;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.NewDriverData;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.data.PassportData;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.OwnershipType;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportSource;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportType;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportTypeSource;
import ru.yandex.market.tpl.carrier.core.domain.user.util.UsersUtil;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.mock.EmployerApiEmulator;

import static ru.yandex.market.tpl.carrier.core.domain.company.Company.DEFAULT_CAMPAIGN_ID;
import static ru.yandex.market.tpl.carrier.core.domain.company.Company.DEFAULT_COMPANY_NAME;

@RequiredArgsConstructor
public class TestUserHelper {

    public static final String DEFAULT_TRANSPORT_NAME = "Машинка №1";
    public static final String DEFAULT_TRANSPORT_NUMBER = "в921сн";

    private final CompanyCommandService companyCommandService;
    private final CompanyRepository companyRepository;
    private final DsRepository dsRepository;
    private final TransportTypeRepository transportTypeRepository;
    private final TransportRepository transportRepository;
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final UserShiftRepository userShiftRepository;

    private final TestTransportTypeHelper transportTypeHelper;

    private final UserShiftCommandService commandService;
    private final UserShiftCommandDataHelper helper;
    private final UserDraftManager userDraftManager;

    private final TransactionTemplate transactionTemplate;
    private final EmployerApiEmulator employerApiEmulator;

    private final ProgramRepository programRepository;

    public Company findOrCreateCompany(String name) {
        return companyRepository.findCompanyByName(name)
                .orElseGet(() -> createCompany(
                        Set.of(deliveryService(DeliveryService.DEFAULT_DS_ID)),
                        employerApiEmulator.nextCabinetMbiIdTest(),
                        name,
                        "I am created at " + Instant.now()));
    }

    public Company findOrCreateCompany(CompanyGenerateParam param) {
        return companyRepository.findCompanyByName(param.companyName)
                .orElseGet(() -> createCompany(
                        param.deliveryServiceIds.stream().map(this::deliveryService).collect(Collectors.toSet()),
                        param.campaignId,
                        param.companyName,
                        param.login,
                        false,
                        param.getContractId(),
                        param.getContractDate()));
    }

    public Company createCompany(Set<DeliveryService> deliveryServices,
                                 Long campaignId, String name, String login) {
        return createCompany(deliveryServices, campaignId, name, login, false);
    }

    private Company createCompany(
            Set<DeliveryService> deliveryServices,
            Long campaignId,
            String name,
            String login,
            boolean isSuperCompany
    ) {
        return createCompany(
                deliveryServices,
                campaignId,
                name,
                login,
                isSuperCompany,
                null,
                null
        );
    }

    private Company createCompany(
            Set<DeliveryService> deliveryServices,
            Long campaignId,
            String name,
            String login,
            boolean isSuperCompany,
            String contractId,
            LocalDate contractDate
    ) {
        Company company = companyCommandService.create(
                CompanyCreateRequest.builder()
                        .campaignId(campaignId)
                        .name(name)
                        .login(login == null ? "stasiyan-yndx@yandex.ru" : login)
                        .phoneNumber("89175704071")
                        .taxpayerNumber("12345678901")
                        .juridicalAddress("г. Москва, Каширское шоссе, д. 31")
                        .naturalAddress("г. Москва, ул. Льва Толстого, д. 16")
                        .deactivated(false)
                        .isSuperCompany(isSuperCompany)
                        .type(CompanyType.SUPPLY)
                        .contractId(contractId)
                        .contractDate(contractDate)
                        .build()
        );
        deliveryServices.forEach(ds -> transactionTemplate.execute(tc -> {
            DeliveryService found = dsRepository.findByIdOrThrow(ds.getId());
            found.getCompanies().add(company);
            return null;
        }));
        return company;
    }


    public UserShift createEmptyShift(User user, Transport transport, LocalDate date) {
        UserShift userShift = commandService.createUserShift(UserShiftCommand.Create.builder()
                .runId(-1)
                .startDateTime(date.atTime(9, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                .endDateTime(date.atTime(18, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                .build()
        );
        commandService.assignUser(new UserShiftCommand.AssignUser(userShift.getId(), user.getId()));
        commandService.assignTransport(new UserShiftCommand.AssignTransport(userShift.getId(), transport.getId()));
        return userShift;
    }

    public DeliveryService deliveryService(long id) {
        return dsRepository.findById(id)
                .orElseGet(() -> dsRepository.save(DeliveryServiceUtil.deliveryService(id)));
    }

    public DeliveryService deliveryService(long id, Set<Company> companies) {
        return dsRepository.findById(id)
                .orElseGet(() -> dsRepository.save(DeliveryServiceUtil.deliveryService(id, companies)));
    }

    public void openShift(User user, long userShiftId) {
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
    }

    public void estimateTime(long userShiftId, Instant estimatedTime) {
        commandService.updateEstimatedTime(new UserShiftCommand.UpdateEstimatedTime(userShiftId, estimatedTime));
    }

    public void arriveAtRoutePoint(UserShift userShift, long routePointId) {
        var user = userShift.getUser();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        routePointId,
                        helper.getArrivalDto()
                ));
    }

    public void arriveAtRoutePoint(RoutePoint routePoint) {
        var userShift = routePoint.getUserShift();
        var user = userShift.getUser();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        routePoint.getId(),
                        helper.getArrivalDto()
                ));
    }

    public void finishCollectDropships(RoutePoint routePoint) {
        arriveAtRoutePoint(routePoint);
        var userShift = routePoint.getUserShift();
        var user = userShift.getUser();

        routePoint.streamCollectDropshipTasks().forEach(cdt ->
                commandService.collectDropships(user, new UserShiftCommand.CollectDropships(
                        userShift.getId(),
                        routePoint.getId(),
                        cdt.getId()
                ))
        );

    }

    @Transactional
    public void finishFullReturnAtEnd(Long userShiftId) {
        UserShift userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        finishFullReturnAtEnd(userShift);
    }

    public void finishFullReturnAtEnd(UserShift userShift) {
        var routePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        var task = routePoint.streamReturnTasks().findFirst().orElseThrow();
        arriveAtRoutePoint(routePoint);

        commandService.finishReturnTask(
                userShift.getUser(),
                new UserShiftCommand.FinishReturnTask(userShift.getId(), routePoint.getId(), task.getId())
        );
    }

    @Transactional
    public void finishDriverChange(UserShift userShift) {
        var routePoint =
                userShift.streamRoutePoints().filter(rp -> rp.getDriverChangeTask() != null).findAny().orElseThrow(
                        () -> new TplIllegalArgumentException("No driver change task for shift " + userShift)
                );
        var task = routePoint.getDriverChangeTask();
        arriveAtRoutePoint(routePoint);
        commandService.finishDriverChangeTask(
                userShift.getUser(),
                new UserShiftCommand.FinishDriverChangeTask(userShift.getId(), routePoint.getId(), task.getId())
        );
    }

    public void finishUserShift(Long userShiftId) {
        commandService.finishUserShift(new UserShiftCommand.Finish(userShiftId));
    }

    public void finishUserShift(UserShift userShift) {
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
    }


    /**
     * USER CREATION BELOW
     */

    private Optional<User> findByUid(long uid) {
        return userQueryService.findByUid(uid);
    }


    private User createUser(long uid, String companyName, String phone,
                            String lastName, String firstName, String patronymic,
                            UserSource source
    ) {
        return transactionTemplate.execute(ts -> {
            Company company = findOrCreateCompany(companyName);

            return userCommandService.create(new UserCommand.CreateDriver(
                    NewDriverData.builder()
                            .uid(uid)
                            .dsmId(null)
                            .role(UserRole.COURIER)
                            .phone(phone)
                            .email("")
                            .firstName(firstName)
                            .lastName(lastName)
                            .patronymic(patronymic)
                            .company(company)
                            .source(source)
                            .build()
            ));
        });
    }

    private User createUser(long uid, String companyName, String phone, UserSource source) {
        return createUser(uid, companyName, phone, UserUtil.LAST_NAME, UserUtil.FIRST_NAME, null, source);
    }

    private User createAndPromoteUser(long uid, String companyName, String phone, UserSource source) {
        User user = createUser(uid, companyName, phone, source);
        return userDraftManager.promoteUserDraft(uid, user);
    }

    private User createAndPromoteUser(long uid, String companyName, String phone,
                                      String lastName, String firstName, String patronymic,
                                      UserSource source
    ) {
        User user = createUser(uid, companyName, phone, lastName, firstName, patronymic, source);
        return userDraftManager.promoteUserDraft(uid, user);
    }

    private User createAndPromoteUser(long uid, String companyName, String phone, String taxiId, UserSource source) {
        User user = createUser(uid, companyName, phone, source);

        var yaProdId = UsersUtil.taxiIdToYaProId(taxiId);
        userCommandService.setYaProId(new UserCommand.SetYaProId(user.getId(), yaProdId));

        return userDraftManager.promoteUserDraft(uid, user);
    }

    public User findOrCreateUser(long uid) {
        return findByUid(uid).orElseGet(
                () -> createAndPromoteUser(uid, DEFAULT_COMPANY_NAME, UserUtil.PHONE, UserSource.CARRIER)
        );
    }

    public User findOrCreateUser(long uid, String companyName, String phone,
                                 String lastName, String firstName, String patronymic,
                                 UserSource source
    ) {
        return findByUid(uid).orElseGet(
                () -> createAndPromoteUser(
                        uid, companyName, phone,
                        lastName, firstName, patronymic,
                        source)
        );
    }

    public User findOrCreateUser(long uid, String phone, String lastName, String firstName, String patronymic) {
        return findOrCreateUser(uid, DEFAULT_COMPANY_NAME, phone, lastName, firstName, patronymic, UserSource.CARRIER);
    }

    public User findOrCreateUser(long uid, String lastName, String firstName, String patronymic) {
       return findOrCreateUser(uid, UserUtil.PHONE, lastName, firstName, patronymic);
    }

    public User findOrCreateUser(long uid, String companyName) {
        return findByUid(uid).orElseGet(() -> createAndPromoteUser(uid, companyName, UserUtil.PHONE, UserSource.CARRIER)
        );
    }


    public User findOrCreateUser(long uid, String companyName, String phone) {
        return findByUid(uid).orElseGet(() -> createAndPromoteUser(uid, companyName, phone, UserSource.CARRIER)
        );
    }

    public User findOrCreateUser(long uid, String companyName, String phone, UserSource source) {
        return findByUid(uid).orElseGet(() -> createAndPromoteUser(uid, companyName, phone, source)
        );
    }

    public User findOrCreateUser(String taxiId, long uid) {
        return findOrCreateUser(taxiId, uid, UserUtil.PHONE);
    }

    public User findOrCreateUser(String taxiId, long uid, String phone) {
        return findByUid(uid).orElseGet(
                () -> createAndPromoteUser(uid, DEFAULT_COMPANY_NAME, phone, taxiId, UserSource.CARRIER)
        );
    }

    @Transactional
    public User addPassport(long uid, PassportData passport) {
        User user = findByUid(uid).orElseThrow();

        var fioAndPhone = userQueryService.findUserFioAndPhone(user.getId());

        var updateUserDataBuilder = NewDriverData.builder()
                .role(user.getRole())
                .uid(user.getUid())
                .email(user.getEmail())
                .lastName(user.getLastName())
                .firstName(user.getFirstName())
                .patronymic(user.getPatronymic())
                .telegram(user.getTelegram())
                .phone(fioAndPhone.getPhone())
                .source(user.getSource())
                .blackListed(user.isBlackListed())
                .passport(passport);


        Iterator<Company> iterator = user.getCompanies().iterator();
        if (iterator.hasNext()) {
            updateUserDataBuilder.company(iterator.next());
        }

        return userCommandService.update(
                UserCommand.UpdateDriver.builder()
                        .userId(user.getId())
                        .data(updateUserDataBuilder.build())
                        .build()
        );
    }

    public Transport findOrCreateTransport() {
        return findOrCreateTransport(DEFAULT_TRANSPORT_NAME, DEFAULT_COMPANY_NAME);
    }

    public Transport findOrCreateTransport(String transportName, String companyName) {
        Company company = findOrCreateCompany(companyName);
        return transportRepository.findByNameAndCompanyId(transportName, company.getId())
            .orElseGet(() -> createTransport(transportName, company, DEFAULT_TRANSPORT_NUMBER));
    }

    public Transport createTransport(String transportName, Company company, String number) {
        TransportType transportType = transportTypeRepository.findByNameAndCompanyId(transportName, company.getId())
            .orElseGet(() -> transportTypeRepository.save(
                new TransportType(
                    transportName,
                    BigDecimal.TEN,
                    100,
                    100,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    company,
                    TransportTypeSource.CARRIER
                )
        ));

        Transport transport = new Transport();
        transport.setCompany(company);
        transport.setTransportType(transportType);
        transport.setName(transportName);
        transport.setNumber(number);
        transport.setBrand("ВАЗ");
        transport.setModel("2114");
        transport.setSource(TransportSource.CARRIER);
        transport.setOwnershipType(OwnershipType.PROPRIETARY);
        return transportRepository.save(transport);
    }

    public Transport findOrCreateTransport(String transportName,
                                           TransportTypeGenerateParam transportTypeGenerateParam, Company company) {
        TransportType transportType = transportTypeHelper.createTransportType(transportTypeGenerateParam);

        return transportRepository.findByNameAndCompanyId(transportName, company.getId()).orElseGet(() -> {
            Transport transport = new Transport();
            transport.setCompany(company);
            transport.setTransportType(transportType);
            transport.setName(transportName);
            transport.setNumber(DEFAULT_TRANSPORT_NUMBER);
            transport.setBrand("ВАЗ");
            transport.setSource(TransportSource.CARRIER);
            transport.setModel("2114");
            return transportRepository.save(transport);
        });
    }

    public Program findOrCreateProgram(String uuid) {
        return programRepository.findByUuid(uuid).orElseGet(
                () -> programRepository.save(
                        new Program(null, uuid, "Program " + uuid, "Description " + uuid)
                )
        );
    }

    @Getter
    @Builder(toBuilder = true)
    public static class CompanyGenerateParam {
        @Builder.Default
        private final String companyName = DEFAULT_COMPANY_NAME;

        @Builder.Default
        private final Set<Long> deliveryServiceIds = Set.of(DeliveryService.DEFAULT_DS_ID);

        @Builder.Default
        private final long campaignId = DEFAULT_CAMPAIGN_ID;

        @Builder.Default
        private final String login = null;

        @Builder.Default
        private final String contractId = null;

        @Builder.Default
        private final LocalDate contractDate = null;
    }

    @Getter
    @Builder(toBuilder = true)
    public static class UserGenerateParam {

        public static final Long DEFAULT_USER_ID = 1000L;

        @Builder.Default
        private final Long userId = DEFAULT_USER_ID;

        @NonNull
        private final LocalDate workdate;

        @Builder.Default
        private final RelativeTimeInterval localTimeInterval = RelativeTimeInterval.valueOf("09:00-19:00");

        @Builder.Default
        private final CompanyGenerateParam company = CompanyGenerateParam.builder().build();

    }

}
