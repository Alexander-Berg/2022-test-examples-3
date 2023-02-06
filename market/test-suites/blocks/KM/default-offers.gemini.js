import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';

export default {
    suiteName: 'DefaultOffers',
    selector: DefaultOffer.root,
    capture(actions) {
        actions.waitForElementToShow(DefaultOffer.root, 5000);
    },
};
