import {makeSuite, makeCase} from 'ginny';

const PRODUCT_ITEM_TYPE_VALUE = 'https://schema.org/Product';
const RATING_ITEM_TYPE_VALUE = 'https://schema.org/AggregateRating';
const AGGREGATE_OFFER_ITEM_TYPE_VALUE = 'https://schema.org/AggregateOffer';
const RATING_ITEM_PROP_VALUE = 'aggregateRating';
const AGGREGATE_OFFER_ITEM_PROP_VALUE = 'offers';
const BEST_RATING_VALUE = 5;
const WORST_RATING_VALUE = 1;
const PRICE_CURRENCY = 'RUR';

/**
 * Тесты на разметку schema.org товара.
 * @property {PageObject.SchemaOrgProduct} this.schemaOrgProduct
 * @property {PageObject.SchemaOrgAggregateRating} this.schemaOrgAggregateRating
 * @property {PageObject.SchemaOrgAggregateOffer} this.schemaOrgAggregateOffer
 */
export default makeSuite('Schema.org для товара.', {
    feature: 'SEO',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'главный элемент разметки товара содержит необходимые атрибуты': makeCase({
                id: 'marketfront-2454',
                issue: 'MARKETVERSTKA-28900',
                async test() {
                    const {schemaOrgProduct} = this;

                    // INFO: тут код исполняется последовательно для формирования читаемого allure-отчета
                    const schemaOrgAttrs = {
                        itemscope: await schemaOrgProduct.getItemScopeFromElem(),
                        itemtype: await schemaOrgProduct.getItemTypeFromElem(),
                    };

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemscope',
                        'true',
                        'На главном элементе установлен атрибут "itemscope"'
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemtype',
                        PRODUCT_ITEM_TYPE_VALUE,
                        `На главном элементе установлен атрибут со значением "${PRODUCT_ITEM_TYPE_VALUE}".`
                    );
                },
            }),

            'содержит в разметке товара всю необходимую информацию.': makeCase({
                id: 'marketfront-2452',
                issue: 'MARKETVERSTKA-28898',
                params: {
                    expectedCategoryName: 'Имя категории, к торой пренадлежит товар.',
                    expectedBrandName: 'Имя бренда, к которому пренадлежит товар.',
                    expectedUrlPath: 'Часть ссылки на страницу товара без указания домена.',
                    expectedProductName: 'Имя товара.',
                },
                async test() {
                    const {schemaOrgProduct, params} = this;
                    const {expectedCategoryName, expectedBrandName, expectedProductName, expectedUrlPath} = params;

                    // INFO: тут код исполняется последовательно для формирования читаемого allure-отчета
                    const productSchemaData = {
                        description: await schemaOrgProduct.getDescriptionFromMeta(),
                        category: await schemaOrgProduct.getCategoryFromMeta(),
                        brand: await schemaOrgProduct.getBrandFromMeta(),
                        name: await schemaOrgProduct.getNameFromMeta(),
                        url: await schemaOrgProduct.getUrlFromMeta(),
                    };

                    await this.expect(productSchemaData).to.have.own.property(
                        'category',
                        expectedCategoryName,
                        `Разметка содержит имя категории товара: "${expectedCategoryName}"`
                    );

                    await this.expect(productSchemaData).to.have.own.property(
                        'brand',
                        expectedBrandName,
                        `Разметка содержит имя бренда товара: "${expectedBrandName}"`
                    );

                    await this.expect(productSchemaData).to.have.own.property(
                        'name',
                        expectedProductName,
                        `Разметка содержит имя товара: "${expectedProductName}"`
                    );

                    await this.expect(
                        productSchemaData.url,
                        `Разметка содержит ссылку на товар с указанным путем: "${expectedUrlPath}"`
                    )
                        .to.be.link(
                            {pathname: expectedUrlPath}, {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );

                    await this.expect(productSchemaData).to.have.own.property('description')
                        .to.have.lengthOf.above(0)
                        .not.be.equal('NaN', 'Описание товара не должно быть равно "NaN"')
                        .not.be.equal('true', 'Описание товара не должно быть равно "true"')
                        .not.be.equal('false', 'Описание товара не должно быть равно "false"')
                        .not.be.equal('undefined', 'Описание товара не должно быть равно "undefined"')
                        .not.be.equal('null', 'Описание товара не должно быть равно "null"');
                },
            }),

            'содержит разметку рейтинга товара c верными атрибутами.': makeCase({
                id: 'marketfront-2454',
                issue: 'MARKETVERSTKA-28900',
                async test() {
                    const {schemaOrgAggregateRating} = this;

                    // INFO: тут код исполняется последовательно для формирования читаемого allure-отчета
                    const schemaOrgAttrs = {
                        itemscope: await schemaOrgAggregateRating.getItemScopeFromElem(),
                        itemtype: await schemaOrgAggregateRating.getItemTypeFromElem(),
                        itemprop: await schemaOrgAggregateRating.getItemPropFromElem(),
                    };

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemscope',
                        'true',
                        'На главном элементе установлен атрибут "itemscope"'
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemtype',
                        RATING_ITEM_TYPE_VALUE,
                        `На главном элементе установлен атрибут со значением "${RATING_ITEM_TYPE_VALUE}".`
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemprop',
                        RATING_ITEM_PROP_VALUE,
                        `На главном элементе установлен атрибут со значением "${RATING_ITEM_PROP_VALUE}".`
                    );
                },
            }),

            'содержит в разметке рейтинга товара всю необходимую информацию.': makeCase({
                id: 'marketfront-2454',
                issue: 'MARKETVERSTKA-28900',
                async test() {
                    const {schemaOrgAggregateRating} = this;

                    // INFO: тут код исполняется последовательно для формирования читаемого allure-отчета
                    const ratingData = {
                        ratingValue: await schemaOrgAggregateRating.getRatingValueFromMeta(),
                        bestRating: await schemaOrgAggregateRating.getBestRatingFromMeta(),
                        worstRating: await schemaOrgAggregateRating.getWorstRatingFromMeta(),
                        ratingCount: await schemaOrgAggregateRating.getRatingCountFromMeta(),
                        reviewCount: await schemaOrgAggregateRating.getReviewCountFromMeta(),
                    };

                    await this.expect(ratingData).to.have.own.property(
                        'bestRating',
                        BEST_RATING_VALUE,
                        `Лучшее значение рейтинга равно "${BEST_RATING_VALUE}".`
                    );

                    await this.expect(ratingData).to.have.own.property(
                        'worstRating',
                        WORST_RATING_VALUE,
                        `Худшее значение рейтинга равно "${WORST_RATING_VALUE}".`
                    );

                    await this.expect(ratingData).to.have.own.property('ratingValue')
                        .within(
                            ratingData.worstRating,
                            ratingData.bestRating,
                            `Значение рейтинга должно быть между "${WORST_RATING_VALUE}" и "${BEST_RATING_VALUE}"`
                        );

                    await this.expect(ratingData)
                        .to.have.own.property('ratingCount')
                        .a('number', 'Рейтинг должен быть задан числом.');

                    await this.expect(ratingData)
                        .to.have.own.property('reviewCount')
                        .a('number', 'Кол-во голосов должно быть задано числом.');
                },
            }),

            'содержит разметку оффера для товара c верными атрибутами.': makeCase({
                id: 'marketfront-2453',
                issue: 'MARKETVERSTKA-28899',
                async test() {
                    const {schemaOrgAggregateOffer} = this;

                    // INFO: тут код исполняется последовательно для формирования читаемого allure-отчета
                    const schemaOrgAttrs = {
                        itemscope: await schemaOrgAggregateOffer.getItemScopeFromElem(),
                        itemtype: await schemaOrgAggregateOffer.getItemTypeFromElem(),
                        itemprop: await schemaOrgAggregateOffer.getItemPropFromElem(),
                    };

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemscope',
                        'true',
                        'На главном элементе установлен атрибут "itemscope"'
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemtype',
                        AGGREGATE_OFFER_ITEM_TYPE_VALUE,
                        'На главном элементе установлен атрибут со значением ' +
                        `"${AGGREGATE_OFFER_ITEM_TYPE_VALUE}".`
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemprop',
                        AGGREGATE_OFFER_ITEM_PROP_VALUE,
                        'На главном элементе установлен атрибут со значением ' +
                        `"${AGGREGATE_OFFER_ITEM_PROP_VALUE}".`
                    );
                },
            }),

            'содержит в разметке оффера для товара всю необходимую информацию.': makeCase({
                id: 'marketfront-2453',
                issue: 'MARKETVERSTKA-28899',
                async test() {
                    const {schemaOrgAggregateOffer} = this;

                    // INFO: тут код исполняется последовательно для формирования читаемого allure-отчета
                    const offerData = {
                        offerCount: await schemaOrgAggregateOffer.getOfferCountFromMeta(),
                        highPrice: await schemaOrgAggregateOffer.getHighPriceFromMeta(),
                        lowPrice: await schemaOrgAggregateOffer.getLowPriceFromMeta(),
                        priceCurrency: await schemaOrgAggregateOffer.getPriceCurrencyFromMeta(),
                    };

                    await this.expect(offerData).to.have.own.property(
                        'priceCurrency',
                        PRICE_CURRENCY,
                        `Значение валюты равно "${PRICE_CURRENCY}".`
                    );

                    await this.expect(offerData)
                        .to.have.own.property('offerCount')
                        .a('number', 'Кол-во доступных офферов должно быть задано числом.');

                    await this.expect(offerData)
                        .to.have.own.property('highPrice')
                        .a('number', 'Значение наибольшей доступной цены должно быть задано числом.');

                    await this.expect(offerData)
                        .to.have.own.property('lowPrice')
                        .a('number', 'Значение наименьшей доступной цены должно быть задано числом.');

                    await this.expect(offerData.highPrice >= offerData.lowPrice).to.be.equal(
                        true,
                        `Значение наибольшей цены "${offerData.highPrice}" не должно быть меньше ` +
                        `значения наименьшей цены "${offerData.lowPrice}".`
                    );
                },
            }),
        },
    },
});
