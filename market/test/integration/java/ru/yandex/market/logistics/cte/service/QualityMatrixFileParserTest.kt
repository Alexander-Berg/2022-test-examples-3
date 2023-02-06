package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType
import ru.yandex.market.logistics.cte.entity.group.QualityAttributeEntity
import ru.yandex.market.logistics.cte.repo.QualityAttributeRepository
import ru.yandex.market.logistics.cte.service.qualityparser.QualityMatrixFileParser
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.Objects
import ru.yandex.market.logistics.cte.client.enums.MatrixType


internal class QualityMatrixFileParserTest(
    @Autowired private val qualityMatrixFileParser: QualityMatrixFileParser,
    @Autowired private val qualityAttributeRepository: QualityAttributeRepository
) : IntegrationTest() {

    @Test
    @DatabaseSetup(
        "classpath:service/quality-matrix/duplicated_categories_attributes/quality_matrix_missing_categories_and_attributes.xml")
    fun parseWithMissingCategoriesAndAttributesAndDuplicatedCategories() {
        val input =
            requireNotNull(getFileUrl(
                "service/quality-matrix/duplicated_categories_attributes/quality_matrix_missing_categories_and_attributes.csv"))

        val file = createMultipartFileFromResource(input)
        val parse = qualityMatrixFileParser.parse(1, file, MatrixType.FULFILLMENT)

        assertions.assertThat(parse.hasErrors()).isTrue
        val errorMessage = parse.buildErrorMessage()

        assertions.assertThat(errorMessage).contains(
            "[{Id категории: 1, Id группы качеств: 0}, {Id категории: 99}] не найдены ",
            "Ошибка чтения на 2 строке",
            "Дублирование колонки атрибута",
            "Ошибка чтения на 4 строке. Ошибка: Дублирования категории с hid = 5",
            "Ошибка чтения на 5 строке. Ошибка: Дублирования категории с hid = 5"
        )
        println(errorMessage)
    }

    @Test
    @DatabaseSetup("classpath:service/quality-matrix/success_upload/quality_matrix_file_parser_categories.xml")
    fun parse() {
        val input =
            requireNotNull(getFileUrl("service/quality-matrix/success_upload/file.csv"))

        addQualityAttributes(
            "2.4.1, Разрывы упаковки/пленки более 1 см\n" +
                "2.4.2, Замятия углов и ребер свыше 1 см\n" +
                "2.4.3, Загрязнения потертости которые нельзя протереть или удалить\n" +
                "2.4.6, Следы влаги/намокания которые нельзя протереть салфеткой\n" +
                "2.4.7, Упаковка со следами вскрытия\n" +
                "2.4.8, Сквозные повреждения сильная деформация\n" +
                "2.5.1, Повреждена этикетка\n" +
                "2.5.2, Запах\n" +
                "2.5.3, Механические повреждения\n" +
                "2.5.4, Следы употребления/частично отсутствует\n" +
                "2.5.5, Акт НРП с товаром\n" +
                "2.6.1, Не включается/не заряжается\n" +
                "2.6.3, Некорректное отображение\n" +
                "2.6.5, Посторонний шум при работе\n" +
                "2.6.8, Другое (ввести вручную!)\n" +
                "2.6.2, Неправомерный возврат ДС"
        )

        val mockMultipartFile = createMultipartFileFromResource(input)
        val qualityGroupValues = qualityMatrixFileParser.parse(0, mockMultipartFile, MatrixType.FULFILLMENT)

        if (qualityGroupValues.hasErrors()) {
            println(qualityGroupValues.buildErrorMessage())
        }
        assertions.assertThat(qualityGroupValues.categories.size).isEqualTo(37)
    }

    private fun getFileUrl(filePath: String) = this.javaClass.classLoader.getResource(filePath)

    private fun addQualityAttributes(content: String) {
        content
            .split('\n')
            .forEach {
                buildQualityAttributeEntity(it)?.let { s ->
                    qualityAttributeRepository.save(s)
                }
            }
    }

    private fun buildQualityAttributeEntity(refIdCommaName: String): QualityAttributeEntity? {
        val values = refIdCommaName.split(',')

        if (values.size != 2) {
            return null
        }

        return QualityAttributeEntity().also {
            it.refId = values[0].trimEnd('.')
            it.name = values[1]
            it.title = values[1]
            it.attributeType = QualityAttributeType.ITEM
            it.description = values[1]
        }
    }

    private fun createMultipartFileFromResource(input: URL): MockMultipartFile {
        val file = File(Objects.requireNonNull(input).toURI())

        var content: ByteArray? = null
        try {
            content = file.readBytes()
        } catch (e: IOException) {
        }

        return MockMultipartFile(
            "file.csv",
            "file.csv",
            "text/plan",
            content
        )
    }
}
