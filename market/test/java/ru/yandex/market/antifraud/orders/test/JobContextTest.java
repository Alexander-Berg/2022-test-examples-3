package ru.yandex.market.antifraud.orders.test;

import java.util.Collection;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.test.config.JobsTestConfiguration;
import ru.yandex.market.volva.jobs.Job;

/**
 * @author dzvyagin
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@ContextConfiguration(classes = {JobsTestConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class JobContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void contextBuildTest(){
        Collection<Job> jobs = context.getBeansOfType(Job.class).values();
        Assertions.assertThat(jobs).isNotEmpty();
        System.out.println("Jobs:");
        jobs.stream().map(Job::getBeanName).forEach(n -> System.out.println("\t" + n));
        System.out.println("Context built successfully!");
    }
}
