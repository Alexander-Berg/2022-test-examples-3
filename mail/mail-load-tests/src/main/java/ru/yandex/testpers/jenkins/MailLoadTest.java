package ru.yandex.testpers.jenkins;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.qatools.properties.DefaultValue;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Required;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Aqua.Test
@Title("Запуск джобы в jenkins")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MailLoadTest {

    private static final String JOB_NAME = "wmi-complex-ubuntu";
    private static final URI TRANSFER = UriBuilder.fromPath("/view/MAIL/job/{job}/buildWithParameters")
        .scheme("https").host("jenkins-load.yandex-team.ru").port(443)
        .build(JOB_NAME);

    @Parameter("Тип теста")
    @Property("test.type")
    @Required
    public TestType testType;

    @Parameter("Номер пакета")
    @Property("verstka.package")
    @DefaultValue("")
    public String packageName;

    @Parameter("Куда результат в st")
    @Property("task")
    @DefaultValue("")
    public String task;

    @Before
    public void setUp() throws IllegalStateException {
        PropertyLoader.newInstance()
            .register(from -> Stream.of(TestType.values())
                    .filter(type -> StringUtils.equals(from, type.toString()))
                    .findFirst()
                    .<RuntimeException>orElseThrow(
                        () -> new IllegalStateException("Неизвестный тип " + from)
                    ),
                TestType.class)
            .populate(this);
    }

    @Test
    @Title("Запуск джобы")
    public void runJob() throws Exception {
        given()
            .filter(log())
            .formParam("token", "lunapi")
            .formParam("TYPE", testType)
            .formParam("PACKAGE", packageName)
            .formParam("TASK", task)
            .expect()
            .when()
            .post(TRANSFER);
    }

    enum TestType {
        U2709("u2709"),
        QUINN("quinn");

        private String value;

        TestType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
