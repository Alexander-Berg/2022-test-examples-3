package ru.yandex.market.ir.uee.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.ir.uee.CommonMapper;
import ru.yandex.market.ir.uee.api.mapper.AccountMapper;
import ru.yandex.market.ir.uee.api.mapper.UserRunMapper;
import ru.yandex.market.ir.uee.repository.AclRepo;
import ru.yandex.market.ir.uee.repository.GenericRepo;
import ru.yandex.market.ir.uee.api.service.PrincipalService;
import ru.yandex.market.ir.uee.api.validator.DQFirewall;
import ru.yandex.market.ir.uee.config.LocalPgInitializer;
import ru.yandex.market.ir.uee.jooq.generated.Tables;
import ru.yandex.market.ir.uee.jooq.generated.tables.daos.AccountDao;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.AccountRecord;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.AclRecord;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.ResourceRecord;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.StaffUserRecord;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.UserRunRecord;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.YtPoolAccountRecord;
import ru.yandex.market.ir.uee.model.AccountReq;
import ru.yandex.market.ir.uee.model.ResourceStatus;
import ru.yandex.market.ir.uee.model.UserRunReq;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.ir.uee.repository.AccountRepo.DEFAULT_ACCOUNT_LOGIN;
import static ru.yandex.market.ir.uee.jooq.generated.Tables.ACL;
import static ru.yandex.market.ir.uee.jooq.generated.Tables.RESOURCE;
import static ru.yandex.market.ir.uee.jooq.generated.Tables.USER_RUN;
import static ru.yandex.market.ir.uee.jooq.generated.Tables.YT_POOL_ACCOUNT;
import static ru.yandex.market.ir.uee.jooq.generated.tables.Account.ACCOUNT;
import static ru.yandex.market.ir.uee.jooq.generated.tables.StaffUser.STAFF_USER;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {LocalPgInitializer.class})
@ComponentScan(basePackages="ru.yandex.market.ir.uee")
@AutoConfigureMockMvc(addFilters = false)
public abstract class BaseTest {
    public static final String DEFAULT_YT_POOL = "default";

    @Autowired
    protected GenericRepo jooq;

    @MockBean
    Blackbox2 blackbox2;

    @MockBean
    Tvm2 tvm2;

    @MockBean
    DQFirewall dqFirewall;

    @SpyBean
    AclRepo aclRepo;

    @Autowired
    protected DSLContext dslContext;

    @Autowired
    PrincipalService principalService;

    @Autowired
    AccountMapper accountMapper;

    @Autowired
    UserRunMapper userRunMapper;

    @SneakyThrows
    protected <T> T readResource(String fileName, Class<T> dataClass) {
        ObjectMapper objectMapper = CommonMapper.OBJECT_MAPPER;
        InputStream resource = BaseTest.class.getResourceAsStream("/" + fileName);
        return objectMapper.readValue(resource, dataClass);
    }

    protected String readFile(String fileName) {
        InputStream resource = BaseTest.class.getResourceAsStream("/" + fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
        return reader.lines().collect(Collectors.joining());
    }

    @SneakyThrows
    protected <T> T extractModelFromMvcResult(MvcResult mvcResult, Class<T> tClass) {
        String contentAsString = mvcResult.getResponse().getContentAsString();
        return new ObjectMapper().readValue(contentAsString, tClass);
    }

    @SneakyThrows
    protected <T> String writeValue(T object) {
        ObjectMapper objectMapper = CommonMapper.OBJECT_MAPPER;
        return objectMapper.writeValueAsString(object);
    }

    protected StaffUserRecord mockStaffUser() {
        StaffUserRecord staffUserRecord = new StaffUserRecord()
                .setId(1)
                .setLogin("test")
                .setEnableNotifications(true);
        return jooq.insertRecord(STAFF_USER, staffUserRecord);
    }

    protected Integer ensureDefaultAccountExistAndReturnId() {
        var record = new AccountDao(dslContext.configuration()).fetchOneByLogin("default");
        if (record == null) {
            AccountRecord accountRecord = new AccountRecord()
                    .setLogin(DEFAULT_ACCOUNT_LOGIN)
                    .setName(DEFAULT_ACCOUNT_LOGIN)
                    .setQuota(JSONB.jsonb("[]"))
                    .setTtlDays(7);
            return jooq.insertRecord(ACCOUNT, accountRecord).getId();
        }
        return record.getId();
    }

    protected void grantAccessToYtPool(Integer accountId, String ytPool) {
        YtPoolAccountRecord ytPoolAccountRecord = new YtPoolAccountRecord()
                .setAccountId(accountId)
                .setYtPool(ytPool);
        jooq.insertRecord(YT_POOL_ACCOUNT, ytPoolAccountRecord);
    }

    protected AclRecord storeAcl(Integer accountId, Integer userId) {
        AclRecord aclRecord = new AclRecord()
                .setAccountId(accountId)
                .setUserId(userId);
        return jooq.insertRecord(ACL, aclRecord);
    }

    @SafeVarargs
    protected final AccountRecord storeAccount(Consumer<AccountRecord>... consumer) {
        var accountReq = readResource("accountReq.json", AccountReq.class);
        var source = accountMapper.toAccountRecord(accountReq);
        Arrays.stream(consumer).forEach(c -> c.accept(source));
        var accountRecord = jooq.insertRecord(ACCOUNT, source);
        storeAcl(accountRecord.getId(), principalService.getPrincipalUserId());
        return accountRecord;
    }

    @SafeVarargs
    protected final AccountRecord storeAccountWithoutAcl(Consumer<AccountRecord>... consumer) {
        var accountReq = readResource("accountReq.json", AccountReq.class);
        var accountRecord = accountMapper.toAccountRecord(accountReq);
        Arrays.stream(consumer).forEach(c -> c.accept(accountRecord));
        return jooq.insertRecord(ACCOUNT, accountRecord);
    }

    @SafeVarargs
    protected final UserRunRecord storeUserRun(Consumer<UserRunRecord>... consumers) {
        var taskReq = readResource("userRunReq.json", UserRunReq.class);
        var input = userRunMapper.toResourceRecord(taskReq.getInput());
        var output = new ResourceRecord(null, taskReq.getOutputType().toString(), null,
                ResourceStatus.DRAFT.toString(), null, false);
        input = jooq.insertRecord(RESOURCE, input);
        output = jooq.insertRecord(RESOURCE, output);
        var userRunRecord = userRunMapper.toUserRunRecord(taskReq, input, output);
        userRunRecord.setAuthorId(principalService.getPrincipalUserId());
        Arrays.stream(consumers).forEach(c -> c.accept(userRunRecord));
        return jooq.insertRecord(USER_RUN, userRunRecord);
    }

    @Before
    public void prepare() {
        cleanUp();
        ensureDefaultAccountExistAndReturnId();
        Mockito.doAnswer(
                invocation -> {
                    var accountId = (Integer) invocation.getArgument(0);
                    var accountRecord = jooq.fetchById(ACCOUNT, ACCOUNT.ID, accountId);
                    if (accountRecord.getLogin().equals(DEFAULT_ACCOUNT_LOGIN)) {
                        return true;
                    }
                    return invocation.callRealMethod();
                }
        ).when(aclRepo).isAclExist(any(), any());
    }

    @After
    public void cleanUp() {
        Arrays.stream(Tables.class.getFields()).forEach(t -> {
            try {
                dslContext.truncate((Table) t.get(null)).cascade().execute();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
