package ru.yandex.autotests.innerpochta.b2b;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.model.MultipleFailureException;
import ru.yandex.autotests.innerpochta.beans.mdoby.Flag;
import ru.yandex.autotests.innerpochta.wmi.core.base.props.TvmProperties;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Headers;
import ru.yandex.autotests.innerpochta.wmi.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FoldersObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.rules.*;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.autotests.lib.junit.rules.retry.RetryRule;
import ru.yandex.autotests.testpers.ssh.SSHAuthRule;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.IGNORE_HIGHLIGHT;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.OUTPUT_AS_CDATA;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.SHOW_CONTENT_META;
import static ru.yandex.autotests.innerpochta.wmi.core.Common.toParameterized;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MbodyApi.apiMbody;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders.folders;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder.messagesByFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule.xRequestIdRule;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;

public class MessageB2BTestBase {
    @ClassRule
    public static SSHAuthRule sshAuthRule = sshOn(URI.create(props().mbodyUri()), props().getRobotGerritWebmailTeamSshKey());

    private static HttpClientManagerRule authClient = auth().with("ZooNew").login();

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .or()
            .ifException(AssertionError.class)
            .every(1, TimeUnit.SECONDS).times(1);

    protected final Logger logger = LogManager.getLogger(this.getClass());


    protected static final int COUNT = props().b2bChunk();
    private static final Flag[] FLAGS = {OUTPUT_AS_CDATA, SHOW_CONTENT_META, IGNORE_HIGHLIGHT};
    protected String mid;

    MessageB2BTestBase(String mid) {
        this.mid = mid;
    }

    private String getMbodyMessage(String host, String mid, Flag flag) {
        return apiMbody(authClient.account().userTicket(), host).message()
                .withMid(mid)
                .withUid(authClient.account().uid())
                .withFlags(flag.toString())
                .get(identity()).peek().asString();
    }

    protected void checkMbodyMessages() throws Exception {
        for (Flag flag: FLAGS) {
            String testMessage = getMbodyMessage(props().mbodyUri(), mid, flag);
            String prodMessage = getMbodyMessage(props().mbodyB2BUri(), mid, flag);

            Map testMessageMap = JsonUtils.getObject(testMessage.toString(), Map.class);
            Map prodMessageMap = JsonUtils.getObject(prodMessage.toString(), Map.class);

            assertThat("Письмо с mid=" + mid, prodMessageMap, beanDiffer(testMessageMap));
        }
    }

    protected static Collection<Object[]> getMids(int testIndex) throws Exception {
        List<Object[]> allMids = getMids();
        if (testIndex*COUNT >= allMids.size()) {
            return Collections.emptyList();
        } else if ((testIndex + 1)*COUNT > allMids.size()) {
            return allMids.subList(testIndex*COUNT, allMids.size());
        } else {
            return allMids.subList(testIndex*COUNT, (testIndex + 1)*COUNT);
        }
    }

    private static List<Object[]> getMids() throws Exception {
        List<String> folderIds = folders(FoldersObj.empty().setUid(authClient.account().uid()))
                .header(Headers.SERVICE_TICKET, TvmProperties.props().ticketFor("hound"))
                .header(Headers.USER_TICKET, authClient.account().userTicket())
                .get()
                .via(authClient.authHC())
                .fids();

        List<String> mids = new ArrayList<>();
        for (String fid : folderIds) {
            mids.addAll(getFolderMids(fid));
        }
        return new ArrayList<>(toParameterized(mids));
    }

    private static List<String> getFolderMids(String fid) {
        return messagesByFolder(MessagesByFolderObj.empty().setUid(authClient.account().uid()).setFid(fid)
                .setTill("2000000000").setFirst("0").setCount("1000"))
                .header(Headers.SERVICE_TICKET, TvmProperties.props().ticketFor("hound"))
                .get()
                .via(authClient.authHC())
                .resp()
                .getEnvelopes().stream()
                .map(e -> e.getMid())
                .collect(Collectors.toList());
    }
}


