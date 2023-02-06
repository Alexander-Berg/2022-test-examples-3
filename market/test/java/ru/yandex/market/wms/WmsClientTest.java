package ru.yandex.market.wms;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.wms.WmsClientFactory;
import ru.yandex.market.hrms.test.configurer.WmsApiConfigurer;


class WmsClientTest extends AbstractCoreTest {

    @Autowired
    private WmsApiConfigurer wmsApiConfigurer;

    @Autowired
    private WmsClientFactory factory;
    private WmsClient wmsClient;

    @Value("${market-hrms.wms.url}")
    private URI endpoint;

    @BeforeEach
    public void init() {
        wmsClient = factory.create(endpoint);
    }

    @Test
    void findByLogin() {
        wmsApiConfigurer.mockAuthUsersResponse("""
                {
                  "limit": 20,
                  "offset": 0,
                  "total": 1,
                  "content": [
                    {
                      "userId": "0x562236532A0206FF206B959CC87D8611",
                      "login": "ad52",
                      "externLogin": "ad52",
                      "active": true,
                      "locale": "ru_RU",
                      "ssoUserName": "CN=AD52,OU=Users,OU=VMS,DC=mast,DC=local",
                      "fullyQualifiedId": "Тестовый пользователь",
                      "tenant": "INFOR",
                      "fullName": "AD52",
                      "emailAddress": null,
                      "roleNames": [
                        "Тестовая роль 1",
                        "Тестовая роль 2"
                      ],
                      "timezone": null
                    }
                  ]
                }
                """);

        User returnedUser = wmsClient.findByLogin("").orElseThrow();
        Assertions.assertEquals("0x562236532A0206FF206B959CC87D8611", returnedUser.getUserId());
        Assertions.assertEquals("ad52", returnedUser.getLogin());
        Assertions.assertEquals("AD52", returnedUser.getFullName());
        Assertions.assertEquals("CN=AD52,OU=Users,OU=VMS,DC=mast,DC=local", returnedUser.getSsoUserName());
        Assertions.assertIterableEquals(List.of("Тестовая роль 1", "Тестовая роль 2"), returnedUser.getRoleNames());
    }

    @Test
    void createUser() {
        String userId = "0x5613F6282A0206FF206B959C0DB886B0";
        wmsApiConfigurer.mockCreateUser("""
                {
                	"login": "ad52",
                	"email": "ad52_2@mast.local",
                	"fullName": "Тестовый пользователь",
                	"roleNames": [
                        "Тестовая роль 1",
                        "Тестовая роль 2"
                      ],
                	"taskManagerUser": {
                	    "gender": "MALE",
                	    "staffLogin": "staff/ad52",
                        "isOutstaff": true
                    }

                }
                """, userId);

        String id = wmsClient.createUser("ad52", "Тестовый пользователь", "ad52_2@mast.local",
                List.of("Тестовая роль 1", "Тестовая роль 2"), "male", "staff/ad52", true);
        Assertions.assertEquals(userId, id);
    }

    @Test
    void beginner() {
        wmsApiConfigurer.mockBeginner(
                """
                        {"users":
                            [
                                {
                                    "login": "sof-ad1",
                                    "beginnerEndTime": "2022-03-03T17:00:00.000Z"
                                },
                                {
                                    "login": "sof-ad2",
                                    "beginnerEndTime": "2022-04-04T17:00:00.000Z"
                                }
                            ]
                        }
                        """
        );

        wmsClient.updateBeginnerEndTime(List.of(
                new Beginner("sof-ad1", Instant.parse("2022-03-03T17:00:00.000Z")),
                new Beginner("sof-ad2", Instant.parse("2022-04-04T17:00:00.000Z"))
        ));
    }
}
