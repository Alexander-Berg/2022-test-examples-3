
import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import CartTotalInformation from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';

import {creditWidgetSuite, creditWidgetWithoutSuite} from '@self/root/src/spec/hermione/test-suites/blocks/credits';
import {prepareStateCarts} from '@self/root/src/spec/hermione/scenarios/credit';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

export default makeSuite('Кредиты.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.yaScenario(this, prepareStateCarts);

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);

                this.setPageObjects({
                    orderInfo: () => this.createPageObject(CartTotalInformation),
                });
                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока информации о заказе',
                    () => this.orderInfo.waitForVisible()
                );
            },
        },
        prepareSuite(creditWidgetSuite, {
            meta: {
                id: 'bluemarket-4052',
            },
            params: {
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(creditWidgetWithoutSuite, {
            params: {
                isAuthWithPlugin: true,
            },
        })
    ),
});
