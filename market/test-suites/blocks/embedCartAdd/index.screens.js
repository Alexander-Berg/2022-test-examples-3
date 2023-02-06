import {makeSuite, makeCase} from '@yandex-market/ginny';
import {waitForLoad, postMessage} from './utils';

export default makeSuite('Встраиваемое окно корзины. Проверка внешнего вида', {
    environment: 'kadavr',
    params: {
        offerId: 'ИД оффера, который добавляем в корзину',
    },
    story: {
        'Окно корзины не перекрывает никакой попап': makeCase({
            id: 'MARKETFRONT-57970',
            issue: 'MARKETFRONT-57970',
            async test() {
                await waitForLoad.call(this);

                await postMessage.call(this, {
                    event: 'addToCart',
                });

                await this.preloader.waitForHidden(5000);
                const viewSelector = await this.view.getSelector();

                return this.browser.assertView('full', viewSelector, {
                    compositeImage: true,
                });
            },
        }),
    },
});
