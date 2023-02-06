package ru.yandex.calendar.test.generic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.frontend.ews.proxy.MockEwsProxyWrapperFactory;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.inside.passport.PassportUid;

/**
 * @author yashunsky
 */
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(AbstractEwsExportedLoginsTest.RunnerFactory.class)
public abstract class AbstractEwsExportedLoginsTest extends AbstractConfTest {
    @Autowired
    private EwsExportRoutines ewsExportRoutines;

    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private TestManager testManager;

    protected enum EwsUsage {
        YES,
        NO;

        @Override
        public String toString() {
            return this == YES ? "with EWS export" : "without EWS export";
        }
    }

    public final boolean useEwsLogins;
    private ListF<String> ewsLogins;

    @Parameterized.Parameters(name = "{0}")
    public static ListF<Object[]> data() {
        return Cf.list(EwsUsage.NO, EwsUsage.YES).map(a -> new Object[]{a});
    }

    public AbstractEwsExportedLoginsTest(EwsUsage ewsUsage) {
        this.useEwsLogins = ewsUsage == EwsUsage.YES;
        this.ewsLogins = Cf.list();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        new CalendarTestContextManager(AbstractEwsExportedLoginsTest.class).beforeTestClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        new CalendarTestContextManager(AbstractEwsExportedLoginsTest.class).afterTestClass();
    }

    @Before
    @SneakyThrows
    public void setUpAll() {
        TestContextManager testContextManager = new CalendarTestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        super.setUpAll();
    }

    protected void setIsEwserIfNeeded(ListF<TestUserInfo> users) {
        testManager.updateIsEwser(useEwsLogins ? users.toArray(TestUserInfo.class) : new TestUserInfo[0]);
    }

    protected void setIsEwserIfNeeded(TestUserInfo testUserInfo) {
        setIsEwserIfNeeded(Cf.list(testUserInfo));
    }

    public boolean isEwser(TestUserInfo user) {
        return testManager.isEwser(user.getUid());
    }

    public void setIsExportedWithEwsIfNeeded(Event event) {
        EventWithRelations eventWr = eventDbManager.getEventWithRelationsByEvent(event);
        mainEventDao.updateIsExportedWithEwsById(event.getMainEventId(),
                testManager.isEwser(event.getCreatorUid()) && !eventWr.isParkingOrApartmentOccupation());
    }

    public void setMockEwsProxyWrapper() {
        ewsExportRoutines.setEwsProxyWrapperForTest(MockEwsProxyWrapperFactory.getMockEwsProxyWrapperForTest());
    }

    public void setMockEwsProxyWrapper(int randomExchangeIdsCount) {
        ewsExportRoutines.setEwsProxyWrapperForTest(
                MockEwsProxyWrapperFactory.getMockEwsProxyWrapperForTest(randomExchangeIdsCount));
    }

    @After
    public void setRealEwsProxyWrapper() {
        ewsExportRoutines.setEwsProxyWrapperForTest(ewsProxyWrapper);
    }

    public void setMockExchangeIdForEventUser(long eventId, PassportUid uid) {
        EventUser eventUser = new EventUser();
        eventUser.setId(eventUserDao.findEventUserByEventIdAndUid(eventId, uid).get().getId());
        eventUser.setExchangeId("real_exchange_id_for_participant_event_user_will_not_be_set_because_of_mock_ews"
                        + UUID.randomUUID().toString());
        eventUserDao.updateEventUser(eventUser, ActionInfo.webTest());
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WantsEws {
    }

    public static class RunnerFactory implements ParametersRunnerFactory {
        @Override
        public Runner createRunnerForTestWithParameters(TestWithParameters test) throws InitializationError {
            return new Runner(test);
        }
    }

    public static class Runner extends BlockJUnit4ClassRunnerWithParameters {

        private final ListF<Object> parameters;

        public Runner(TestWithParameters test) throws InitializationError {
            super(test);
            parameters = Cf.x(test.getParameters());
        }

        @Override
        protected boolean isIgnored(FrameworkMethod child) {
            return super.isIgnored(child) || parameters.filterByType(EwsUsage.class).exists(EwsUsage.YES::equals)
                    && child.getAnnotation(WantsEws.class) == null
                    && child.getDeclaringClass().getAnnotation(WantsEws.class) == null;
        }
    }
}
