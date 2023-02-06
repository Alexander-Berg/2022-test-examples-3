package ru.yandex.market.crm.campaign.http.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.crm.campaign.domain.promo.entities.TestPuidsGroup;
import ru.yandex.market.crm.campaign.services.sql.TestPuidsDAO;

/**
 * @author apershukov
 */
@RestController
public class TestIdsController {

    private final TestPuidsDAO testPuidsDao;

    public TestIdsController(TestPuidsDAO testPuidsDao) {
        this.testPuidsDao = testPuidsDao;
    }

    @RequestMapping(value = "/api/test_ids/puids", method = RequestMethod.GET)
    public List<TestPuidsGroup> listTestPuids() {
        return testPuidsDao.getAll();
    }
}
