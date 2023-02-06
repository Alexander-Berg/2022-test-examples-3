package ru.yandex.market.tsum.clients.nanny;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nanny.tickets.Tickets;
import nanny.tickets_api.TicketsApi;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.nanny.configuration.Content;
import ru.yandex.market.tsum.clients.nanny.configuration.NannyConfiguration;
import ru.yandex.market.tsum.clients.nanny.dashboard.NannyDeploy;
import ru.yandex.market.tsum.clients.nanny.service.NannyService;
import ru.yandex.market.tsum.clients.nanny.task_group.children.NannyTask;
import ru.yandex.market.tsum.clients.nanny.task_group.children.history.NannyTaskHistory;
import ru.yandex.market.tsum.clients.nanny.task_groups.NannyTaskGroup;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 24/03/2017
 */
public class NannyClientTest {

    private NannyClient nannyClient;
    private NannyTicketApiClient nannyTicketApiClient;

    @Before
    public void setUp() throws Exception {
        nannyClient = new NannyClient("https://nanny.yandex-team.ru", "AQAD-qJSJwR8AAABT2AmPI5EfUS_k4lv3Mw2nz0", null);
        nannyTicketApiClient = new NannyTicketApiClient(
            "https://nanny.yandex-team.ru", "AQAD-qJSJwR8AAABT2AmPI5EfUS_k4lv3Mw2nz0"
        );

    }

    @Ignore
    @Test
    public void test() throws Exception {
        NannyConfiguration conf = nannyClient.getConfiguration(
            "testing_market_formalizer_fol", "28ae5007f93ea4709f88f51362030b5accd8b314"
        );

        NannyService service = nannyClient.getService("testing_market_formalizer_fol");
    }

    @Ignore
    @Test
    public void testGetServiceAnswer() {
        NannyService service = nannyClient.getService("production_market_market_sre_tms_vla");
        Optional<Content> activateContext = service.getInfoAttrs()
            .getContent()
            .getRecipes()
            .getContent().stream()
            .filter(content -> content.getDesc().contains("Activate"))
            .findFirst();
        Assert.assertTrue(activateContext.isPresent());
        Assert.assertTrue(activateContext.get()
            .getContext()
            .stream()
            .anyMatch(c ->
                c.getKey().equals("operating_degrade_level") &&
                    c.getValue() != null && Double.parseDouble(c.getValue()) > 0
            )
        );
    }

    @Ignore
    @Test
    public void testRecipe() {

        TicketsApi.FindTicketsResponse response = nannyTicketApiClient.findTickets(
            TicketsApi.FindTicketsRequest.newBuilder()
                .setQuery(Tickets.TicketsFindQuery.newBuilder().setReleaseId("SANDBOX_RELEASE-112345791-TESTING"))
                .setLimit(1000)
                .build()
        );

        Preconditions.checkArgument(response.getValueCount() == response.getTotal());

        Map<String, String> serviceToSnapshot = new HashMap<>();
        for (Tickets.Ticket ticket : response.getValueList()) {
            Tickets.ServiceDeployment serviceDeployment = ticket.getSpec().getServiceDeployment();
            serviceToSnapshot.put(serviceDeployment.getServiceId(), serviceDeployment.getSnapshotId());
        }

//        serviceToSnapshot.put("testing_market_front_vendors_fol", "859537a21bf");
//        serviceToSnapshot.put("testing_market_front_vendors_sas", "a4fba46f580");
        nannyClient.runRecipeDeploy("market_front_vendors", "testing_sequential_deploy", serviceToSnapshot, "42",
            false);
    }

    @Ignore
    @Test
    public void getDeploy() {
        NannyDeploy asd = nannyClient.getDeployment(
            "market_tsum_tms", "5d3ec83f8e85f37c78556fba:b8f8ef59-2ee2-45a4-bef5-848cfe2e7b79:1"
        );
    }

    @Ignore(value = "integration test to check api works")
    @Test
    public void testFind() throws Exception {
        nannyClient.findServices("/market");
    }

    @Ignore(value = "integration test to check api works")
    @Test
    public void getServiceInstancesTest() throws Exception {
        nannyClient.getServiceInstances("belmatter_testing_service");
    }

    @Test
    public void checkDataName() throws IOException {
        String recipeJson = Resources.toString(Resources.getResource("nanny/nannyRecipeBackctld.json"), Charsets.UTF_8);
        JsonObject recipeObject = new Gson().fromJson(recipeJson, JsonObject.class);
        JsonArray tasksArray = recipeObject.getAsJsonObject("content").getAsJsonArray("tasks");
        int skippedTasks = 0;
        for (JsonElement taskElement : tasksArray) {
            JsonObject data = taskElement.getAsJsonObject().getAsJsonObject("data");
            if (!nannyClient.checkDataName(data)) {
                skippedTasks++;
            }
        }
        Assert.assertEquals(3, skippedTasks);
    }

    private boolean hasExpectedDescription(NannyTaskGroup taskGroup) {
        String description = taskGroup.getDescription();
        return description.startsWith("Activate") ||
            description.startsWith("Remove") ||
            description.startsWith("Deactivate") ||
            description.startsWith("Generate ");
    }

    @Ignore
    @Test
    public void getTaskGroups() {
        List<NannyTaskGroup> taskGroups = nannyClient.getTaskGroups("DONE", 10, 100);
        Assert.assertEquals(10, taskGroups.size());
        Assert.assertTrue("Task should have expected description",
            taskGroups.stream().anyMatch(this::hasExpectedDescription));
        Assert.assertTrue("NannyServiceId should not be empty",
            taskGroups.stream().allMatch(taskGroup -> taskGroup.getNannyServiceId() != null));
    }

    @Ignore
    @Test
    public void getGroupTasks() {
        List<NannyTask> groupTasks = nannyClient.getGroupTasks("search-0125405940");
        Assert.assertTrue(groupTasks.stream().anyMatch(task -> task.getProcessorOptions() != null &&
            task.getProcessorOptions().getType().equals("NewConfigurationActivateTask")));
        Assert.assertTrue(groupTasks.stream().allMatch(group -> group.getId() != null));
    }

    @Ignore
    @Test
    public void getTaskHistory() {
        NannyTaskHistory history = nannyClient.getTaskHistory("search-0125405940/job-0000000000");
        Assert.assertTrue(history.getInstanceCount() > 0);
        Assert.assertTrue(history.getInstanceStates() != null);
        Assert.assertTrue(history.getInstanceStates().stream().allMatch(item -> item.getTimestamp() > 0));
        Assert.assertTrue(history.getInstanceStates().stream().anyMatch(item -> item.getProcessingCount() > 0));

    }
}
