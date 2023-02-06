import {makeSuite, makeCase} from '@yandex-market/ginny';

//* @param {PageObject.YandexGoOrderInfoUnpaid} yandexGoOrderInfoUnpaid

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Информация о неоплаченном заказе', {
    id: 'm-touch-3844',
    tags: ['Контур#Интеграции'],
    story: {
        'По-умолчанию': {
            'отображается верно': makeCase({
                async test() {
                    const yandexGoOrderInfoUnpaidSelector = await this.yandexGoOrderInfoUnpaid.getSelector();

                    return this.browser.assertView('plain', yandexGoOrderInfoUnpaidSelector);
                },
            }),
        },
    },
});
