package ru.yandex.market.crm.platform.reader.puid.passport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.platform.blackbox.PassportProfile;
import ru.yandex.market.crm.platform.blackbox.PassportProfileProvider;
import ru.yandex.market.crm.platform.models.PuidToEmail;
import ru.yandex.market.crm.platform.reader.passport.PassportEmailMatchService;
import ru.yandex.market.crm.platform.reader.services.passport.PassportEmailResolvingSubtasksDAO;
import ru.yandex.market.crm.platform.reader.test.AbstractServiceTest;
import ru.yandex.market.crm.platform.reader.test.FactsServiceStub;
import ru.yandex.market.crm.platform.services.passport.PassportEmailResolvingTaskDAO;
import ru.yandex.market.crm.platform.services.passport.domain.ResolvingTask;
import ru.yandex.market.crm.platform.services.passport.domain.TaskStatus;
import ru.yandex.market.crm.platform.yt.YtFolders;
import ru.yandex.market.mcrm.lock.LockService;
import ru.yandex.market.mcrm.tx.TxService;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PassportEmailMatchServiceTest extends AbstractServiceTest {

    @Inject
    private PassportEmailResolvingTaskDAO passportEmailResolvingTaskDAO;

    @Inject
    private PassportEmailResolvingSubtasksDAO passportEmailResolvingSubtasksDAO;

    @Inject
    private TxService txService;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private Yt yt;

    @Inject
    private YtClient ytClient;

    @Inject
    private LockService lockService;

    private final FactsServiceStub factsServiceStub = new FactsServiceStub();
    private final PassportProfileProvider passportProfileProvider = mock(PassportProfileProvider.class);

    private PassportEmailMatchService service;

    private static final String FACT_NAME = "PuidToEmail";
    private static final String EMAIL = "email@test.com";
    private static final Logger LOG = LoggerFactory.getLogger(PassportEmailMatchServiceTest.class);

    @Before
    public void setUp() {
        service = new PassportEmailMatchService(
                ytClient,
                lockService,
                factsServiceStub,
                passportEmailResolvingTaskDAO,
                passportEmailResolvingSubtasksDAO,
                passportProfileProvider,
                txService
        );

        yt.cypress().create(
                new CreateNode(getPuidsInputTable(), ObjectType.Table)
                        .setIgnoreExisting(true)
                        .setRecursive(true)
        );
    }

    @After
    public void tearDown() throws InterruptedException {
        yt.cypress().remove(getPuidsInputTable());
        yt.cypress().remove(getPuidsOutputTable());

        service.stop();
        LOG.info("Service stoped");
    }

    @Test
    public void smoke() {
        var taskId = insertTask();
        service.start();
        LOG.info("Service started");

        waitTaskCompletion(taskId);
        verify(passportProfileProvider, times(0)).getPassportProfiles(anyList());
    }

    @Test
    public void testBlackboxCantFindEmail() {
        var puidsTableRows = Arrays.asList(1L, 2L, 3L);
        prepareInput(puidsTableRows);

        var passportProfiles = Arrays.asList(
                new PassportProfile(1L, EMAIL),
                new PassportProfile(2L, null),
                new PassportProfile(3L, "")
        );

        when(passportProfileProvider.getPassportProfiles(anyList()))
                .thenReturn(passportProfiles);

        var taskId = insertTask();
        service.start();

        waitTaskCompletion(taskId);

        assertOutput(List.of(
                new PassportProfile(1L, EMAIL)
        ));

        List<PuidToEmail> savedFacts = factsServiceStub.get(FACT_NAME).stream()
                .map(x -> (PuidToEmail) (x))
                .collect(Collectors.toList());

        assertSavedFacts(passportProfiles, savedFacts);
    }

    private void assertSavedFacts(List<PassportProfile> passportProfiles, List<PuidToEmail> savedFacts) {
        assertEquals(3, savedFacts.size());

        List<Long> savedFactPuids = savedFacts.stream()
                .map(x -> x.getUid().getIntValue())
                .collect(Collectors.toList());
        Set<Long> passportPuidsSet = passportProfiles.stream().map(PassportProfile::getUid).collect(Collectors.toSet());
        assertTrue(passportPuidsSet.containsAll(savedFactPuids));

        assertEquals(2, savedFacts.stream().filter(PuidToEmail::getEmailNotFound).count());

        Optional<PuidToEmail> savedEmail = savedFacts.stream()
                .filter(puidToEmail -> !puidToEmail.getEmailNotFound())
                .findFirst();
        assertTrue(savedEmail.isPresent());
        assertEquals(EMAIL, savedEmail.get().getEmail());
    }

    private void assertOutput(List<PassportProfile> passportProfiles) {
        var profilesMap = new HashMap<>();
        for (PassportProfile passportProfile : passportProfiles) {
            profilesMap.put(passportProfile.getUid(), passportProfile.getEmail());
        }

        yt.tables().read(getPuidsOutputTable(), YTableEntryTypes.YSON, (node) -> {
            var puid = node.getLong("puid");
            assertTrue(profilesMap.containsKey(puid));

            var email = node.getStringO("email");
            email.ifPresentOrElse(
                    e -> assertEquals(profilesMap.get(puid), e),
                    () -> assertNull(profilesMap.get(puid))
            );
        });
    }

    private void waitTaskCompletion(long taskId) {
        while (true) {
            var task = passportEmailResolvingTaskDAO.getTask(taskId);

            assertNotEquals(TaskStatus.ERROR, task.getStatus());
            if (TaskStatus.FINISHED == task.getStatus()) {
                return;
            }
        }
    }

    private void prepareInput(List<Long> puidsTableRows) {
        List<YTreeMapNode> yTreeMapNodeList = puidsTableRows.stream()
                .map(puid -> new YTreeBuilder()
                        .beginMap()
                        .key("puid").value(puid)
                        .buildMap()
                ).collect(Collectors.toList());

        yt.tables().write(getPuidsInputTable(), YTableEntryTypes.YSON, yTreeMapNodeList);
    }

    private YPath getPuidsInputTable() {
        return ytFolders.getHome().child("puids_input");
    }

    private YPath getPuidsOutputTable() {
        return ytFolders.getHome().child("puids_output");
    }

    private long insertTask() {
        var task = new ResolvingTask();
        task.setInputTable(getPuidsInputTable().toString());
        task.setOutputTable(getPuidsOutputTable().toString());
        task.setStatus(TaskStatus.PENDING);

        return passportEmailResolvingTaskDAO.insertTask(task).getId();
    }
}
