import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';

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
                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока информации',
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
