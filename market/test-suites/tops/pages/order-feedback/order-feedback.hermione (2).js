import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import feedbackForm from '@self/root/src/spec/hermione/test-suites/blocks/orderFeedback/feedbackForm';
import {region} from '@self/root/src/spec/hermione/configs/geo';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Отзыв на заказ', {
    environment: 'kadavr',
    defaultParams: {
        pageId: PAGE_IDS_COMMON.ORDER_FEEDBACK,
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        prepareSuite(feedbackForm, {})
    ),
});
