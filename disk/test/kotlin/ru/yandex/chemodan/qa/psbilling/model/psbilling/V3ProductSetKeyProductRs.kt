package ru.yandex.chemodan.qa.psbilling.model.psbilling

import com.fasterxml.jackson.annotation.JsonProperty
import ru.yandex.chemodan.qa.psbilling.model.psbilling.utils.Product

data class V3ProductSetKeyProductRs(
    @JsonProperty("items")
    val items: List<ProductWithPricesPojo>
) {
    fun toProduct4Test(): List<Product> {
        return items
            .filter { it.productIdFamily != "mail_pro_b2c_test_trial" }
            .flatMap { p ->
                p.prices
                    .sortedBy(ProductPricePojo::period) // костыль с сортировкой по цене
                    .map {
                        Product(
                            priceId = it.priceId,
                            name = p.title,
                            period = it.period,
                            amount = it.amount,
                            currency = it.currency,
                        )
                    }
            }
    }
}

data class ProductWithPricesPojo(
    @JsonProperty("product_id_family")
    val productIdFamily: String,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("prices")
    val prices: List<ProductPricePojo>
)
