import {makeSuite, prepareSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {modelPageRoute} from '@self/project/src/spec/hermione/fixtures/express';
import DefaultOfferWidgetSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOfferWidget';

export default makeSuite('Модель со вторым экспресс оффером', {
    environment: 'kadavr',
    story: prepareSuite(DefaultOfferWidgetSuite, {
        params: {
            pageId: PAGE_IDS_COMMON.YANDEX_MARKET_PRODUCT,
            pageRoute: modelPageRoute,
        },
    }),
});
