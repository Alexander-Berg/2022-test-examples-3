import {makeSuite, makeCase} from 'ginny';

const OFFER_ITEM_TYPE_VALUE = 'https://schema.org/Offer';
const OFFER_ITEM_PROP_VALUE = 'offers';
const PRICE_CURRENCY = 'RUR';

/**
 * Тесты на разметку schema.org оффера.
 * @property {PageObject.SchemaOrgOffer} this.schemaOrgOffer
 */
export default makeSuite('Schema.org для оффера.', {
    feature: 'SEO',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'главный элемент разметки оффера содержит необходимые атрибуты': makeCase({
                id: 'marketfront-2451',
                issue: 'MARKETVERSTKA-28902',
                async test() {
                    const {schemaOrgOffer} = this;

                    // INFO: тут специально последовательно для читаемого  allure-отчета
                    const schemaOrgAttrs = {
                        itemscope: await schemaOrgOffer.getItemScopeFromElem(),
                        itemtype: await schemaOrgOffer.getItemTypeFromElem(),
                        itemprop: await schemaOrgOffer.getItemPropFromElem(),
                    };

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemscope',
                        'true',
                        'На главном элементе установлен атрибут "itemscope"'
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemtype',
                        OFFER_ITEM_TYPE_VALUE,
                        `На главном элементе установлен атрибут со значением "${OFFER_ITEM_TYPE_VALUE}".`
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemprop',
                        OFFER_ITEM_PROP_VALUE,
                        `На главном элементе установлен атрибут со значением "${OFFER_ITEM_PROP_VALUE}".`
                    );
                },
            }),

            'содержит разметку оффера c верными атрибутами.': makeCase({
                id: 'marketfront-2451',
                issue: 'MARKETVERSTKA-28902',
                async test() {
                    const {schemaOrgOffer} = this;

                    // INFO: тут специально последовательно для читаемого  allure-отчета
                    const schemaOrgAttrs = {
                        price: await schemaOrgOffer.getPriceFromMeta(),
                        priceCurrency: await schemaOrgOffer.getPriceCurrencyFromMeta(),
                    };

                    await this.expect(schemaOrgAttrs)
                        .to.have.own.property('price')
                        .a('number', 'Значение цены должно быть задано числом.');

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'priceCurrency',
                        PRICE_CURRENCY,
                        `Значение валюты равно "${PRICE_CURRENCY}".`
                    );
                },
            }),
        },
    },
});
