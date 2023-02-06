package ru.yandex.chemodan.qa.psbilling.extensions

import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import ru.yandex.chemodan.qa.psbilling.client.PsBillingClient
import ru.yandex.chemodan.qa.psbilling.model.psbilling.V3ProductSetKeyProductRs
import ru.yandex.chemodan.qa.psbilling.model.psbilling.utils.Product
import java.util.stream.Stream

class AnonymousProductSetTestTemplate : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        return true
    }

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        return PsBillingClient.v3ProductSetKeyProduct("mail_pro_b2c")
            .`as`(V3ProductSetKeyProductRs::class.java)
            .toProduct4Test()
            .map { n -> invocationContext(n) }
            .stream()
    }

    private fun invocationContext(product: Product): TestTemplateInvocationContext {
        return object : TestTemplateInvocationContext {
            override fun getDisplayName(invocationIndex: Int): String {
                return "Покупка продукта ${product.name} на ${product.period}"
            }

            override fun getAdditionalExtensions(): List<Extension> {
                return listOf(object : ParameterResolver {
                    override fun supportsParameter(paramCtx: ParameterContext, extCtx: ExtensionContext?): Boolean {
                        return paramCtx.parameter.type == Product::class.java
                    }

                    override fun resolveParameter(paramCtx: ParameterContext?, extCtx: ExtensionContext?): Any {
                        return product
                    }
                })
            }
        }
    }
}
