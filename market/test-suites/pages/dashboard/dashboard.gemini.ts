import {makeShotCase, makeShotSuite, getTestingShop} from 'spec/utils';
import {PLATFORM_TYPE} from '@yandex-market/b2b-core/shared/constants';
import App from './pageObjects/App';

const shop = getTestingShop('SMB_offer_automation');

export default makeShotSuite({
    suiteName: 'Dashboard.',
    feature: 'Dashboard',
    childSuites: [
        makeShotCase({
            suiteName: 'SMB. Dahboard View',
            id: 'marketmbi-5084',
            issue: 'MARKETPARTNER-18005',
            environment: 'testing',
            selector: App.accountBlock,
            page: {
                route: 'market-partner:html:shops-dashboard:get',
                params: {
                    campaignId: shop.campaignId,
                    platformType: PLATFORM_TYPE.SHOP,
                },
            },
            user: shop.contacts.owner,
        }),
    ],
});
