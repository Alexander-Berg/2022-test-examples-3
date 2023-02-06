package ru.yandex.market.checker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.TestHelper;
import ru.yandex.market.checker.check.ContentSizeChecker;
import ru.yandex.market.checker.core.CoreCheckerTask;
import ru.yandex.market.checker.check.model.CoreSimpleCheck;
import ru.yandex.market.checker.core.JobInfo;
import ru.yandex.market.checker.dao.CoreCheckerDao;
import ru.yandex.market.checker.dao.JobInfoDao;
import ru.yandex.market.checker.zora.ZoraCheckerTaskRunner;
import ru.yandex.market.checker.zora.ZoraHttpService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author imelnikov
 */
class CheckerTaskRunnerTest extends EmptyTest {

    private static final List<String> URLS = Arrays.asList(
            "http://yandex.ru",
            "http://granbazar.ru/catalog/oborudovanie/teplovoe-oborudovanie/apparaty-ponchikovye/105401/",
            "http://clickserve.dartsearch.net/link/click?lid=43700011364849516&ds_s_kwgid=58700001332044839&ds_url_v=2&ds_dest_url=https://www.nike.com/ru/t/sportswear-windrunner-cargo-jacket-b0TjCq/BV2833-539?cp=gsns_kw_pla!ru!y!products"
    );

    private static String host;
    @Autowired
    private ZoraHttpService zoraHttpService;
    @Autowired
    private JobInfoDao jobInfoDao;


    @BeforeEach
    void init() throws UnknownHostException {
        host = InetAddress.getLocalHost().getHostName();
    }

    @Test
    @Disabled
    void testPageLoading() {
        CheckerTaskRunner taskRunner = new ZoraCheckerTaskRunner("production", host, zoraHttpService);
        List<CoreSimpleCheck> checks = Collections.singletonList(
                new CoreSimpleCheck(1, "1000", new ContentSizeChecker()));
        List<CoreCheckerTask> tasks = new ArrayList<>();
        for (String url : URLS) {
            System.out.println("Check url " + url);
            CoreCheckerTask task = TestHelper.createTask(url);
            task.setChecks(checks);
            taskRunner.loadPage(task);
            tasks.add(task);
        }

        tasks.forEach(t -> assertEquals(200, t.getHttpStatus()));
    }

    // MARKETASSESSOR-3166
    @Test
    @Disabled
    void loadSSL() {
        CheckerTaskRunner runner = new ZoraCheckerTaskRunner("production", host, zoraHttpService);

        runner.loadPage(
                TestHelper.createTask("http://www.elektrabel.by/catalog/floor_heating/mats/md100.html?complect=5,0"));
        runner.loadPage(
                TestHelper.createTask("http://motobody.ru/ru_RU/c/Мотоэкипировка/276"));
        runner.loadPage(
                TestHelper.createTask("https://www.mebelion.ru/catalog/She_825188.html?utm_source=imarket_spb&utm_medium=cpc&utm_term=2723444&utm_content=She_825188&utm_campaign=mebel|korpusnaya-mebel|obuvnitsy|banketki-stellazhi-dlya-obuvi"));
    }

    @Test
    @Disabled
    void executeTask() {
        CheckerTaskRunner runner = new ZoraCheckerTaskRunner("production", host, zoraHttpService);
        CoreCheckerTask task = TestHelper.createTask("yandex.ru");

        JobInfo jobInfo = jobInfoDao.jobList().get(0);
        jobInfo.setCheckerDao(jobInfoDao.createCheckerDao(jobInfo));
        CoreCheckerDao checkerDao = jobInfo.getCheckerDao();
        checkerDao.initDb();

        runner.executeTask(task, checkerDao);
    }
}
