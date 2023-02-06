package ru.yandex.market.hrms.api.controller.scan;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.test.configurer.WarehouseApiConfigurer;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "BadgeScanControllerTest.before.csv")
class BadgeScanControllerTest extends AbstractApiTest {

    @Autowired
    WarehouseApiConfigurer warehouseApiConfigurer;

    @Test
    @DisplayName("Сканирование сотрудника, для которого известна связка wms-staff")
    void scanKnownEmployee() throws Exception {
        mockClock(LocalDate.of(2021, 2, 15));
        mockMvc.perform(scanRequest(
                """
                        {
                            "wmsLogin": "ЫщА-фТешЗЩМ93",
                            "domainId": 1,
                            "saveRequired": true
                        }
                        """,
                "b-bari"
        ))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                                        {
                                           "scannedAt": "2021-02-15T00:00:00",
                                           "employeeName": "Андрей",
                                           "employeePosition": "позиция",
                                           "wmsLogin": "sof-antipov93",
                                           "staffLogin": "antipov93",
                                           "success": true,
                                           "scannedBy": {
                                             "id": 5,
                                             "name": "Бари",
                                             "staff": "b-bari",
                                             "photoUrl": "/b-bari/avatar/100.jpg",
                                             "position": "Разработчик каруселей",
                                             "groupTitle": "",
                                             "fired": false,
                                             "newbie": false,
                                             "isOffice": false,
                                             "isOutstaff": false
                                           },
                                           "employee": {
                                             "id": 1,
                                             "name": "Андрей",
                                             "staff": "antipov93",
                                             "photoUrl": "/antipov93/avatar/100.jpg",
                                             "position": "позиция",
                                             "wms": "sof-antipov93",
                                             "groupTitle": "",
                                             "fired": false,
                                             "newbie": false,
                                             "isOffice": false,
                                             "isOutstaff": false
                                           }
                                         }
                                """,
                        true));

    }

    @Test
    @DisplayName("Сканирование сотрудника, для которого неизвестна связка wms-staff")
    void scanUnknownButFoundEmployee() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 10, 10, 14));
        warehouseApiConfigurer.mockFindStaffLogin("sof-timursha", "timursha");
        mockMvc.perform(scanRequest(
                """
                        {
                            "wmsLogin": "sof-timursha\\tpassword\\t",
                            "saveRequired": true
                        }
                        """,
                "andreevdm"
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Сотрудник не найден по wms-логину")))
                .andExpect(jsonPath("$.wmsLogin", is("sof-timursha")));
    }

    @Test
    @DisplayName("Сканирование сотрудника, для которого не удалось найти стафф-логин")
    void scanNotFoundEmployee() throws Exception {
        mockClock(LocalDate.of(2021, 2, 14));
        warehouseApiConfigurer.mockNotFoundStaffLogin("sof-ogonek");
        mockMvc.perform(scanRequest(
                """
                        {
                            "wmsLogin": "sof-ogonek",
                            "saveRequired": true
                        }
                        """,
                "sergey-fed"
        ))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                                {
                                  "scannedAt": "2021-02-14T00:00:00",
                                  "wmsLogin": "sof-ogonek",
                                  "message": "Сотрудник не найден по wms-логину",
                                  "success": false
                                }
                                """,
                        true
                ));
    }

    @Test
    @DisplayName("Сканирование сотрудника, для которого не удалось найти стафф-логин")
    void scanLoginsWithRussianLetters() throws Exception {
        mockClock(LocalDate.of(2021, 2, 14));
        warehouseApiConfigurer.mockNotFoundStaffLogin("ыща-щпщтул");
        mockMvc.perform(scanRequest(
                """
                        {
                            "wmsLogin": "ыща-щпщтул",
                            "saveRequired": true
                        }
                        """,
                "sergey-fed"
        ))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                                {
                                  "scannedAt": "2021-02-14T00:00:00",
                                  "wmsLogin": "sof-ogonek",
                                  "message": "Сотрудник не найден по wms-логину",
                                  "success": false
                                }
                                """,
                        true
                ));
    }

    @Test
    @DisplayName("Сканирование сотрудника, для которого не удалось найти стафф-логин, зато удалось аутстафф")
    void scanLoginsForOutstaff() throws Exception {
        mockClock(LocalDate.of(2021, 2, 14));
        warehouseApiConfigurer.mockNotFoundStaffLogin("sof-vlepihin");
        mockMvc.perform(scanRequest(
                """
                        {
                            "wmsLogin": "sof-vlepihin"
                        }
                        """,
                "nzhilik"
        ))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        """
                                {
                                  "scannedAt": "2021-02-14T00:00:00",
                                  "wmsLogin": "sof-vlepihin",
                                  "success": true,
                                  "employee": {
                                     "id": 1,
                                     "name": "Л Слава",
                                     "position": "Кладовщик",
                                     "wms": "sof-vlepihin",
                                     "isOutstaff": true,
                                     "active": false,
                                     "isActive": false,
                                   }
                                }
                                """,
                        true
                ));
    }

    private static MockHttpServletRequestBuilder scanRequest(String body, String by) {
        return MockMvcRequestBuilders.post("/lms/warehouse/scan")
                .cookie(new Cookie("yandex_login", by))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
