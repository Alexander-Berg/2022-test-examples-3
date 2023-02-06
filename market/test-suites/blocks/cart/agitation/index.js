import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {region} from '@self/root/src/spec/hermione/configs/geo';

// pageObjects
import CartCheckoutButton from
    '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import LoginAgitation
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/LoginAgitation/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

// suites
import closePopup from './closePopup';
import notNow from './notNow';
import backFromPassport from './backFromPassport';
import merge from './merge';

const singleCart = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export default makeSuite('Агитация.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-78266',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        isAuthWithPlugin: false,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
                    loginAgitationWrapper: () => this.createPageObject(LoginAgitation),
                    modal: () => this.createPageObject(PopupBase, {
                        parent: this.loginAgitationWrapper,
                    }),
                });

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    singleCart
                );

                await this.browser.yaScenario(
                    this,
                    prepareCartPageBySkuId,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        region: region['Москва'],
                    }
                );
            },
        },
        prepareSuite(closePopup, {}),
        prepareSuite(notNow, {}),
        prepareSuite(backFromPassport, {}),
        prepareSuite(merge, {})
    ),
});
