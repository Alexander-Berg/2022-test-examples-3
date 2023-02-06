package ru.yandex.market.clab.test;

import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 02.10.2018
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {IntegrationTestContextInitializer.class})
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

}
