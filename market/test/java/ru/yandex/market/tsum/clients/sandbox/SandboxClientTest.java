package ru.yandex.market.tsum.clients.sandbox;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 17/05/2017
 */
public class SandboxClientTest {


    @Test
    @Ignore
    public void testFrontWorkflow() throws Exception {
        SandboxClient sandboxClient = new SandboxClient(
            "https://sandbox.yandex-team.ru/api/v1.0",
            "",
            "AQAD_OAUTH_TOKEN",
            null
        );

        TaskInputDto taskInput = new TaskInputDto("MARKET_FRONT_BUILD_VENDORS");
        taskInput.setDescription("Тестируем пайп. ID ab:c:424242");
        taskInput.setOwner("MARKET");
        taskInput.addCustomField("vendor_branch", "master");
        taskInput.addCustomField("common_branch", "develop");
        taskInput.addCustomField("app_branch", "master");
        taskInput.addCustomField("fix_version", "2017.42.42");

        TaskRequirementsInputDto requirementsInput = new TaskRequirementsInputDto();
        requirementsInput.setClientTags("GENERIC");
        //requirementsInput.setClientTags("LXC");
        requirementsInput.setPlatform("any");
        taskInput.setRequirements(requirementsInput);

        SandboxTask task = sandboxClient.createTask(taskInput);
        sandboxClient.startTask(task.getId(), "Должно");

        while (true) {
            Thread.sleep(100);
            TaskSearchRequest searchRequest = new TaskSearchRequest();
            searchRequest.setType("MARKET_FRONT_BUILD_VENDORS");
            task = sandboxClient.getTask(searchRequest).get();
            if (task.getStatus().equals("SUCCESS")) {
                break;
            }
        }

        sandboxClient.release(new ReleaseCreate(task.getId(), SandboxReleaseType.TESTING, "Пайп!"));

        List<TaskResource> resources = sandboxClient.getResources(task.getId());
    }

    @Test
    public void testDateTimeFormat() throws ParseException {
        DateFormat format = new SimpleDateFormat(SandboxClient.DATE_TIME_FORMAT);
        Date date = format.parse("2017-06-14T12:16:08.380000Z");
        Assert.assertEquals(1497442948000L, date.getTime());
    }

    @Test
    @Ignore
    public void getRelease() {
        SandboxClient sandboxClient = new SandboxClient(
            "https://sandbox.yandex-team.ru/api/v1.0",
            "",
            "OAUTH_TOKEN",
            null
        );
        Release release = sandboxClient.getRelease(181927524);
    }

    @Test
    public void testCheckIfResponseContainsResourceType() {
        String expectedResourceType = "MARKET_SPOK_RESOURCE";
        String responseStub = String.format("[{\"type\": \"%s\", \"description\": \"\"}]", expectedResourceType);

        Assert.assertTrue(
            SandboxClient.checkIfResponseContainsResourceType(responseStub, expectedResourceType)
        );
    }

    @Test
    public void testCheckIfResponseDoesNotContainResourceType() {
        String expectedResourceType = "MARKET_SPOK_RESOURCE";
        String responseStub = "[{\"type\": \"WRONG_RESOURCE_TYPE\", \"description\": \"\"}]";

        Assert.assertFalse(
            SandboxClient.checkIfResponseContainsResourceType(responseStub, expectedResourceType)
        );
    }
}
