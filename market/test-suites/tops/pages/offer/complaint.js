import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import ProductOrOfferComplaintSuite from '@self/platform/spec/hermione/test-suites//blocks/ProductOrOfferComplaint';
import OfferComplaintButton from '@self/platform/spec/page-objects/components/OfferComplaintButton';
import Notification from '@self/root/src/components/Notification/__pageObject';
import ComplaintForm from '@self/platform/spec/page-objects/components/ComplaintForm';
import ComplaintFormSubmitButton from '@self/platform/spec/page-objects/components/ComplaintForm/SubmitButton';
import ComplaintFormHeader from '@self/platform/spec/page-objects/components/ComplaintForm/Header';

const SHOP_INFO = {
    id: 123,
    name: 'test shop',
    slug: 'test-shop',
};

const OFFER_WARE_ID = '1';

export default makeSuite('Жалоба на оффер', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const offer = createOffer({
                    cpc: 'CPC',
                    shop: SHOP_INFO,
                }, OFFER_WARE_ID);
                await this.browser.setState('report', offer);
                await this.browser.yaOpenPage('touch:offer', {offerId: OFFER_WARE_ID});
            },
        },
        prepareSuite(ProductOrOfferComplaintSuite, {
            meta: {
                id: 'm-touch-3265',
                issue: 'MOBMARKET-9871',
            },
            hooks: {
                async beforeEach() {
                    await this.offerComplaintButton.waitForVisible();
                    await this.offerComplaintButton.clickOfferComplaintButton();
                },
            },
            params: {
                otherIndex: 7,
            },
            pageObjects: {
                offerComplaintButton() {
                    return this.createPageObject(OfferComplaintButton);
                },
                notification() {
                    return this.createPageObject(Notification);
                },
                complaintForm() {
                    return this.createPageObject(ComplaintForm);
                },
                complaintFormSubmitButton() {
                    return this.createPageObject(ComplaintFormSubmitButton);
                },
                complaintFormHeader() {
                    return this.createPageObject(ComplaintFormHeader);
                },
            },
        })
    ),
});

