import {mergeSuites, makeSuite} from 'ginny';

import OfferServicesCart from '@self/root/src/spec/hermione/test-suites/blocks/offerServices/offerServicesCart';
import OfferServicesBnpl from '@self/platform/spec/hermione/test-suites/blocks/offerServices/offerServicesBnpl';
import OfferServicesCatalog from '@self/platform/spec/hermione/test-suites/blocks/offerServices/offerServicesCatalog';
import OfferServicesCheckout from '@self/platform/spec/hermione/test-suites/blocks/offerServices/offerServicesCheckout';
import OfferServicesOffer from '@self/platform/spec/hermione/test-suites/blocks/offerServices/offerServicesOffer';
import OfferServicesOrder from '@self/platform/spec/hermione/test-suites/blocks/offerServices/offerServicesOrder';
import OfferServicesProduct from '@self/platform/spec/hermione/test-suites/blocks/offerServices/offerServicesProduct';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Услуги', {
    environment: 'testing',
    story: mergeSuites(
        OfferServicesBnpl,
        OfferServicesCart,
        OfferServicesCatalog,
        OfferServicesCheckout,
        OfferServicesOffer,
        OfferServicesOrder,
        OfferServicesProduct
    ),
});
