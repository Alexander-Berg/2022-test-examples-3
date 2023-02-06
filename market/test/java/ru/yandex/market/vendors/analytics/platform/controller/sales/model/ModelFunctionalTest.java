package ru.yandex.market.vendors.analytics.platform.controller.sales.model;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;

/**
 * Functional tests for model controllers.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "ModelFunctionalTest.csv")
public abstract class ModelFunctionalTest extends CalculateFunctionalTest {
}
