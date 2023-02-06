package ru.yandex.market.tsum.spok.controllers;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.core.registry.v2.model.spok.RtcEnvironmentSpec;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.registry.v2.dao.InstallationsDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.Installation;
import ru.yandex.misc.test.Assert;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class})
public class SpokControllerTest {
    private static final Gson GSON = new Gson();
    private static final String SERVICE_ID = "1";
    private static final String NANNY_SERVICE_PARAMS_FILE_PATH = "spok-validation-testcases/nanny-service.json";
    private static final String DEPLOY_SERVICE_PARAMS_FILE_PATH = "spok-validation-testcases/deploy-service.json";

    @Autowired
    private MongoTemplate mongoTemplate;

    private InstallationsDao installationsDao;
    private ServiceParams nannyServiceParams;
    private ServiceParams deployServiceParams;

    @Before
    public void setUp() throws Exception {
         nannyServiceParams = GSON.fromJson(FileUtils.readFileToString(getFile(NANNY_SERVICE_PARAMS_FILE_PATH),
                 StandardCharsets.UTF_8), ServiceParams.class);
        deployServiceParams = GSON.fromJson(FileUtils.readFileToString(getFile(DEPLOY_SERVICE_PARAMS_FILE_PATH),
            StandardCharsets.UTF_8), ServiceParams.class);
        installationsDao = new InstallationsDao(mongoTemplate);
    }

    @Test
    public void saveOrUpdateInstallationTest_nannyInstallationDoestExists_installationCreated() {
        ServiceParams serviceParams = nannyServiceParams;
        SpokController spokController = new SpokController(null, null, null, null, null, installationsDao,
            null, null, null, null, null, null, null);
        Component component = new Component(serviceParams, SERVICE_ID);
        Environment environment = Environment.TESTING;
        RtcEnvironmentSpec installationSpec = serviceParams.getEnvironments().get(environment);

        spokController.saveOrUpdateInstallation(component, environment, component.getName(),
            installationSpec, serviceParams);

        List<Installation> installations = installationsDao.getByComponentIdAndEnvironment(component.getId(),
            environment);
        Assert.assertEquals(installations.size(), 1);
        Installation newInstallation = installations.get(0);
        Assert.assertEquals(newInstallation.getNannyServices().size(),
            serviceParams.getEnvironments().get(environment).getNannyLocations().size());
        Assert.assertEquals(newInstallation.getDeployServices().size(), 0);
    }

    @Test
    public void saveOrUpdateInstallationTest_deployInstallationDoestExists_installationCreated() {
        ServiceParams serviceParams = deployServiceParams;
        SpokController spokController = new SpokController(null, null, null, null, null, installationsDao,
            null, null, null, null, null, null, null);
        Component component = new Component(serviceParams, SERVICE_ID);
        Environment environment = Environment.TESTING;
        RtcEnvironmentSpec installationSpec = serviceParams.getEnvironments().get(environment);


        spokController.saveOrUpdateInstallation(component, environment, component.getName(),
            installationSpec, serviceParams);

        List<Installation> installations = installationsDao.getByComponentIdAndEnvironment(component.getId(),
            environment);
        Assert.assertEquals(installations.size(), 1);
        Installation newInstallation = installations.get(0);
        Assert.assertEquals(newInstallation.getNannyServices().size(),
            0);
        Assert.assertEquals(newInstallation.getDeployServices().size(),
            serviceParams.getEnvironments().get(environment).getYaDeployLocations().size());
    }

    private static File getFile(String name) throws URISyntaxException {
        URL resource = SpokControllerTest.class.getClassLoader().getResource(name);
        Preconditions.checkNotNull(resource, "file not found: %s", name);
        return new File(resource.toURI());
    }
}
