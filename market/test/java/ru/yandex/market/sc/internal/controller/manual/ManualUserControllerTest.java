package ru.yandex.market.sc.internal.controller.manual;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.model.InternalUserDto;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ScIntControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class ManualUserControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;

    private SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter(178L);
    }

    @Test
    @SneakyThrows
    void createUser() {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/users")
                                .header("Content-Type", "application/json")
                                .content("""
                                        {
                                                "uid": 1200000000000000,
                                                "sortingCenterId": 178,
                                                "name": "TEST",
                                                "email": "TEST@yandex-team.ru",
                                                "role": "SUPPORT"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().json(
                                """
                                                {
                                                    "deleted": false,
                                                    "uid": 1200000000000000,
                                                    "sortingCenterId": 178,
                                                    "name": "TEST",
                                                    "email": "TEST@yandex-team.ru",
                                                    "role": "SUPPORT"
                                                }
                                        """
                        )
                );
    }

    @Test
    @SneakyThrows
    void deleteUser() {
        var response = mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/users")
                                .header("Content-Type", "application/json")
                                .content("""
                                        {
                                                "uid": 1200000000000000,
                                                "sortingCenterId": 178,
                                                "name": "TEST",
                                                "email": "TEST@yandex-team.ru",
                                                "role": "SUPPORT"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var userDto = JacksonUtil.fromString(response, InternalUserDto.class);

        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/manual/users/" + userDto.getId())
                                .header("Content-Type", "application/json")
                                .content("""
                                                {
                                                    "deleted": true,
                                                    "uid": 1200000000000000,
                                                    "sortingCenterId": 178,
                                                    "name": "TEST",
                                                    "email": "TEST@yandex-team.ru",
                                                    "role": "SUPPORT"
                                                }
                                        """)
                )
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void getUsers() {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/users")
                                .header("Content-Type", "application/json")
                                .content("""
                                        {
                                                "uid": 1200000000000000,
                                                "sortingCenterId": 178,
                                                "name": "TEST",
                                                "email": "TEST@yandex-team.ru",
                                                "role": "SUPPORT"
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/manual/users?scId=178")
                                .header("Content-Type", "application/json")
                )
                .andExpect(
                        content().json(
                                """
                                                [
                                                    {
                                                        "deleted": false,
                                                        "uid": 1200000000000000,
                                                        "sortingCenterId": 178,
                                                        "name": "TEST",
                                                        "email": "TEST@yandex-team.ru",
                                                        "role": "SUPPORT"
                                                    }
                                                ]
                                        """
                        )
                );
    }

}
