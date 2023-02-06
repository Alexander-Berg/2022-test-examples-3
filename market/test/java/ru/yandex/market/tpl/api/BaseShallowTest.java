package ru.yandex.market.tpl.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.service.OrderPickupValidator;
import ru.yandex.market.tpl.api.service.RequestInfoService;
import ru.yandex.market.tpl.api.service.ScanRequestMapper;
import ru.yandex.market.tpl.common.util.SynchronizedData;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.OAuthUser;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.advice.PersonalDataResponseBodyAdvice;
import ru.yandex.market.tpl.core.domain.clientreturn.mapper.ClientReturnOrderDtoMapper;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.order.PhotoRepository;
import ru.yandex.market.tpl.core.domain.task.TaskErrorRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.external.boxbot.LockerApi;
import ru.yandex.market.tpl.core.query.order.mapper.OrderBatchMapper;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.query.usershift.mapper.DestinationMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.FlowTaskDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.PhotoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.ScanTaskDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.GenericTaskDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.ReturnTaskDtoMapper;
import ru.yandex.market.tpl.core.service.order.OrderFeaturesResolver;
import ru.yandex.market.tpl.core.service.order.TransferTypeResolver;
import ru.yandex.market.tpl.core.service.order.movement.MovementPhotoValidator;
import ru.yandex.market.tpl.core.service.user.UserAuthService;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.company.Company.DEFAULT_COMPANY_NAME;

/**
 * @author kukabara
 */
public abstract class BaseShallowTest {

    protected static final Long UID = 1L;
    protected static final long USER_ID = 1L;
    protected static final String AUTH_HEADER_VALUE = "OAuth uid-" + UID;
    @MockBean
    protected TvmClient tvmClient;
    @MockBean
    protected BlackboxClient blackboxClient;
    @MockBean
    private SynchronizedData<String, User> usersByYaProId;
    @MockBean
    protected UserRepository userRepository;
    @MockBean
    protected ScanRequestMapper scanRequestMapper;
    @MockBean
    protected OrderPickupValidator orderPickupValidator;
    @MockBean
    protected MovementPhotoValidator movementPhotoValidator;
    @MockBean
    protected RequestInfoService requestInfoService;
    @MockBean
    protected UserShiftQueryService queryService;
    @MockBean
    protected FlowTaskDtoMapper flowTaskDtoMapper;
    @Autowired
    protected MockMvc mockMvc;
    @MockBean
    private PhotoRepository photoRepository;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @MockBean
    private TransactionTemplate transactionTemplate;
    @MockBean
    private PhotoMapper photoMapper;
    @MockBean
    private TaskErrorRepository taskErrorRepository;
    @MockBean
    private TransferTypeResolver transferTypeResolver;
    @MockBean
    private LockerApi lockerApi;
    @SpyBean
    private OrderBatchMapper orderBatchMapper;
    @MockBean
    private ReturnTaskDtoMapper returnTaskDtoMapper;
    @MockBean
    private ScanTaskDtoMapper scanTaskDtoMapper;
    @MockBean
    private GenericTaskDtoMapper genericTaskDtoMapper;
    @MockBean
    private UserAuthService userAuthService;
    @MockBean
    private DestinationMapper destinationMapper;
    @MockBean
    private ClientReturnOrderDtoMapper clientReturnOrderDtoMapper;
    @MockBean
    private PersonalDataResponseBodyAdvice personalDataResponseBodyAdvice;
    @MockBean
    private OrderFeaturesResolver orderFeaturesResolver;

    @BeforeEach
    void setUp() {
        when(blackboxClient.oauth(anyString(), anyString())).thenReturn(new OAuthUser(UID, Set.of(), null, List.of()));
        User user = UserUtil.createUserWithoutSchedule(
                UID,
                TransportType.builder()
                        .name("Машинка")
                        .capacity(BigDecimal.valueOf(10.0))
                        .build(),
                Company.builder()
                        .name(DEFAULT_COMPANY_NAME)
                        .login("test@yandex.ru")
                        .taxpayerNumber("01234567890")
                        .juridicalAddress("г. Москва")
                        .phoneNumber("88005553535")
                        .build());
        UserUtil.setId(user, USER_ID);
        when(userAuthService.findByUid(anyLong())).thenReturn(Optional.of(user));
    }

}
