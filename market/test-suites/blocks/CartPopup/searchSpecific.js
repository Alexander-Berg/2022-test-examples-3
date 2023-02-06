import {makeCase, makeSuite} from 'ginny';
import Notification from '@self/root/src/components/Notification/__pageObject';

export default makeSuite('Попап перехода в корзину', {
    params: {
        pageId: 'id страницы, которая будет открыта',
        routeParams: 'Параметры страницы для роутинга',
        reportMock: 'Данные для мокирования запроса в репорт',
    },
    story: {
        'Не появляется если в корзине уже есть товары': makeCase({
            async test() {
                const cartButtonSelector = await this.cartButton.getSelector();
                await this.browser.scroll(cartButtonSelector);

                await this.cartButton.click();

                return this.browser.waitForVisible(Notification.root, 5000);
            },
        }),
    },
});
