package ru.yandex.market.wms.taskrouter.skills

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.taskrouter.config.BaseTest
import ru.yandex.market.wms.taskrouter.config.TestConfig

@Import(TestConfig::class)
class SkillsControllerTest : BaseTest() {

    @Test
    @DatabaseSetup("/skills/db/before.xml")
    fun listEmployeeSkills() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/admin-user-skills/skills/PICKING"),
            MockMvcResultMatchers.status().isOk, emptyMap(),
            "skills/response/list-response.json"
        )
    }

    @Test
    @DatabaseSetup("/skills/db/before.xml")
    fun listEmployeeSkillsWithFilters() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/admin-user-skills/skills/PICKING"),
            MockMvcResultMatchers.status().isOk,
            mapOf(
                "filter" to "userId==User2 or USERID==User1",
                "limit" to "1",
                "offset" to "1",
                "sort" to "userId",
                "order" to "DESC"
            ),
            "skills/response/list-response-filtered.json"
        )
    }

    @Test
    @DatabaseSetup("/skills/db/before.xml")
    fun listEmployeeSkillsWithFilteringBySkills() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/admin-user-skills/skills/PICKING"),
            MockMvcResultMatchers.status().isOk,
            mapOf(
                "filter" to "(SKILLS=='MEZ1',skills=='MEZ2')",
                "sort" to "USERID",
                "order" to "DESC"
            ),
            "skills/response/list-response-filtered-by-skills.json"
        )
    }

    @Test
    @DatabaseSetup("/skills/db/before.xml")
    @ExpectedDatabase(
        value = "/skills/db/after-update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateEmployeeSkills() {
        assertHttpCall(
            MockMvcRequestBuilders.put("/admin-user-skills/skills/PICKING"),
            MockMvcResultMatchers.status().isOk,
            "skills/request/update-request.json",
            "skills/response/update-response.json"
        )
    }

    @Test
    @DatabaseSetup("/skills/db/before.xml")
    @ExpectedDatabase(
        value = "/skills/db/after-set-skills-to-users.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun setSkillsToUsers() {
        assertHttpCall(
            MockMvcRequestBuilders.post("/admin-user-skills/set-skills-to-users/PICKING"),
            MockMvcResultMatchers.status().isOk,
            "skills/request/set-skills-to-users-request.json",
            "skills/response/set-skills-to-users-response.json"
        )
    }

    @Test
    @DatabaseSetup("/skills/db/before.xml")
    fun skillsList() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/admin-user-skills/skills-list"),
            MockMvcResultMatchers.status().isOk, emptyMap(),
            "skills/response/skills-list-response.json"
        )
    }
}
