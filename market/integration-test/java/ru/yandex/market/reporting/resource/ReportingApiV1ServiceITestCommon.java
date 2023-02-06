package ru.yandex.market.reporting.resource;

import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.market.reporting.common.domain.tree.Category;
import ru.yandex.market.reporting.common.domain.tree.Region;
import ru.yandex.market.reporting.generator.dao.MetadataService;
import ru.yandex.market.reporting.generator.domain.AuditReportParameters;
import ru.yandex.market.reporting.generator.domain.CompetitorsMapKind;
import ru.yandex.market.reporting.generator.domain.DatePeriod;
import ru.yandex.market.reporting.generator.domain.Domains;
import ru.yandex.market.reporting.generator.domain.FileDesc;
import ru.yandex.market.reporting.generator.domain.Job;
import ru.yandex.market.reporting.generator.domain.JobParameters;
import ru.yandex.market.reporting.generator.domain.JobStatus;
import ru.yandex.market.reporting.generator.domain.JobStatusEnum;
import ru.yandex.market.reporting.generator.domain.JobStatuses;
import ru.yandex.market.reporting.generator.domain.Jobs;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.Profile;
import ru.yandex.market.reporting.generator.domain.Profiles;
import ru.yandex.market.reporting.generator.domain.ReportComponents;
import ru.yandex.market.reporting.generator.domain.ResourceUrl;
import ru.yandex.market.reporting.generator.domain.ShopFeeds;
import ru.yandex.market.reporting.generator.util.GsonUtils;

import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.HttpURLConnection;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Locale;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.market.reporting.generator.domain.JobStatusEnum.IN_PROGRESS;
import static ru.yandex.market.reporting.generator.domain.JobStatusEnum.NEW;
import static ru.yandex.market.reporting.generator.domain.JobStatusEnum.SUCCESSFUL;

@Log4j2
@Ignore
public class ReportingApiV1ServiceITestCommon {

    @Value("${username}")
    String testUser;

    @Value("${profile}")
    String testProfile;

    @Value("${service.url}")
    String serviceUrl;

    @Inject
    ReportingMetadataService reportingMetadataService;

    WebTarget target;

    @Before
    public void setUp() throws Exception {
        target = ClientBuilder.newClient().target(serviceUrl);
    }

    @Test
    public void testDomains() {
        Domains domains = target.path("v1/domains")
                .request()
                .get(Domains.class);

        assertThat(domains.getDomains(), hasItems("mts.ru", "mvideo.ru"));
    }

    @Test
    public void testDomains_with_query_and_limit() {
        Domains domains = target.path("v1/domains")
                .queryParam("query", "mvi")
                .queryParam("limit", "5")
                .request()
                .get(Domains.class);

        assertThat(domains.getDomains(), hasItem("mvideo.ru"));
        assertThat(domains.getDomains().size(), is(5));
    }

    @Test
    public void testRegions() {
        Region region = target.path("v1/regions")
                .request()
                .get(Region.class);

        assertThat(region.getId(), is(10000L));
        assertThat(region.getName(), is("Земля"));
        assertThat(region.getRegions().size(), is(6));
    }

    @Test
    public void testCategories() {
        Category category = target.path("v1/categories")
                .request()
                .get(Category.class);

        assertThat(category.getId(), is(90401L));
        assertThat(category.getName(), is("Все товары"));
        assertThat(category.getCategories().size(), greaterThan(10));
    }

    @Test
    public void testFeedsByDomain() {
        ShopFeeds feeds = target.path("v1/feeds_by_domain")
                .queryParam("domain", "mts.ru")
                .request()
                .get(ShopFeeds.class);

        assertThat(feeds.getShops().isEmpty(), is(false));
    }

