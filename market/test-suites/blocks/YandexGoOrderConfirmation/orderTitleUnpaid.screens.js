import {makeSuite, makeCase} from '@yandex-market/ginny';

//* @param {PageObject.YandexGoOrderTitleUnpaid} yandexGoOrderTitleUnpaid

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Заголовок неоплаченного заказа', {
    id: 'm-touch-3843',
    tags: ['Контур#Интеграции'],
    story: {
        'По-умолчанию': {
            'отображается верно': makeCase({
                async test() {
                    const yandexGoOrderTitleUnpaidSelector = await this.yandexGoOrderTitleUnpaid.getSelector();

                    return this.browser.assertView('plain', yandexGoOrderTitleUnpaidSelector);
                },
            }),
        },
    },
});
