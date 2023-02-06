package ru.yandex.market.checkout.referee;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author kukabara
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:appContext.xml")
@Transactional("pgTransactionManager")
@WebAppConfiguration
public abstract class EmptyTest {
    protected static final Random RND = new Random();

    static {
        System.setProperty("environment", "development");
        System.setProperty("spring.profiles.active", "development");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected CheckoutRefereeClient checkoutRefereeClient;
    @Autowired
    protected CheckoutRefereeClient checkoutRefereeJsonClient;

    protected CheckoutRefereeClient client;

    @BeforeEach
    public void init() {
        client = checkoutRefereeClient;
    }

    protected static long getId() {
        return (long) Math.abs(RND.nextInt());
    }


}
