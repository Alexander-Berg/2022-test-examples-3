import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// suites
import digitalProductHeaderShowing from './digitalProductHeaderShowing';
import groupHeaderProductRemove from './groupHeaderProductRemoving';
import pharmaSubgroupOfDeliveryWithOthers from './pharmaSubgroupOfDeliveryWithOthers';
import pharmaOnlyIsNotSubgroup from './pharmaOnlyIsNotSubgroup';

export default makeSuite('Группировка товаров в корзине.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(digitalProductHeaderShowing, {
            meta: {
                id: 'marketfront-5288',
                issue: 'MARKETFRONT-70719',
            },
        }),
        prepareSuite(groupHeaderProductRemove, {
            meta: {
                id: 'marketfrontmarketfront-5289',
                issue: 'MARKETFRONT-70719',
            },
        }),
        prepareSuite(pharmaSubgroupOfDeliveryWithOthers, {
            meta: {
                id: 'marketfrontmarketfront-5371',
                issue: 'MARKETFRONT-70719',
            },
        }),
        prepareSuite(pharmaOnlyIsNotSubgroup, {
            meta: {
                id: 'marketfrontmarketfront-5372',
                issue: 'MARKETFRONT-70719',
            },
        })
    ),
});
