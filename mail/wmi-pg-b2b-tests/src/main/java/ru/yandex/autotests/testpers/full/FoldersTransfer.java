package ru.yandex.autotests.testpers.full;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.google.common.net.HttpHeaders;
import org.apache.commons.collections.keyvalue.AbstractKeyValue;
import org.apache.commons.collections.keyvalue.DefaultKeyValue;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.StringDescription;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.factory.MailSendMsgObjFactory;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FoldersObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.InitialComposeCheck;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.autotests.passport.api.tools.registration.RegUser;
import ru.yandex.autotests.testpers.manual.UserCreate;
import ru.yandex.autotests.testpers.misc.PgProperties;
import ru.yandex.autotests.testpers.misc.filters.SortFilter;
import ru.yandex.qatools.allure.annotations.*;

import javax.ws.rs.core.UriBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.rholder.retry.StopStrategies.stopAfterAttempt;
import static com.github.rholder.retry.WaitStrategies.fixedWait;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Predicates.isNull;
import static com.google.common.io.Files.asCharSink;
import static com.google.common.io.Files.asCharSource;
import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.wmi.core.base.DocumentConverter.from;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.GOTO_ALL;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.GOTO_DRAFT;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.GOTO_SPAM;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.GOTO_TRASH;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.SORT_SUBJ_DESC;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelOne;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.Obj.XMLVERSION_DARIA2;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj.symbolOn;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList.mailboxListJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper.markSpam;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper.operDelete;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToLabel.messageToLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newChildFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderSetSymbol.settingsFolderSetSymbol;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsLabelCreate.newLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.testpers.manual.LaunchMigration.JENKINS;
import static ru.yandex.autotests.testpers.manual.LaunchMigration.LOGIN;
import static ru.yandex.autotests.testpers.manual.LaunchMigration.TOKEN;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Aqua.Test
@Title("Тестирование миграции сравнением")
@Features("Миграция ORA-PG")
@Issue("MAILPG-175")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FoldersTransfer extends BaseTest {
    
    private static final Logger LOG = LogManager.getLogger(FoldersTransfer.class);

    public static final String ORA2PG_PACKAGE_TRANSFER = "ora2pg-package-transfer";
    public static final String ORA2PG_PACKAGE_RESTORE = "ora2pg-package-restore_user";

    public static HttpClientManagerRule authClient2 = auth();
    private static final String DEFAULT_COLOR = "3126463";

    private static InitialComposeCheck composeCheck;

    private static Map<String, File> ora1 = new HashMap<>();
    private static Map<String, File> pgResp = new HashMap<>();
    private static Map<String, File> pgResp2 = new HashMap<>();
    private static Map<String, File> ora2 = new HashMap<>();

    public static final Retryer<String> RETRYER = RetryerBuilder.<String>newBuilder()
            .retryIfResult(isNull())
            .withStopStrategy(stopAfterAttempt(20))
            .withWaitStrategy(fixedWait(5, SECONDS)).build();

    public static final URI TRANSFER = UriBuilder.fromPath("/job/{job}/buildWithParameters")
            .scheme("https").host(JENKINS)
            .port(443)
            .build(ORA2PG_PACKAGE_TRANSFER);

    public static final URI RESTORE = UriBuilder.fromPath("/job/{job}/buildWithParameters")
            .scheme("https").host(JENKINS)
            .build(ORA2PG_PACKAGE_RESTORE);

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void transfer() throws InterruptedException {
        RegUser newUser = UserCreate.createNewUser();

//        String login = "robbitter-9198260035"; 
        String login = newUser.getLogin();
        String pwd = "simple123456";
    
        authClient2.with(login, pwd).login();
        composeCheck = new InitialComposeCheck(authClient2);
    }

    private DefaultHttpClient hc;

    @Before
    public void setUp() throws Exception {
        hc = authClient2.authHC();
    }

    @Test
    @Title("Создаем папки")
    public void aCreateFolders() throws Exception {
        String name1 = Util.getRandomString();
        String name2 = Util.getRandomString();
        String name3 = Util.getRandomString();
        String name4 = "кириллическая папка с пробелами " + name3;
        String name5 = Util.getRandomString();
        String name6 = Util.getRandomString();
        String name7 = Util.getRandomString();


        String fid1 = createFolder(name1);
        String fid2 = createFolder(name2);
        String fid3 = createFolder(name3);
        String fid4 = createFolder(name4);
        String fid5 = createFolder(name5);

        String fid11 = subFolder(fid1, name6);
        String fid12 = subFolder(fid1, name7);
        String fid21 = subFolder(fid2, name3);
        String fid41 = subFolder(fid4, name4);
        String fid51 = subFolder(fid5, name3);
        String fid111 = subFolder(fid11, name4);
        String f1111 = subFolder(fid111, name4);
        subFolder(fid21, "\u3445");
        subFolder(fid21, name6);
        subFolder(f1111, "012301231230");
        subFolder(f1111, name6);
        subFolder(fid12, Util.getRandomString());

        composeCheck.get();
    }


    @Test
    @Title("Создаем метки")
    public void aCreateLabels() throws Exception {
        newLabel(Util.getRandomString(), DEFAULT_COLOR).post().via(hc);
        newLabel("кириллицо " + Util.getRandomString(), "3126463").post().via(hc);
        newLabel(Util.getRandomString(), "3100463").post().via(hc);
        newLabel(Util.getRandomString(), "3126003").post().via(hc);
        newLabel(Util.getRandomString(), "3126400").post().via(hc);
    }


    @Test
    @Title("Отправляем отложенное письмо")
    public void aCreateOutboxFolders() throws Exception {
        //папки исходящих пока не существует, отправляем отложенное письмо, чтобы она появилась
        MailSendMsgObj msg = new MailSendMsgObjFactory()
                .setComposeCheck(new InitialComposeCheck(authClient2))
                .setAccount(authClient2.acc())
                .getDelayedMsg(DAYS.toMillis(100)); //now + 100 days
        //отправляем письмо
        jsx(SendMessage.class).params(msg).post().via(hc);
    }

    @Test
    @Title("Отправляем письмо с аттачем")
    public void aMailWithAtta() throws Exception {
        File attach = Util.generateRandomShortFile("atta.zip", 64);

        MailSendMsgObj msg = msg().addAtts("application", attach);
        MailSend.sendViaProd(msg).post().via(hc).resultOk();
        new WaitUtils(authClient2).subj(msg.getSubj()).waitDeliver();
    }

    @Test
    @Title("Сохраняем письмо с аттачем в черновики")
    public void aMailWithAttaToDraft() throws Exception {
        File attach = Util.generateRandomShortFile("atta.zip", 64);

        MailSendMsgObj msg = msg().addAtts("application", attach).setNosend("yes");
        MailSend.sendViaProd(msg).post().via(hc).resultOk();
    }


    @Test
    @Title("Отправляем простое письмо")
    public void aSimpleMail() throws Exception {
        MailSendMsgObj msg = msg();
        MailSend.sendViaProd(msg).post().via(hc).resultOk();
        new WaitUtils(authClient2).subj(msg.getSubj()).waitDeliver();
    }

    @Test
    @Title("Перемещаем письмо в спам")
    public void aAddToSpam() throws Exception {
        MailSendMsgObj msg = msg();
        MailSend.sendViaProd(msg).post().via(hc).resultOk();
        String mid = new WaitUtils(authClient2).subj(msg.getSubj()).waitDeliver().getMid();

        // Метка важные
        String warnLabel = jsx(Labels.class).post().via(hc).lidByName(WmiConsts.LABEL_PRIORITY_HIGH);
        // Помечаем важным
        messageToLabel(labelOne(mid, warnLabel)).post().via(hc).errorcodeShouldBeEmpty();

        markSpam(mid).post().via(hc);
    }

    @Test
    @Title("Перемещаем письмо в удаленные")
    public void aAddToDeleted() throws Exception {
        MailSendMsgObj msg = msg();
        MailSend.sendViaProd(msg).post().via(hc).resultOk();
        String mid = new WaitUtils(authClient2).subj(msg.getSubj()).waitDeliver().getMid();
        String lid = newLabel("кириллицо " + Util.getRandomString(), "3126463").post().via(hc).updated();
        // Помечаем важным
        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();

        operDelete(mid).post().via(hc);
    }

    @Test
    @Title("Помечаем письмо произвольной меткой и важным")
    public void aMarkWarn() throws Exception {
        MailSendMsgObj msg = msg();
        MailSend.sendViaProd(msg).post().via(hc).resultOk();
        String mid = new WaitUtils(authClient2).subj(msg.getSubj()).waitDeliver().getMid();

        String lid = newLabel("кириллицо " + Util.getRandomString(), "3126463").post().via(hc).updated();
        // Метка важные
        String warnLabel = jsx(Labels.class).post().via(hc).lidByName(WmiConsts.LABEL_PRIORITY_HIGH);
        // Помечаем важным
        messageToLabel(labelOne(mid, warnLabel)).post().via(hc).errorcodeShouldBeEmpty();
        messageToLabel(labelOne(mid, lid)).post().via(hc).errorcodeShouldBeEmpty();
    }

    @Test
    @Title("Помечаем папки символом")
    public void aSetSymbolOnNewFolder() throws IOException {
        Stream.of(
                SettingsFolderSymbolObj.Symbols.ARCHIVE,
                SettingsFolderSymbolObj.Symbols.DISCOUNT,
                SettingsFolderSymbolObj.Symbols.TEMPLATE).forEach(symbol -> {
                    try {
                        String fid = createFolder(Util.getRandomString());
                        settingsFolderSetSymbol(symbolOn(fid, symbol.value()))
                                .post().via(hc).errorcodeShouldBeEmpty().shouldBe().updated(is("ok"));

                    } catch (IOException e) {
                        throw new RuntimeException("", e);
                    }
                }
        );
//        MAILPG-407
//        Stream.of(
//                SettingsFolderSymbolObj.Symbols.ARCHIVE,
//                SettingsFolderSymbolObj.Symbols.DISCOUNT,
//                SettingsFolderSymbolObj.Symbols.TEMPLATE).forEach(symbol -> {
//                    try {
//                        String fid = createFolder(Util.getRandomString());
//                        settingsFolderSetSymbol(symbolOn(fid, symbol.value()))
//                                .post().via(hc).errorcodeShouldBeEmpty().shouldBe().updated(is("ok"));
//                    } catch (IOException e) {
//                        throw new RuntimeException("", e);
//                    }
//                }
//        );
    }


    @Test
    @Title("Сохраняем состояние до трансфера из ORA в PG")
    public void bSaveStateORA() throws Exception {
        saveRespTo(ora1, "ORA before");
    }

    //TODO кастомная сортировка папок 


    @Test
    @Title("[Трансфер] Из ORA в PG")
    public void cTransferORAtoPG() throws Exception {
        String location = given().auth().preemptive().basic(LOGIN, TOKEN)
                .filter(log())
                .formParam("LOGIN", authClient2.acc().getLogin())
                .formParam("DO_UPDATE_MAIL_MIGRATION", "true")
                .formParam("TRANSFER_PKG", PgProperties.pgProps().getTransferPkg())
                .expect()
                .post(TRANSFER).header(HttpHeaders.LOCATION);

        shouldWaitForJobSuccessStatus(location);
    }

    @Test
    @Title("Сохраняем состояние после трансфера в PG")
    public void dSaveStatePG() throws Exception {
        saveRespTo(pgResp, "PG");
    }


    @Test
    @Title("[Трансфер] Из PG в PG")
    @Description("curl -s \"sharpei.mail.yandex.net/stat\" - отдаст информацию о всех шардах")
    public void eTransferORAtoPG() throws Exception {
        String location = given().auth().preemptive().basic(LOGIN, TOKEN)
                .filter(log())
                .formParam("LOGIN", authClient2.acc().getLogin())
                .formParam("DO_UPDATE_MAIL_MIGRATION", "true")
                .formParam("TRANSFER_PKG", PgProperties.pgProps().getTransferPkg())
                //см. @Description
                .formParam("TO_DB", "postgre:5")
                .expect()
                .post(TRANSFER).header(HttpHeaders.LOCATION);

        shouldWaitForJobSuccessStatus(location);
    }

    @Test
    @Title("Сохраняем состояние после трансфера из PG в PG")
    public void fSaveStatePG2() throws Exception {
        saveRespTo(pgResp2, "PG");
    }

    @Test
    @Title("[Трансфер] Из PG в ORA")
    public void gMigrateBackPGtoORA() throws Exception {
        String location = given().auth().preemptive().basic(LOGIN, TOKEN)
                .formParam("LOGIN", authClient2.acc().getLogin())
                .formParam("DO_UPDATE_MAIL_MIGRATION", "true")
                .formParam("TRANSFER_PKG", PgProperties.pgProps().getTransferPkg())
                .formParam("TO_DB", format("oracle:ymail/ympass@%s", composeCheck.getMDB()))
                .filter(log())
                .expect()
                .post(TRANSFER).header(HttpHeaders.LOCATION);

        shouldWaitForJobSuccessStatus(location);
    }

    @Test
    @Title("Сохраняем состояние после обратного трансфера в ORA")
    public void hSaveStateORA() throws Exception {
        saveRespTo(ora2, "ORA after");
    }

    @Test
    @Title("1. Сравнение после миграции ORA->PG")
    public void iaShouldBeNoDiffORAtoPG() throws Exception {
        compare(ora1, pgResp, "ORA->PG");
    }

    @Test
    @Title("2. Сравнение после миграции PG->PG")
    public void iaShouldBeNoDiffPGtoPG() throws Exception {
        compare(pgResp, pgResp2, "PG->PG");
    }

    @Test
    @Title("3. Сравнение после миграции PG->ORA")
    public void ibShouldBeNoDiffPGtoORA() throws Exception {
        compare(pgResp, ora2, "PG->ORA");
    }

    @Test
    @Title("4. Итоговое сравнение ORA vs ORA")
    public void icShouldBeNoDiffORAtoORA() throws Exception {
        compare(ora1, ora2, "ORA vs ORA");
    }

    private void shouldWaitForJobSuccessStatus(String location) throws Exception {
        String path = RETRYER.call(() -> given().filter(log()).port(443).get(location + "api/json").path("executable?.url"));
        String result = RETRYER.call(() -> given().filter(log()).port(443).get(path + "api/json").path("result"));
        assertThat(result, equalTo("SUCCESS"));
    }

    private void compare(Map<String, File> resp, Map<String, File> resp2, String direction) throws IOException {
        LOG.warn(format("========== NEXT PAIR (%s) ==========", direction));

        resp.entrySet().stream().forEach(entry -> {
            try {

                String content = asCharSource(entry.getValue(), UTF_8).read();
                Document doc = from(content).getConverted();

                DocumentCompareMatcher documentCompareMatcher = equalToDoc(doc)
                        .filterWithCondition(new SortFilter<>(), true);

                String content2 = asCharSource(resp2.get(entry.getKey()), UTF_8).read();
                Document doc2 = from(content2).getConverted();

                if (!documentCompareMatcher.matches(doc2)) {
                    StringDescription description = new StringDescription();
                    documentCompareMatcher.describeMismatch(doc2, description);

                    attachment(direction, entry.getKey(), documentCompareMatcher.getSavedPath());
                }

            } catch (IOException e) {
                throw new RuntimeException("Can't read resp from file", e);
            }
        });
    }
   
    @Attachment("[{0}] {1}")
    private String attachment(String direction, String url, String path) {
        String ell = elliptics().name(path).get().url();
        LOG.info(format("[%s] %s", direction, url));
        LOG.info(ell);
        
        return elliptics().name(path).get().asString();
    }

    private MailSendMsgObj msg() {
        return new MailSendMsgObjFactory()
                .setComposeCheck(new InitialComposeCheck(authClient2))
                .setAccount(authClient2.acc())
                .getSimpleEmptySelfMsg()
                .setSend("MailSendAttachesWithDifferentNames::sendAttachWithSpecifiedName()" + Util.getRandomString());
    }

    private void saveRespTo(Map<String, File> resp, String state) throws Exception {
//        replace.withHc(authClient2.authHC()).before();
        Map<Object, Object> collect = Stream.of(
                mailboxListJsx(empty().setSortType(SORT_SUBJ_DESC)),
                mailboxListJsx(empty().setGoto(GOTO_SPAM).setSortType(SORT_SUBJ_DESC)),
                mailboxListJsx(empty().setGoto(GOTO_TRASH).setSortType(SORT_SUBJ_DESC)),
                mailboxListJsx(empty().setGoto(GOTO_DRAFT).setSortType(SORT_SUBJ_DESC)),
                mailboxListJsx(empty().setGoto(GOTO_ALL).setSortType(SORT_SUBJ_DESC)),
                api(Labels.class),
                jsx(Labels.class),
                api(FolderList.class),
                jsx(FolderList.class),
                jsx(FolderList.class).params(FoldersObj.empty().setXmlVersion(XMLVERSION_DARIA2))
        )
                .map(oper -> {
                    try {
                        File file = folder.newFile();
                        asCharSink(file, UTF_8).write(oper.get().via(hc).toString());
                        return new DefaultKeyValue(oper.getRequest(), file);
                    } catch (IOException e) {
                        throw new RuntimeException(state, e);
                    }
                }).collect(Collectors.toMap(AbstractKeyValue::getKey, AbstractKeyValue::getValue));

        resp.putAll((Map) collect);
    }

    private String createFolder(String name) throws IOException {
        return newFolder(name).post().via(hc).updated();
    }


    private String subFolder(String parentfid, String childFolderName) throws Exception {
        return newChildFolder(childFolderName, parentfid).post().via(hc).updated();
    }

    private Document transform(Document doc, String path) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(new StreamSource(getClass().getClassLoader().getResourceAsStream(path)));

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(new DOMSource(doc), result);


        String response = writer.toString();
        System.out.println(response);
        return from(response).getConverted();
    }
}
