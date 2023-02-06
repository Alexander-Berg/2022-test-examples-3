import {prepareSuite, makeSuite} from 'ginny';

import SubscriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/Subscription';
import Subscription from '@self/platform/spec/page-objects/Subscription';

import catalogerMock from './fixtures/cataloger-department.mock';

const DEPARTMENT_PAGE_PARAMS = {
    slug: 'elektronika',
    nid: 54440,
    hid: 198119,
};

export default makeSuite('Подписка в футере', {
    environment: 'kadavr',
    feature: 'Рекламная подписка в футере',
    story: {
        async beforeEach() {
            await this.browser.setState('Cataloger.tree', catalogerMock);
            await this.browser.yaOpenPage('touch:catalog', DEPARTMENT_PAGE_PARAMS);
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
