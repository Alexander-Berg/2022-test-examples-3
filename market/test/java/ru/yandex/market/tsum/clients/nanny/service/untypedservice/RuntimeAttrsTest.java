package ru.yandex.market.tsum.clients.nanny.service.untypedservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 05/09/2017
 */
public class RuntimeAttrsTest {
    private JsonObject runtimeAttrsJsonObject;

    @Before
    public void setUp() throws Exception {
        //resources
        JsonObject sandboxFile = new JsonObject();
        sandboxFile.addProperty("resource_type", "MARKET_COMMON_CONFIG");
        sandboxFile.addProperty("task_id", "136801870");
        sandboxFile.addProperty("task_type", "MARKET_YA_PACKAGE");
        JsonArray sandboxFiles = new JsonArray();
        sandboxFiles.add(sandboxFile);
        JsonObject resources = new JsonObject();
        resources.add("sandbox_files", sandboxFiles);
        //engines
        JsonObject engines = new JsonObject();
        engines.addProperty("engine_type", "ISS_SAS");
        //instances
        JsonObject group = new JsonObject();
        group.addProperty("name", "SAS_MARKET_TEST_FRONT_DESKTOP");
        group.addProperty("release", "tags/stable-100-r68");
        JsonArray groups = new JsonArray();
        groups.add(group);
        JsonObject extendedGencfgGroups = new JsonObject();
        extendedGencfgGroups.add("groups", groups);
        JsonObject instances = new JsonObject();
        instances.add("extended_gencfg_groups", extendedGencfgGroups);

        JsonObject content = new JsonObject();
        content.add("resources", resources);
        content.add("engines", engines);
        content.add("instances", instances);
        runtimeAttrsJsonObject = new JsonObject();
        runtimeAttrsJsonObject.addProperty("_id", "1234");
        runtimeAttrsJsonObject.add("content", content);

    }

    @Test
    public void addStaticFile() {
        RuntimeAttrs runtimeAttrs = new RuntimeAttrs(runtimeAttrsJsonObject);

        String path1 = "path1";
        String content1 = "content1";
        String path2 = "path2";
        String content2 = "content2";

        runtimeAttrs.addStaticFile(path1, content1);

        JsonArray staticFiles = runtimeAttrs.getContent()
            .getAsJsonObject("resources")
            .getAsJsonArray("static_files");

        assertEquals(path1, staticFiles.get(0).getAsJsonObject().get("local_path").getAsString());
        assertEquals(content1, staticFiles.get(0).getAsJsonObject().get("content").getAsString());

        runtimeAttrs.addStaticFile(path2, content2);
        assertEquals(path2, staticFiles.get(1).getAsJsonObject().get("local_path").getAsString());
        assertEquals(content2, staticFiles.get(1).getAsJsonObject().get("content").getAsString());

        runtimeAttrs.addStaticFile(path1, content2);
        assertEquals(path2, staticFiles.get(0).getAsJsonObject().get("local_path").getAsString());
        assertEquals(content2, staticFiles.get(0).getAsJsonObject().get("content").getAsString());
        assertEquals(path1, staticFiles.get(1).getAsJsonObject().get("local_path").getAsString());
        assertEquals(content2, staticFiles.get(1).getAsJsonObject().get("content").getAsString());
    }

    @Test
    public void updateSandboxResource() {
        RuntimeAttrs runtimeAttrs = new RuntimeAttrs(runtimeAttrsJsonObject);
        runtimeAttrs.updateSandboxResource(123456789, "MARKET_COMMON_CONFIG", 987654321);
        assertEquals("123456789", runtimeAttrs.getContent()
            .getAsJsonObject("resources")
            .getAsJsonArray("sandbox_files")
            .get(0).getAsJsonObject().getAsJsonPrimitive("task_id").getAsString()
        );
    }

    @Test
    public void setEngine() {
        RuntimeAttrs runtimeAttrs = new RuntimeAttrs(runtimeAttrsJsonObject);
        runtimeAttrs.setEngine("ISS_MSK");
        assertEquals("ISS_MSK", runtimeAttrs.getContent()
            .getAsJsonObject("engines").getAsJsonPrimitive("engine_type").getAsString()
        );

    }

    @Test
    public void setExtendedGenCfgGroup() {
        RuntimeAttrs runtimeAttrs = new RuntimeAttrs(runtimeAttrsJsonObject);
        runtimeAttrs.setExtendedGenCfgGroup("IVA_MARKET_TEST_FRONT_DESKTOP", "tags/stable-100-r69", true);
        JsonObject extendedGencfgGroups = runtimeAttrs.getContent()
            .getAsJsonObject("instances")
            .getAsJsonObject("extended_gencfg_groups").getAsJsonObject();

        JsonObject group = extendedGencfgGroups.getAsJsonArray("groups").get(0).getAsJsonObject();

        assertEquals("IVA_MARKET_TEST_FRONT_DESKTOP", group.getAsJsonPrimitive("name").getAsString());
        assertEquals("tags/stable-100-r69", group.getAsJsonPrimitive("release").getAsString());
        assertFalse(extendedGencfgGroups.getAsJsonObject("gencfg_volumes_settings").get("use_volumes").getAsBoolean());
    }

    @Test
    public void enableBindPathsIntoContainer() {
        RuntimeAttrs runtimeAttrs = new RuntimeAttrs(runtimeAttrsJsonObject);
        runtimeAttrs.enableBindPathsIntoContainer();

        JsonArray bind = runtimeAttrs.getContent()
            .getAsJsonObject("instance_spec")
            .getAsJsonObject("layersConfig")
            .getAsJsonArray("bind");

        assertTrue(bind.contains(RuntimeAttrs.createBindObject("/place/db/www/logs")));
        assertTrue(bind.contains(RuntimeAttrs.createBindObject("/place/db/bsconfig/webcache")));
        assertTrue(bind.contains(RuntimeAttrs.createBindObject("/place/db/bsconfig/webstate")));
    }


    @Test
    public void removeContainers() {
        JsonObject content = new JsonObject();
        JsonArray containers = new JsonArray();
        JsonObject container = new JsonObject();
        container.addProperty("name", "useful");
        JsonObject containerToRemove = new JsonObject();
        containerToRemove.addProperty("name", "toRemove");
        containers.add(container);
        containers.add(containerToRemove);
        JsonObject instanceSpec = new JsonObject();
        instanceSpec.add("containers", containers);
        content.add("instance_spec", instanceSpec);

        RuntimeAttrs runtimeAttrs = new RuntimeAttrs(null, content);

        runtimeAttrs.removeContainer("toRemove");

        JsonArray modifiedContainers =
            runtimeAttrs.getContent().get("instance_spec").getAsJsonObject().getAsJsonArray("containers");
        assertEquals(1, modifiedContainers.size());
        Assert.assertEquals(container, modifiedContainers.get(0));
    }

}
