package ru.yandex.market.hrms.core.persistence;

import javax.persistence.Entity;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@Slf4j
public class EntityAnnotationTest {

    @Test
    public void allEntitiesShouldHaveDefaultConstructor() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        for (BeanDefinition bd : scanner.findCandidateComponents("ru.yandex.market.hrms")) {
            Class<?> clazz = Class.forName(bd.getBeanClassName());
            //should throw an exception if class has no default constructor
            try {
                clazz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                log.error(sf("@Entity class {} has no default constructor", clazz.getName()));
                throw e;
            }
        }
    }
}
