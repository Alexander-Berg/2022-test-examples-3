package ru.yandex;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-bean.xml")
@ActiveProfiles({"functionalTest", "development"})
@Transactional
public abstract class EmptyTest {
    static {
        System.setProperty("environment", "development");
    }

    protected static final Random RND = new Random(1337);
    protected static final Date NOW = new Date();

    @PersistenceContext
    protected EntityManager entityManager;

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    protected static BigDecimal nextBigDecimal() {
        return BigDecimal.valueOf(RND.nextInt() + RND.nextDouble());
    }

    /**
     * Check if the proxy or persistent collection is initialized.
     *
     * @param proxy a persistable object, proxy, persistent collection or <tt>null</tt>.
     * @return true if the argument is already initialized, or is not a proxy or collection.
     */
    protected static boolean isInitialized(Object proxy) {
        return Hibernate.isInitialized(proxy);
    }
}
