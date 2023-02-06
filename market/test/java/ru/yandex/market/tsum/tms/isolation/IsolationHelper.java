package ru.yandex.market.tsum.tms.isolation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import ru.yandex.market.tsum.tms.isolation.model.NamedBean;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 04/12/2018
 */
public class IsolationHelper {
    private IsolationHelper() {

    }


    public static Set<NamedBean> loadBeansHierarchy(Class<?> configClass) {
        Set<NamedBean> declaredServices = getBeans(configClass);

        Import importAnnotation = configClass.getAnnotation(Import.class);
        if (importAnnotation == null) {
            return declaredServices;
        }

        Arrays.stream(importAnnotation.value())
            .forEach(config -> declaredServices.addAll(loadBeansHierarchy(config)));

        return declaredServices;
    }

    private static Set<NamedBean> getBeans(Class<?> config) {
        return Arrays.stream(config.getDeclaredMethods())
            .filter(x -> x.isAnnotationPresent(Bean.class))
            .map(x -> {
                String beanName = x.getName();
                String[] name = x.getAnnotation(Bean.class).name();
                if (name.length > 0 && name[0].length() > 0) {
                    beanName = name[0];
                }

                return new NamedBean(beanName, x.getReturnType());
            })
            .collect(Collectors.toSet());
    }
}
