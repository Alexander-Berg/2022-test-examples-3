package ru.yandex.autotests.market.partner.cocon;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.cocon.steps.CoconSteps;
import ru.yandex.autotests.market.cocon.user.CoconUsers;
import ru.yandex.autotests.market.cocon.user.User;
import ru.yandex.qatools.allure.annotations.Parameter;


@Aqua.Test(title = "Проверка конфигов кокона")
@Feature("Совместимость конфига кокона с market-partner")
@RunWith(Parameterized.class)
public class CabinetConfigTest {

    private static final Logger log = Logger.getLogger(CabinetConfigTest.class);

    private static final CoconSteps coconSteps = new CoconSteps();
    private static final CoconUsers coconUsers = new CoconUsers();

    @Parameter
    private final String cabinet;

    @Parameter
    private final String page;

    @Parameter
    private final User user;

    public CabinetConfigTest(String cabinet, String page, User user) {
        this.cabinet = cabinet;
        this.page = page;
        this.user = user;
    }


    @Parameterized.Parameters(name = "Cabinet: {0} Page: {1} User: {2}")
    public static Collection<Object[]> configContent() throws IOException {
        Collection<Object[]> data = new ArrayList<>();
        Map<String, List<String>> latestConfig = coconSteps.getLatestConfig();
        latestConfig.forEach((cabinet, pages) ->
                pages.forEach(page -> data.add(new Object[]{
                        cabinet,
                        page,
                        coconUsers.getUser(cabinet)
                }))
        );
        return data;
    }

    @Test
    public void testCabinetConfig() throws IOException, URISyntaxException {
        log.info("Checking cabinet " + cabinet + " for page " + page + " with user " + user);
        coconSteps.checkCabinetPage(cabinet, page, user);
    }


}
