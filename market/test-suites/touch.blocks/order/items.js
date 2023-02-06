// eslint-disable-next-line no-restricted-imports
import {
    curry, flow, get,
    head, map, sum,
} from 'lodash/fp';
import {
    makeCase,
    makeSuite,
} from 'ginny';

const TITLE_TEXT = 'Состав заказа';

const selectOrder = (collections, id) => collections.order[id];

const selectOrderItem = curry((collections, id) => collections.orderItem[id]);

const selectOrderItems = (collections, id) => flow([
    selectOrder,
    get('items'),
    map(selectOrderItem(collections)),
])(collections, id);

const selectOrderItemsCounts = (collections, id) => flow([
    selectOrderItems,
    map(get('count')),
])(collections, id);

const selectOrderItemsCount = (collections, id) => flow([
    selectOrderItemsCounts,
    sum,
])(collections, id);

/**
 * Получает ссылку на изображение элемента заказа. Обратите внимание, что из ссылки вида:
 * https://example.com/foo.png
 * делает:
 * https://example.com/foo.png75x75
 */
const getOrderItemPictureUrl = flow([
    get('pictures'),
    head,
    get('url'),
    url => `${url}75x75`,
]);

const selectOrderItemsPicturesUrls = (collections, id) => flow([
    selectOrderItems,
    map(getOrderItemPictureUrl),
])(collections, id);

const selectOrderItemsPrices = (collections, id) => flow([
    selectOrderItems,
    map(get('buyerPrice')),
    map(price => `${price} ₽`),
])(collections, id);

const selectOrderItemsTitles = (collections, id) => flow([
    selectOrderItems,
    map(get('offerName')),
])(collections, id);

/**
 * Тесты на обыкновенный заказ.
 * @param {PageObject.OrderCard} orderCard - карточка заказа (для списка и для отдельного заказа они разные),
 */
module.exports = makeSuite('Состав заказа.', {
    feature: 'Состав заказа',
    params: {
        expectedOrderId: 'ID заказа, состав которого должен быть отображён',
        expectedOrderCollections: 'Коллекции заказа, состав которого должен быть отображён',
    },
    story: {
        Заголовок: {
            'по умолчанию': {
                [`содержит подпись "${TITLE_TEXT}" с правильным количеством товаров`]: makeCase({
                    id: 'bluemarket-225',
                    issue: 'BLUEMARKET-3974',
                    environment: 'kadavr',
                    async test() {
                        const expectedItemsCount = selectOrderItemsCount(
                            this.params.expectedOrderCollections,
                            this.params.expectedOrderId
                        );
                        const expectedText = `${TITLE_TEXT}${expectedItemsCount}`;
                        const actualText = await this.orderItems
                            .getTitleText();
                        return this.expect(actualText).to.be.equal(
                            expectedText,
                            'Заголовок должен быть правильным'
                        );
                    },
                }),
            },
        },
        'Элементы заказа': {
            'по умолчанию': {
                'правильно называются': makeCase({
                    id: 'bluemarket-225',
                    issue: 'BLUEMARKET-3974',
                    environment: 'kadavr',
                    async test() {
                        const expectedTitles = selectOrderItemsTitles(
                            this.params.expectedOrderCollections,
                            this.params.expectedOrderId
                        );
                        const actualTitles = await this.orderItems.getTitlesTexts();
                        await this.expect(actualTitles).to.deep.equal(
                            expectedTitles,
                            'Названия элементов заказа должны соответствовать ожидаемым'
                        );
                    },
                }),
                'имеют цену': makeCase({
                    id: 'bluemarket-225',
                    issue: 'BLUEMARKET-3974',
                    environment: 'kadavr',
                    async test() {
                        const expectedPrices = selectOrderItemsPrices(
                            this.params.expectedOrderCollections,
                            this.params.expectedOrderId
                        );
                        const actualPrices = await this.orderItems.getPrices();
                        await this.expect(actualPrices).to.deep.equal(
                            expectedPrices,
                            'Стоимости элементов заказа должны соответствовать ожидаемым'
                        );
                    },
                }),
                'содержат количество': makeCase({
                    id: 'bluemarket-225',
                    issue: 'BLUEMARKET-3974',
                    environment: 'kadavr',
                    async test() {
                        const expectedCounts = flow([
                            selectOrderItemsCounts,
                            map(count => `${count} шт.`),
                        ])(
                            this.params.expectedOrderCollections,
                            this.params.expectedOrderId
                        );
                        const actualPrices = await this.orderItems.getCounts();
                        await this.expect(actualPrices).to.deep.equal(
                            expectedCounts,
                            'Счётчики элементов заказа должны соответствовать ожидаемым'
                        );
                    },
                }),
                'оснащены картинками': makeCase({
                    id: 'bluemarket-225',
                    issue: 'BLUEMARKET-3974',
                    environment: 'kadavr',
                    async test() {
                        const expectedUrls = selectOrderItemsPicturesUrls(
                            this.params.expectedOrderCollections,
                            this.params.expectedOrderId
                        );
                        const actualUrls = await this.orderItems.getPicturesUrls();
                        await this.expect(actualUrls).to.deep.equal(
                            expectedUrls,
                            'Изображения элементов заказа должны соответствовать ожидаемым'
                        );
                    },
                }),
            },
        },
    },
});
