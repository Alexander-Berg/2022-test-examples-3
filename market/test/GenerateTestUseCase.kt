package ru.yandex.market.tpl.e2e.domain.feature.test

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import kotlinx.coroutines.runBlocking
import ru.yandex.market.tpl.e2e.data.feature.testcase.StepDto
import ru.yandex.market.tpl.e2e.data.feature.testcase.TestCaseDto
import ru.yandex.market.tpl.e2e.domain.feature.issue.GetIssueUseCase
import ru.yandex.market.tpl.e2e.domain.feature.testcase.GetTestCaseUseCase

class GenerateTestUseCase(
    private val getTestCaseUseCase: GetTestCaseUseCase,
    getIssueUseCase: GetIssueUseCase,
    configuration: Configuration,
) {
    private val issue = getIssueUseCase.fromBranch(configuration.branch)

    fun buildTestFile(testCaseId: String): FileSpec {
        return FileSpec.builder("ru.yandex.market.tpl.courier.test", PLACEHOLDER)
            .addType(buildClass(testCaseId))
            .build()
    }

    private fun buildClass(testCaseId: String): TypeSpec {
        val testCaseDto = runBlocking { getTestCaseUseCase.getTestCase(testCaseId) }
        val stepFunctions = buildStepFunctions(testCaseDto)

        return TypeSpec.classBuilder(PLACEHOLDER)
            .addPreconditionsComment(testCaseDto)
            .addTestClassAnnotations()
            .addInstrumentedTestRule()
            .addMainTestFunction(testCaseId, testCaseDto, stepFunctions)
            .apply {
                stepFunctions.forEach(::addFunction)
            }
            .build()
    }

    private fun TypeSpec.Builder.addPreconditionsComment(testCaseDto: TestCaseDto): TypeSpec.Builder {
        return addKdoc(buildCodeBlock {
            addStatement("Предусловия:")
            add(testCaseDto.preconditions)
        })
    }

    private fun TypeSpec.Builder.addTestClassAnnotations(): TypeSpec.Builder {
        return addAnnotation(
            AnnotationSpec.builder(ClassName("org.junit.runner", "RunWith"))
                .addMember(
                    "%M::class",
                    MemberName("io.qameta.allure.android.runners", "AllureAndroidJUnit4")
                )
                .build()
        )
            .addAnnotation(ClassName("androidx.test.filters", "LargeTest"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("io.qameta.allure.kotlin", "Epic"))
                    .addMember("%S", PLACEHOLDER)
                    .build()
            )
            .addAnnotation(
                AnnotationSpec.builder(ClassName("io.qameta.allure.kotlin.junit4", "DisplayName"))
                    .addMember("%S", PLACEHOLDER)
                    .build()
            )
    }

    private fun TypeSpec.Builder.addInstrumentedTestRule(): TypeSpec.Builder {
        return addProperty(
            PropertySpec.builder("rule", ClassName("org.junit.rules", "TestRule"))
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("org.junit", "Rule"))
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
                        .build()
                )
                .initializer(
                    "%M",
                    MemberName("ru.yandex.market.tpl.courier.arch.ext", "instrumentedTestRule")
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addMainTestFunction(
        testCaseId: String,
        testCaseDto: TestCaseDto,
        stepFunctions: List<FunSpec>,
    ): TypeSpec.Builder {
        return addFunction(
            FunSpec.builder(PLACEHOLDER)
                .addAnnotation(ClassName("org.junit", "Test"))
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("io.qameta.allure.kotlin", "Issue"))
                        .addMember("%S", issue)
                        .build()
                )
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("io.qameta.allure.kotlin", "TmsLink"))
                        .addMember("%S", testCaseId)
                        .build()
                )
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("io.qameta.allure.kotlin", "Story"))
                        .addMember("%S", PLACEHOLDER)
                        .build()
                )
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("io.qameta.allure.kotlin.junit4", "DisplayName"))
                        .addMember("%S", testCaseDto.name)
                        .build()
                )
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("io.qameta.allure.kotlin", "Description"))
                        .addMember("%S", PLACEHOLDER)
                        .build()
                )
                .apply {
                    stepFunctions.forEach {
                        addStatement("%N()", it)
                    }
                }
                .build()
        )
    }

    private fun buildStepFunctions(testCaseDto: TestCaseDto): List<FunSpec> =
        testCaseDto.stepsExpects.mapIndexed(::buildStepFunction)

    private fun buildStepFunction(index: Int, stepDto: StepDto): FunSpec {
        return FunSpec.builder("$PLACEHOLDER${index + 1}")
            .addAnnotation(
                AnnotationSpec.builder(ClassName("io.qameta.allure.kotlin", "Step"))
                    .addMember("%S", stepDto.step)
                    .build()
            )
            .addComment(PLACEHOLDER)
            .addModifiers(KModifier.PRIVATE)
            .apply {
                stepDto.expect.split('\n').forEach {
                    beginControlFlow(
                        "%M(%S) {",
                        MemberName("io.qameta.allure.kotlin.Allure", "step"),
                        it.trim(' ', '-'),
                    )
                    addComment(PLACEHOLDER)
                    endControlFlow()
                }
            }
            .build()
    }

    data class Configuration(val branch: String)

    companion object {
        const val PLACEHOLDER = "TODO"
    }
}