    @Test
    public void testFeedsByDomain_empty() {
        ShopFeeds feeds = target.path("v1/feeds_by_domain")
                .queryParam("domain", "\\N")
                .request()
                .get(ShopFeeds.class);

        assertThat(feeds.getShops().isEmpty(), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetResourceUrl() {
        ResourceUrl resourceUrl = getResourceUrl("/my_file");

        assertThat(resourceUrl.getUrl(), endsWith("/my_file"));
        assertThat(resourceUrl.getHeaders(), hasKey("Authorization"));
    }

    public ResourceUrl getResourceUrl(String fileId) {
        String json = target.path("v1/get_resource_url")
                .queryParam("id", fileId)
                .request()
                .get(String.class);

        log.debug("Got resourse_url {}", json);

        return GsonUtils.fromJson(
                json, ResourceUrl.class);
    }

    @Test
    public void testProfiles() {
        target.path("v1/drop_profile")
                .queryParam("name", "my_profile")
                .request()
                .delete();

        MarketReportParameters marketReportParameters = getCpcReportParameters(CompetitorsMapKind.ASSORTMENT, 1L);

        Profile profile = new Profile("my_profile", marketReportParameters);
        Response response = target.path("v1/save_profile")
                .queryParam("name", profile.getName())
                .request()
                .post(Entity.json(profile.getParameters()));

        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));

        Profiles profiles = target.path("v1/profiles")
                .request()
                .get(Profiles.class);

        assertThat(profiles.getProfiles(), hasItem(profile));

        Response delete = target.path("v1/drop_profile")
                .queryParam("name", "my_profile")
                .request()
                .delete();
        assertThat(delete.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    @Ignore
    @Test
    public void testBuildReport_invalid_parameters() throws InterruptedException {
        MarketReportParameters marketReportParameters = new MarketReportParameters();

        Response response = target.path("v1/build_report")
                .queryParam("user", testUser)
                .queryParam("profile", testProfile)
                .request()
                .post(Entity.json(marketReportParameters));

        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testBuildCpcReportAssortmentAndCpa() throws InterruptedException {
        MarketReportParameters marketReportParameters = getCpcReportParameters(CompetitorsMapKind.ASSORTMENT, null);

        DatePeriod period = new DatePeriod(YearMonth.of(2016, 5), YearMonth.of(2016, 10));
        ReportComponents.CpaSlide1 cpaSlide1 = new ReportComponents.CpaSlide1();
        cpaSlide1.setCategoryDynamicDiagramPeriod(period);
        cpaSlide1.setCompletedCancelledDiagramPeriod(period);
        cpaSlide1.setOrdersShareDiagramPeriod(period);
        cpaSlide1.setOrdersShareDynamicDiagramPeriod(period);
        marketReportParameters.getComponents().setCpaSlide1(cpaSlide1);

        ReportComponents.CpaSlide2 cpaSlide2 = new ReportComponents.CpaSlide2();
        cpaSlide2.setCompetitionMapDiagramPeriod(period);
        marketReportParameters.getComponents().setCpaSlide2(cpaSlide2);

        testBuildReportInt(marketReportParameters);
    }

    @Test
    public void testBuildCpcReportAssortment() throws InterruptedException {
        testBuildReportInt(getCpcReportParameters(CompetitorsMapKind.ASSORTMENT, null));
    }

    @Test
    public void testBuildCpcReportCategory() throws InterruptedException {
        testBuildReportInt(getCpcReportParameters(CompetitorsMapKind.CATEGORY, null));
    }

    @Test
    public void testBuildCpcReportBrand() throws InterruptedException {
        testBuildReportInt(getCpcReportParameters(CompetitorsMapKind.BRAND, 1L));
    }

    private void testBuildReportInt(MarketReportParameters marketReportParameters) throws InterruptedException {
        JobStatus jobStatus = target.path("v1/build_report")
                .queryParam("user", testUser)
                .queryParam("profile", testProfile)
                .request()
                .post(Entity.json(marketReportParameters), JobStatus.class);

        assertThat(jobStatus.getJobId(), not(isEmptyString()));
        assertThat(jobStatus.getStatus(), is(NEW));
        assertThat(jobStatus.getFiles().size(), is(0));


        JobStatus jobStatusInProgress = waitForStatus(jobStatus.getJobId(), IN_PROGRESS);
        assertThat(jobStatusInProgress.getJobId(), is(jobStatus.getJobId()));


        JobStatus jobStatusFinished = waitForStatus(jobStatus.getJobId(), SUCCESSFUL);
        assertThat(jobStatusFinished.getFiles().size(), is(greaterThanOrEqualTo(2)));

        verifyFilesAvailable(jobStatusFinished);

        testJobActions(jobStatus.getJobId());
    }

    private void testJobActions(String jobId) {
        Jobs jobs = target.path("v1/jobs")
                .queryParam("user", testUser)
                .request()
                .get(Jobs.class);

        assertThat(jobs.getJobs().size(), greaterThanOrEqualTo(1));
        Job ourJob = jobs.getJobs().stream()
                .filter(j -> j.getJobId().equals(jobId))
                .findAny().get();
//        assertThat(ourJob.getParameters(), is(reportParameters));
        assertThat(ourJob.getUser(), is(testUser));
        assertThat(ourJob.getProfile(), is(testProfile));


        Job ourJob1 = target.path("v1/job")
                .queryParam("id", jobId)
                .request()
                .get(Job.class);
        assertThat(ourJob1, is(ourJob));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildCpaReport() throws InterruptedException {
        MarketReportParameters marketReportParameters = new MarketReportParameters();
        marketReportParameters.setShop("Pleer");
        marketReportParameters.setDomain("Pleer.RU");
        marketReportParameters.setRegions(ImmutableList.of(225L, 10174L, 187L, 1L));
        marketReportParameters.setCategories(ImmutableList.of(90403L, 90639L));

        DatePeriod period = new DatePeriod(YearMonth.of(2016, 5), YearMonth.of(2016, 10));
        ReportComponents.CpaSlide1 cpaSlide1 = new ReportComponents.CpaSlide1();
        cpaSlide1.setCategoryDynamicDiagramPeriod(period);
        cpaSlide1.setCompletedCancelledDiagramPeriod(period);
        cpaSlide1.setOrdersShareDiagramPeriod(period);
        cpaSlide1.setOrdersShareDynamicDiagramPeriod(period);
        marketReportParameters.getComponents().setCpaSlide1(cpaSlide1);

        ReportComponents.CpaSlide2 cpaSlide2 = new ReportComponents.CpaSlide2();
        cpaSlide2.setCompetitionMapDiagramPeriod(period);
        marketReportParameters.getComponents().setCpaSlide2(cpaSlide2);

        testBuildReportInt(marketReportParameters);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildForecasterReport() throws InterruptedException {
        MarketReportParameters marketReportParameters = new MarketReportParameters();
        marketReportParameters.setShop("Megafon");
        marketReportParameters.setDomain("megafon.ru");
        marketReportParameters.setRegions(ImmutableList.of(213L, 2L, 65L));
        marketReportParameters.setCategories(ImmutableList.of(91491L));
        marketReportParameters.setLanguage(Locale.forLanguageTag("ru"));

        ReportComponents.Forecaster forecaster = new ReportComponents.Forecaster();
        forecaster.setPeriodLength(7);
        marketReportParameters.getComponents().setForecaster(forecaster);

        JobStatus jobStatus = target.path("v1/build_report")
                .queryParam("user", testUser)
                .queryParam("profile", testProfile)
                .request()
                .post(Entity.json(marketReportParameters), JobStatus.class);

        assertThat(jobStatus.getJobId(), not(isEmptyString()));
        assertThat(jobStatus.getStatus(), is(NEW));
        assertThat(jobStatus.getFiles().size(), is(0));

        JobStatus jobStatusFinished = waitForStatus(jobStatus.getJobId(), SUCCESSFUL);
        assertThat(jobStatusFinished.getFiles().size(), is(2));

        verifyFilesAvailable(jobStatusFinished);
    }

    @Test
    public void testBuildAssortmentReport() throws InterruptedException {
        MarketReportParameters marketReportParameters = new MarketReportParameters();
        marketReportParameters.setShop("MTC");
        marketReportParameters.setDomain("mts.ru");
        marketReportParameters.setRegions(ImmutableList.of(225L, 10174L, 187L, 1L));
        marketReportParameters.setCategories(ImmutableList.of(91491L, 90490L, 91042L, 10498025L));

        DatePeriod period = new DatePeriod(YearMonth.of(2016, 5), YearMonth.of(2016, 10));
        ReportComponents.Assortment assortment = new ReportComponents.Assortment();
        assortment.setPeriod(period);
//        assortment.setNumModels(5);
        assortment.setGroupByMonth(true);
        marketReportParameters.getComponents().setAssortment(assortment);

        Job jobStatus = target.path("v1/build_report")
                .queryParam("user", testUser)
                .queryParam("profile", testProfile)
                .request()
                .post(Entity.json(marketReportParameters), Job.class);

        assertThat(jobStatus.getJobId(), not(isEmptyString()));
        assertThat(jobStatus.getStatus(), is(NEW));
        assertThat(jobStatus.getFiles().size(), is(0));

        JobStatus jobStatusFinished = waitForStatus(jobStatus.getJobId(), SUCCESSFUL);
        assertThat(jobStatusFinished.getFiles().size(), is(2));

        verifyFilesAvailable(jobStatusFinished);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildAuditReport() throws InterruptedException {
        AuditReportParameters auditReportParameters = new AuditReportParameters();
        auditReportParameters.setFeedId(462156L);

        JobStatus jobStatus = target.path("v1/build_audit_report")
                .queryParam("user", testUser)
                .request()
                .post(Entity.json(auditReportParameters), JobStatus.class);

        assertThat(jobStatus.getJobId(), not(isEmptyString()));
        assertThat(jobStatus.getStatus(), is(NEW));
        assertThat(jobStatus.getFiles().size(), is(0));

        JobStatus jobStatusFinished = waitForStatus(jobStatus.getJobId(), SUCCESSFUL);
        assertThat(jobStatusFinished.getFiles().size(), is(1));

        verifyFilesAvailable(jobStatusFinished);
    }

    @Test
    public void restartReport() {
        JobParameters<MarketReportParameters> parameters = new JobParameters<>();
        parameters.setParameters(getCpcReportParameters(CompetitorsMapKind.ASSORTMENT, null));

        String jobId = reportingMetadataService.addNewJob(testUser, testProfile, parameters);
        reportingMetadataService.failJob(jobId, new RuntimeException());

        JobStatus jobStatus = target.path("v1/rebuild_report")
                .queryParam("id", jobId)
                .request()
                .post(Entity.json(null), JobStatus.class);

        assertThat(jobStatus.getStatus(), is(NEW));
    }

    public MarketReportParameters getCpcReportParameters(CompetitorsMapKind mapKind, Long brandVndId) {
        MarketReportParameters marketReportParameters = new MarketReportParameters();
        marketReportParameters.setShop("MTC");
        marketReportParameters.setDomain("mts.ru");
        marketReportParameters.setRegions(ImmutableList.of(225L, 10174L, 187L, 1L));
        marketReportParameters.setCategories(ImmutableList.of(91491L, 90490L, 91042L, 10498025L));

        DatePeriod period = new DatePeriod(YearMonth.of(2016, 5), YearMonth.of(2016, 10));
        ReportComponents.CpcSlide1 cpcSlide1 = new ReportComponents.CpcSlide1();
        cpcSlide1.setCategoryDynamicDiagramPeriod(period);
        cpcSlide1.setClicksShareDiagramPeriod(period);
        cpcSlide1.setClicksShareDynamicDiagramPeriod(period);
        marketReportParameters.getComponents().setCpcSlide1(cpcSlide1);

        ReportComponents.CpcSlide2 cpcSlide2 = new ReportComponents.CpcSlide2();
        cpcSlide2.setCompetitionMapDiagramPeriod(new DatePeriod(YearMonth.of(2016, 9), YearMonth.of(2016, 10)));
        cpcSlide2.setMapKind(mapKind);
        cpcSlide2.setBrandVndId(brandVndId);
        marketReportParameters.getComponents().setCpcSlide2(cpcSlide2);

        return marketReportParameters;
    }

    public JobStatus waitForStatus(String jobId, JobStatusEnum expectedStatus) throws InterruptedException {
        EnumSet<JobStatusEnum> inProgress = EnumSet.of(NEW, IN_PROGRESS);

        JobStatuses jobStatusNext = null;
        int attempts = 10000;
        do {
            if (jobStatusNext != null) {
                Thread.sleep(1000L);
            }

            jobStatusNext = target.path("v1/job_status")
                    .queryParam("id", jobId)
                    .request()
                    .get(JobStatuses.class);
        } while (jobStatusNext.getJobs().get(0).getStatus() != expectedStatus
                && inProgress.contains(jobStatusNext.getJobs().get(0).getStatus())
                && attempts-- > 0);

        if (jobStatusNext.getJobs().get(0).getStatus() != expectedStatus) {
            fail();
        }
        return jobStatusNext.getJobs().get(0);
    }

    public void verifyFilesAvailable(JobStatus jobStatus) {
        jobStatus.getFiles().stream()
                .map(FileDesc::getFileId)
                .forEach(this::verifyFileAvailable);
    }

    public void verifyFileAvailable(String fileId) {
        ResourceUrl resourceUrl = getResourceUrl(fileId);
        Response response = ClientBuilder.newClient()
                .target(resourceUrl.getUrl())
                .request()
                .headers(new MultivaluedHashMap(resourceUrl.getHeaders()))
                .head();

        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }
}
