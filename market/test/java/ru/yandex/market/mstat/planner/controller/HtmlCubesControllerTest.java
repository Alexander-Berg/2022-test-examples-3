package ru.yandex.market.mstat.planner.controller;

import com.google.common.collect.ImmutableMap;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.DefaultEnvironmentConfiguration;
import org.jtwig.environment.Environment;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentFactory;
import org.jtwig.resource.reference.ResourceReference;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mstat.planner.model.Department;
import ru.yandex.market.mstat.planner.model.Employee;
import ru.yandex.market.mstat.planner.model.EmployeeRole;
import ru.yandex.market.mstat.planner.model.Project;
import ru.yandex.market.mstat.planner.model.ProjectColor;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.model.RequestStatus;
import ru.yandex.market.mstat.planner.model.User;
import ru.yandex.market.mstat.planner.model.UserPlannerRole;
import ru.yandex.market.mstat.planner.util.RestUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.yandex.market.mstat.planner.util.RestUtil.toJson;

public class HtmlCubesControllerTest {

    @Test
    @Ignore
    public void t() {
         EnvironmentConfiguration configuration = new DefaultEnvironmentConfiguration();
        EnvironmentFactory environmentFactory = new EnvironmentFactory();
         Environment environment = environmentFactory.create(configuration);

 // Resource
//         ResourceReference resource = new ResourceReference(
//             ResourceReference.CLASSPATH,
//             "/project.twig"
//         );
        ResourceReference resource = new ResourceReference(
            ResourceReference.STRING,
            "{% set empl_relocation = {'project': {'colors': {}}} %}" +
                "{{ empl_relocation.project.colors.keySet() }}");

 // Template

         JtwigTemplate jtwigTemplate = new JtwigTemplate(environment, resource);

 // Model


        JtwigModel model = JtwigModel.newModel()
            .with("a", BigDecimal.valueOf(1.0001))
            .with("c", BigDecimal.valueOf(0.001))
            .with("b", BigDecimal.valueOf(1));
        // Output
         String output = jtwigTemplate.render(model);
        System.out.println(output);
    }


    @Test
    public void t2() {
        System.out.println(String.format("%.2f", new BigDecimal(1.342353456334645)));
    }


}

// dummy changes MARKETDX-830
