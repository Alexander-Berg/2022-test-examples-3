import {makeSuite, makeCase} from 'ginny';
import {flow, castArray, head} from 'lodash';

const REVIEW_ITEM_TYPE_VALUE = 'https://schema.org/Review';
const RATING_ITEM_TYPE_VALUE = 'https://schema.org/Rating';
const REVIEW_ITEM_PROP_VALUE = 'review';
const RATING_ITEM_PROP_VALUE = 'reviewRating';
const BEST_RATING_VALUE = 5;
const WORST_RATING_VALUE = 1;
const REVIEW_AUTHOR_NAME_VALUE = 'someDisplay N.';
const DATE_PUBLISHED_FORMAT_REGEXP = /\d{4}-\d{2}-\d{2}/;
const DATE_PUBLISHED_FORMAT = 'YYYY-MM-DD';

const getFirstItem = flow(castArray, head);

/**
 * Тесты на разметку schema.org отзыва.
 * @property {PageObject.SchemaOrgReview} this.schemaOrgReview
 * @property {PageObject.SchemaOrgRating} this.schemaOrgRating
 */
export default makeSuite('Schema.org для отзыва.', {
    environment: 'kadavr',
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'имеет все нужные атрибуты на основном элементе разметки.': makeCase({
                id: 'marketfront-2448',
                issue: 'MARKETVERSTKA-28896',
                async test() {
                    const {schemaOrgReview} = this;

                    // INFO: Тут специально последовательно для последовательного формирования allure-отчета.
                    const schemaOrgAttrs = {
                        itemScope: await schemaOrgReview.getItemScopeFromElem().then(getFirstItem),
                        itemType: await schemaOrgReview.getItemTypeFromElem().then(getFirstItem),
                        itemProp: await schemaOrgReview.getItemPropFromElem().then(getFirstItem),
                    };

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemScope',
                        'true',
                        'На главном элементе установлен атрибут "itemscope"'
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemType',
                        REVIEW_ITEM_TYPE_VALUE,
                        `На главном элементе установлен атрибут "itemtype" со значением "${REVIEW_ITEM_TYPE_VALUE}".`
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemProp',
                        REVIEW_ITEM_PROP_VALUE,
                        `На главном элементе установлен атрибут "itemprop" со значением "${REVIEW_ITEM_PROP_VALUE}".`
                    );
                },
            }),
            'содержит разметку отзыва c верными атрибутами.': makeCase({
                id: 'marketfront-2448',
                issue: 'MARKETVERSTKA-28896',
                async test() {
                    const {schemaOrgReview} = this;

                    // INFO: Тут специально последовательно для последовательного формирования allure-отчета.
                    const schemaOrgAttrs = {
                        datePublished: await schemaOrgReview.getDatePublished().then(getFirstItem),
                        author: await schemaOrgReview.getAuthor().then(getFirstItem),
                        description: await schemaOrgReview.getDescription().then(getFirstItem),
                    };

                    const isDateFormatValid = DATE_PUBLISHED_FORMAT_REGEXP.test(schemaOrgAttrs.datePublished);

                    await this.expect(isDateFormatValid).be.equal(
                        true,
                        `Дата публикации должна быть задана и соответствовать формату: "${DATE_PUBLISHED_FORMAT}"`
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'author',
                        REVIEW_AUTHOR_NAME_VALUE,
                        `Атрибут "author" должен быть задан и равен: "${REVIEW_AUTHOR_NAME_VALUE}".`
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property('description')
                        .to.have.lengthOf.above(0)
                        .not.be.equal('NaN', 'Описание отзыва не должно быть равно "NaN"')
                        .not.be.equal('true', 'Описание отзыва не должно быть равно "true"')
                        .not.be.equal('false', 'Описание отзыва не должно быть равно "false"')
                        .not.be.equal('undefined', 'Описание отзыва не должно быть равно "undefined"')
                        .not.be.equal('null', 'Описание отзыва не должно быть равно "null"');
                },
            }),
            'содержит разметку рейтинга отзыва c верными атрибутами.': makeCase({
                id: 'marketfront-2449',
                issue: 'MARKETVERSTKA-28897',
                async test() {
                    const {schemaOrgRating} = this;

                    // INFO: Тут специально последовательно для последовательного формирования allure-отчета.
                    const schemaOrgAttrs = {
                        itemScope: await schemaOrgRating.getItemScopeFromElem().then(getFirstItem),
                        itemType: await schemaOrgRating.getItemTypeFromElem().then(getFirstItem),
                        itemProp: await schemaOrgRating.getItemPropFromElem().then(getFirstItem),
                    };

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemScope',
                        'true',
                        'На главном элементе установлен атрибут "itemscope"'
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemType',
                        RATING_ITEM_TYPE_VALUE,
                        `На главном элементе установлен атрибут "itemtype" со значением "${RATING_ITEM_TYPE_VALUE}".`
                    );

                    await this.expect(schemaOrgAttrs).to.have.own.property(
                        'itemProp',
                        RATING_ITEM_PROP_VALUE,
                        `На главном элементе установлен атрибут "itemprop" со значением "${RATING_ITEM_PROP_VALUE}".`
                    );
                },
            }),
            'содержит в разметке рейтинга отзыва всю необходимую информацию.': makeCase({
                id: 'marketfront-2449',
                issue: 'MARKETVERSTKA-28897',
                async test() {
                    const {schemaOrgRating} = this;

                    // INFO: Тут специально последовательно для последовательного формирования allure-отчета.
                    const ratingData = {
                        ratingValue: await schemaOrgRating.getRatingValue().then(getFirstItem),
                        bestRating: await schemaOrgRating.getBestRating().then(getFirstItem),
                    };

                    await this.expect(ratingData).to.have.own.property(
                        'bestRating',
                        BEST_RATING_VALUE,
                        `Лучшее значение рейтинга равно "${BEST_RATING_VALUE}".`
                    );

                    await this.expect(ratingData).to.have.own.property('ratingValue').within(
                        WORST_RATING_VALUE,
                        ratingData.bestRating,
                        `Значение рейтинга должно быть между "${WORST_RATING_VALUE}" и "${BEST_RATING_VALUE}"`
                    );
                },
            }),
        },
    },
});
