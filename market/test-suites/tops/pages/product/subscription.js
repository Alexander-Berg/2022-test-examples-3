import {prepareSuite, makeSuite} from 'ginny';

import {productWithDefaultOffer, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
import SubscriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/Subscription';
import Subscription from '@self/platform/spec/page-objects/Subscription';

export default makeSuite('Подписка в футере', {
    environment: 'kadavr',
    feature: 'Рекламная подписка в футере',
    story: {
        async beforeEach() {
            await this.browser.setState('report', productWithDefaultOffer);
            await this.browser.yaOpenPage('touch:product', phoneProductRoute);
        },
        'Рекламная подписка': prepareSuite(SubscriptionSuite, {
            pageObjects: {
                subscription() {
                    return this.createPageObject(Subscription);
                },
            },
        }),
    },
});
