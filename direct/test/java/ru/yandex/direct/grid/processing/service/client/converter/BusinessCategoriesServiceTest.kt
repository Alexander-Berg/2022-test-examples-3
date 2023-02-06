package ru.yandex.direct.grid.processing.service.client.converter

import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class BusinessCategoriesServiceTest {

    @Autowired
    private lateinit var businessCategoriesService: BusinessCategoriesService

    @Test
    fun checkMappingCreated() {
        assertThat(businessCategoriesService.allDirectBusinessCategories, not(empty()))
    }
}
