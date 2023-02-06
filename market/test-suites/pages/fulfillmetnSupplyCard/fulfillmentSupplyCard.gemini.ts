import {getTestingSupplier, makeShotCase, makeShotSuite} from 'spec/utils';
import {PLATFORM_TYPE} from '@yandex-market/b2b-core/shared/constants';
import App from './pageObjects/App';

const supplier = getTestingSupplier('rnpn.tst');

export default makeShotSuite({
    suiteName: 'Fulfillment Supplier Card',
    feature: 'Поставки',
    childSuites: [
        makeShotCase({
            suiteName: 'App',
            id: 'marketmbi-2352',
            issue: 'MARKETPARTNER-10061',
            environment: 'testing',
            page: {
                route: 'market-partner:html:fulfillment-supply-card:get',
                params: {
                    platformType: PLATFORM_TYPE.SUPPLIER,
                    campaignId: supplier.campaignId,
                    requestId: 12873,
                },
            },
            user: supplier.contacts.owner,
            selector: App.root,
        }),
    ],
});
