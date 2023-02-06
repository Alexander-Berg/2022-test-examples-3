package ru.yandex.autotests.directapi.test.units;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.autotests.direct.db.steps.ClientBrandsSteps;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.db.steps.UsersSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static ru.yandex.autotests.directapi.rules.ApiSteps.USE_OPERATOR_UNITS_FALSE;
import static ru.yandex.autotests.directapi.test.units.UnitsUtils.clearSpendUnits;
import static ru.yandex.autotests.directapi.test.units.UnitsUtils.getUid;
import static ru.yandex.autotests.directapi.test.units.UnitsUtils.getUnitsBalance;
import static ru.yandex.autotests.directapi.test.units.UnitsUtils.setManualUnitsLimit;
import static ru.yandex.autotests.directapi.test.units.UnitsUtils.spendAllUnits;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTestData.BRAND_CHIEF_LOGIN;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTestData.BRAND_MEMBER_LOGIN;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTestData.DEFAULT_UNITS_LIMIT;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTestData.INACTIVE_UNITS_LIMIT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class UnitsWithdrawalTesterImpl implements UnitsWithdrawalTester {

    private static final Logger log = LoggerFactory.getLogger(UnitsWithdrawalTesterImpl.class);

    private static UnitsLock lock = UnitsLock.INSTANCE;

    static final int DEFAULT_LOCK_TIMEOUT_MINUTES = 4;

    private final int lockTimeoutMinutes;

    private final ApiSteps api;

    private final Map<String, Long> unitsBefore;
    private final Map<String, Long> unitsAfter;

    private final boolean useManualUnits;

    public UnitsWithdrawalTesterImpl(ApiSteps api) {
        this(api, false);
    }

    public UnitsWithdrawalTesterImpl(ApiSteps api, boolean useManualUnits) {
        this(api, useManualUnits, DEFAULT_LOCK_TIMEOUT_MINUTES);
    }

    public UnitsWithdrawalTesterImpl(ApiSteps api, boolean useManualUnits, int lockTimeoutMinutes) {
        this.api = api;

        unitsBefore = new HashMap<>();
        unitsAfter = new HashMap<>();
        this.useManualUnits = useManualUnits;

        this.lockTimeoutMinutes = lockTimeoutMinutes;
    }

    public static UnitsWithdrawalTesterBuilder builder() {
        return new UnitsWithdrawalTesterBuilder();
    }

    /** @inheritDoc */
    @Override
    public void init() {
        lock.acquire(lockTimeoutMinutes);

        Long brandChiefClientId = getClientId(BRAND_CHIEF_LOGIN);
        Long brandMemberClientId = getClientId(BRAND_MEMBER_LOGIN);

        brandsSteps(BRAND_CHIEF_LOGIN).deleteBrand(brandChiefClientId);
        brandsSteps(BRAND_CHIEF_LOGIN).unbindClientFromAllBrands(brandChiefClientId);

        brandsSteps(BRAND_CHIEF_LOGIN).createBrand(brandChiefClientId);
        brandsSteps(BRAND_MEMBER_LOGIN).bindToBrand(brandMemberClientId, brandChiefClientId);

        assumeThat("Клиенты объединены брендом",
                brandsSteps(BRAND_MEMBER_LOGIN).getBrand(brandMemberClientId).getBrandClientid(),
                equalTo(brandChiefClientId));

        setUnitsLimit(BRAND_CHIEF_LOGIN, INACTIVE_UNITS_LIMIT);
        setUnitsLimit(BRAND_MEMBER_LOGIN, INACTIVE_UNITS_LIMIT);
    }

    /** @inheritDoc */
    @Override
    public void reset(String operatorLogin, String clientLogin, String useOperatorUnits, int unitsDelta,
            Collection<String> loginsWithoutUnits, Collection<String> loginsToClearUnits) {
        new Reset.Builder(operatorLogin, clientLogin, this::reset)
                .useOperatorUnits(useOperatorUnits)
                .unitsDiff(unitsDelta)
                .loginsWithoutUnits(loginsWithoutUnits)
                .loginsToClearUnits(loginsToClearUnits)
                .execute();
    }

    /** @inheritDoc */
    @Override
    public ResetBuilder resetBuilder(String operatorLogin, String clientLogin) {
        return new Reset.Builder(operatorLogin, clientLogin, this::reset);
    }

    private void reset(Reset r) {
        r.loginsToClearUnits.forEach(login -> clearSpendUnits(api, login));

        api
                .as(r.operator)
                .clientLogin(r.client)
                .useOperatorUnits(r.useOperatorUnits);

        setUnitsLimit(BRAND_MEMBER_LOGIN, r.brandMemberLimit);
        setUnitsLimit(BRAND_CHIEF_LOGIN, r.brandChiefLimit);

        assumeThat("Лимит баллов для неглавного клиента бренда установлен",
                getUnitsLimit(BRAND_MEMBER_LOGIN), equalTo(r.brandMemberLimit));
        assumeThat("Лимит баллов для главного клиента бренда установлен",
                getUnitsLimit(BRAND_CHIEF_LOGIN), equalTo(r.brandChiefLimit));

        r.loginsWithoutUnits.forEach(login -> setUnitsLimit(login, INACTIVE_UNITS_LIMIT));
        r.loginsWithoutUnits.forEach(login -> spendAllUnits(api, login));

        r.loginsWithoutUnits.forEach(login ->
                assumeThat("Лимит баллов для клиентов без баллов установлен неверно",
                        getUnitsLimit(login), equalTo(INACTIVE_UNITS_LIMIT)));
        r.loginsWithoutUnits.forEach(login ->
                assumeThat("У клиентов без баллов не должно быть баллов",
                        getUnitsBalance(api, login), equalTo(0L)));

        unitsBefore.clear();
        unitsAfter.clear();
    }

    /** @inheritDoc */
    @Override
    public <R> void testUnitsWithdrawal(Supplier<R> serviceCall, Consumer<? super R> resultChecker,
            Collection<String> expectedUnitsWithdrawLogins,
            Collection<String> expectedUnitsKeepLogins)
    {
        Runnable callAndCheck = () -> resultChecker.accept(serviceCall.get());

        testUnitsWithdrawal(callAndCheck, expectedUnitsWithdrawLogins, expectedUnitsKeepLogins);
    }

    /** @inheritDoc */
    @Override
    public void testUnitsWithdrawal(Supplier<String> serviceCall,
            Collection<String> expectedUnitsWithdrawLogins,
            Collection<String> expectedUnitsKeepLogins)
    {
        Runnable callAndCheck = () -> expectedUnitsWithdrawLogins.contains(serviceCall.get());

        testUnitsWithdrawal(callAndCheck, expectedUnitsWithdrawLogins, expectedUnitsKeepLogins);
    }

    /** @inheritDoc */
    @Override
    public void testUnitsWithdrawal(Function<String, String> serviceCallReturningUsedUnitsLoginHeader,
            Collection<String> expectedUnitsWithdrawLogins,
            Collection<String> expectedUnitsKeepLogins)
    {
        String login = api.login();
        Runnable callWithHeaderCheck = () -> serviceCallReturningUsedUnitsLoginHeader.apply(login);

        testUnitsWithdrawal(callWithHeaderCheck, expectedUnitsWithdrawLogins, expectedUnitsKeepLogins);
    }

    /** @inheritDoc */
    @Override
    public void testUnitsWithdrawal(Runnable serviceCall,
            Collection<String> expectedUnitsWithdrawLogins,
            Collection<String> expectedUnitsKeepLogins)
    {
        expectedUnitsWithdrawLogins.forEach(login -> unitsBefore.put(login, getUnitsBalance(api, login)));
        expectedUnitsKeepLogins.forEach(login -> unitsBefore.put(login, getUnitsBalance(api, login)));

        serviceCall.run();

        unitsBefore.keySet().forEach(login -> unitsAfter.put(login, getUnitsBalance(api, login)));
        unitsBefore.keySet().forEach(this::printUnitsChange);

        expectedUnitsWithdrawLogins.forEach(
                login -> assertThat(String.format("С пользователя %s списаны баллы", login),
                        unitsAfter.get(login), lessThan(unitsBefore.get(login))));

        expectedUnitsKeepLogins.forEach(
                login -> assertThat(String.format("С пользователя %s не списаны баллы", login),
                        unitsAfter.get(login), equalTo(unitsBefore.get(login))));
    }

    /** @inheritDoc */
    @Override
    public void shutdown() {
        lock.release();
    }

    private void printUnitsChange(String login) {
        log.info("\t\tUnits for {}: {} -> {}", login, unitsBefore.get(login), unitsAfter.get(login));
    }

    private Long getUnitsLimit(String login) {
        return useManualUnits ? getManualUnitsLimit(getUid(api, login)) : getAutoUnitsLimit(login);
    }

    private void setUnitsLimit(String login, Long value) {
        if (useManualUnits) {
            setManualUnitsLimit(api, login, value);
            if (value <= INACTIVE_UNITS_LIMIT) {
                setAutoUnitsLimit(login, value);
            }
        } else {
            setAutoUnitsLimit(login, value);
            if (value <= INACTIVE_UNITS_LIMIT) {
                setManualUnitsLimit(api, login, value);
            }
        }
    }

    private Long getManualUnitsLimit(Long uid) {
        return jooqSteps().usersApiOptionsSteps().getUserApiUnitsDailyLimit(uid);
    }

    private Long getAutoUnitsLimit(String login) {
        return jooqSteps().clientsApiOptionsSteps().getUnitsDaily(getClientId(login));
    }

    private void setAutoUnitsLimit(String login, Long value) {
        jooqSteps().useShardForLogin(login).clientsApiOptionsSteps().setUnitsDaily(getClientId(login), value);
    }


    private Long getClientId(String login) {
        return Long.parseLong(api.userSteps.clientFakeSteps().getClientData(login).getClientID());
    }

    private ClientBrandsSteps brandsSteps(String login) {
        return jooqSteps().useShardForLogin(login).clientBrandsSteps();
    }

    private UsersSteps usersSteps(String login) {
        return jooqSteps().useShardForLogin(login).usersSteps();
    }

    private DirectJooqDbSteps jooqSteps() {
        return api.userSteps.getDirectJooqDbSteps();
    }

    private static class Reset {
        final String operator, client;
        final String useOperatorUnits;
        final long brandMemberLimit, brandChiefLimit;
        final Collection<String> loginsWithoutUnits;
        final Collection<String> loginsToClearUnits;

        Reset(Builder b) {
            this.operator = requireNonNull(b.operator);
            this.client = b.client;
            this.useOperatorUnits = b.useOperatorUnits;
            this.brandMemberLimit = b.baseLimit;
            this.brandChiefLimit = brandMemberLimit + b.unitsDiff;
            this.loginsWithoutUnits = b.loginsWithoutUnits;
            this.loginsToClearUnits = b.loginsToClearUnits;
        }

        static class Builder implements UnitsWithdrawalTester.ResetBuilder {
            final String operator, client;
            final Consumer<Reset> exec;
            String useOperatorUnits = USE_OPERATOR_UNITS_FALSE;
            long baseLimit = DEFAULT_UNITS_LIMIT, unitsDiff = 0;
            Collection<String> loginsWithoutUnits = ImmutableSet.of();
            Collection<String> loginsToClearUnits = ImmutableSet.of();

            Builder(String operator, String client, Consumer<Reset> exec) {
                this.operator = operator;
                this.client = client;
                this.exec = requireNonNull(exec);
            }

            public Builder useOperatorUnits(String use) {
                this.useOperatorUnits = use;
                return this;
            }

            public Builder baseLimit(long limit) {
                this.baseLimit = limit;
                return this;
            }

            public Builder unitsDiff(long diff) {
                this.unitsDiff = diff;
                return this;
            }

            public Builder loginsWithoutUnits(Collection<String> loginsWithoutUnits) {
                this.loginsWithoutUnits = requireNonNull(loginsWithoutUnits);
                return this;
            }

            public Builder loginsToClearUnits(Collection<String> loginsToClearUnits) {
                this.loginsToClearUnits = requireNonNull(loginsToClearUnits);
                return this;
            }

            @Override
            public void execute() {
                exec.accept(build());
            }

            private Reset build() {
                return new Reset(this);
            }
        }
    }

}
