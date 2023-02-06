import {makeSuite, makeCase} from 'ginny';

const ORGANIZATION_ITEM_TYPE_VALUE = 'https://schema.org/Organization';
const POSTAL_ADDRESS_ITEM_TYPE_VALUE = 'https://schema.org/PostalAddress';
const RATING_ITEM_TYPE_VALUE = 'https://schema.org/AggregateRating';
const POSTAL_ADDRESS_ITEM_PROP_VALUE = 'address';
const RATING_ITEM_PROP_VALUE = 'aggregateRating';
const BEST_RATING_VALUE = 5;
const WORST_RATING_VALUE = 1;

/**
 * Тесты на разметку schema.org организации.
 * @property {PageObject.SchemaOrgOrganization} this.schemaOrgOrganization
 * @property {PageObject.SchemaOrgAggregateRating} this.schemaOrgAggregateRating
 * @property {PageObject.SchemaOrgPostalAddress} this.schemaOrgPostalAddress
 */
export default makeSuite('Schema.org для организации.', {
    environment: 'kadavr',
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'имеет все нужные атрибуты на основном элементе разметки.': makeCase({
                id: 'marketfront-2440',
                issue: 'MARKETVERSTKA-28894',
                async test() {
                    const schemaOrgAttrs = {
                        itemscope: await this.schemaOrgOrganization.getItemScopeFromElem(),
                        itemtype: await this.schemaOrgOrganization.getItemTypeFromElem(),
                    };

                    return this.expect(schemaOrgAttrs)
                        .to.have.own.property(
                            'itemscope',
                            'true',
                            'На главном элементе установлен атрибут "itemscope"'
                        )
                        .then(() => this.expect(schemaOrgAttrs)
                            .to.have.own.property(
                                'itemtype',
                                ORGANIZATION_ITEM_TYPE_VALUE,
                                `На главном элементе установлен атрибут со значением "${ORGANIZATION_ITEM_TYPE_VALUE}".`
                            )
                        );
                },
            }),
            'содержит имя организации.': makeCase({
                id: 'marketfront-2440',
                issue: 'MARKETVERSTKA-28894',
                params: {
                    expectedName: 'Ожидаемое название организации',
                },
                async test() {
                    const realName = await this.schemaOrgOrganization.getNameFromMeta();

                    return this.expect(realName).be.equal(
                        this.params.expectedName,
                        `Имя организации должно быть равно "${this.params.expectedName}"`
                    );
                },
            }),
            'содержит разметку адреса организации c верными атрибутами.': makeCase({
                id: 'marketfront-2444',
                issue: 'MARKETVERSTKA-28892',
                async test() {
                    const {schemaOrgPostalAddress: addressSchema} = this;

                    const schemaOrgAttrs = {
                        itemscope: await addressSchema.getItemScopeFromElem(),
                        itemtype: await addressSchema.getItemTypeFromElem(),
                        itemprop: await addressSchema.getItemPropFromElem(),
                    };

                    return this.expect(schemaOrgAttrs)
                        .to.have.own.property(
                            'itemscope',
                            'true',
                            'На главном элементе установлен атрибут "itemscope"'
                        )
                        .then(() => this.expect(schemaOrgAttrs)
                            .to.have.own.property(
                                'itemtype',
                                POSTAL_ADDRESS_ITEM_TYPE_VALUE,
                                `На главном элементе установлен атрибут со значением "${POSTAL_ADDRESS_ITEM_TYPE_VALUE}".`
                            )
                        )
                        .then(() => this.expect(schemaOrgAttrs)
                            .to.have.own.property(
                                'itemprop',
                                POSTAL_ADDRESS_ITEM_PROP_VALUE,
                                `На главном элементе установлен атрибут со значением "${POSTAL_ADDRESS_ITEM_PROP_VALUE}".`
                            )
                        );
                },
            }),
            'содержит в разметке адрес организации.': makeCase({
                id: 'marketfront-2444',
                issue: 'MARKETVERSTKA-28892',
                params: {
                    expectedAddress: 'Ожидаемое значение адреса организации',
                },
                async test() {
                    const realAddress = await this.schemaOrgPostalAddress.getStreetAddressFromMeta();

                    return this.expect(realAddress).be.equal(
                        this.params.expectedAddress,
                        `Адрес организации должен быть равен "${this.params.expectedAddress}"`
                    );
                },
            }),
            'содержит разметку рейтинга организации c верными атрибутами.': makeCase({
                id: 'marketfront-2443',
                issue: 'MARKETVERSTKA-28893',
                async test() {
                    const {schemaOrgAggregateRating: ratingSchema} = this;

                    const schemaOrgAttrs = {
                        itemscope: await ratingSchema.getItemScopeFromElem(),
                        itemtype: await ratingSchema.getItemTypeFromElem(),
                        itemprop: await ratingSchema.getItemPropFromElem(),
                    };

                    return this.expect(schemaOrgAttrs)
                        .to.have.own.property(
                            'itemscope',
                            'true',
                            'На главном элементе установлен атрибут "itemscope"'
                        )
                        .then(() => this.expect(schemaOrgAttrs)
                            .to.have.own.property(
                                'itemtype',
                                RATING_ITEM_TYPE_VALUE,
                                `На главном элементе установлен атрибут со значением "${RATING_ITEM_TYPE_VALUE}".`
                            )
                        )
                        .then(() => this.expect(schemaOrgAttrs)
                            .to.have.own.property(
                                'itemprop',
                                RATING_ITEM_PROP_VALUE,
                                `На главном элементе установлен атрибут со значением "${RATING_ITEM_PROP_VALUE}".`
                            )
                        );
                },
            }),
            'содержит в разметке рейтинга организации всю необходимую информацию.': makeCase({
                id: 'marketfront-2443',
                issue: 'MARKETVERSTKA-28893',
                async test() {
                    const {schemaOrgAggregateRating: ratingSchema} = this;

                    const ratingData = {
                        ratingValue: await ratingSchema.getRatingValueFromMeta(),
                        bestRating: await ratingSchema.getBestRatingFromMeta(),
                        worstRating: await ratingSchema.getWorstRatingFromMeta(),
                        ratingCount: await ratingSchema.getRatingCountFromMeta(),
                        reviewCount: await ratingSchema.getReviewCountFromMeta(),
                    };

                    return this.expect(ratingData)
                        .to.have.own.property(
                            'bestRating',
                            BEST_RATING_VALUE,
                            `Лучшее значение рейтинга равно "${BEST_RATING_VALUE}".`
                        )
                        .then(() => this.expect(ratingData)
                            .to.have.own.property(
                                'worstRating',
                                WORST_RATING_VALUE,
                                `Худшее значение рейтинга равно "${WORST_RATING_VALUE}".`
                            )
                        )
                        .then(() => this.expect(ratingData)
                            .to.have.own.property('ratingValue')
                            .within(
                                ratingData.worstRating,
                                ratingData.bestRating,
                                `Значение рейтинга должно быть между "${WORST_RATING_VALUE}" и "${BEST_RATING_VALUE}"`
                            )
                        )
                        .then(() => this.expect(ratingData)
                            .to.have.own.property('ratingCount')
                            .a('number', 'Рейтинг должен быть задан числом.')
                        )
                        .then(() => this.expect(ratingData)
                            .to.have.own.property('reviewCount')
                            .a('number', 'Кол-во голосов должно быть задано числом.')
                        );
                },
            }),
        },
    },
});
