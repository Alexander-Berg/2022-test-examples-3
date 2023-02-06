import schema from 'js-schema';
import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createFilter, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import nodeConfig from '@self/platform/configs/current/node';

import {
    state as cutpriceOfferState,
    route as cutpriceOfferRoute,
} from '@self/platform/spec/hermione/fixtures/cutprice/cutpriceOffer';

import MetricaVisibleSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/visible';
import DiscountFilterPageObject from '@self/platform/components/DiscountFilter/__pageObject__';

export default makeSuite('Метрика.', {
    story: mergeSuites(
        makeSuite('Видимость фильтра уценки.', {
            environment: 'kadavr',
            story: prepareSuite(MetricaVisibleSuite, {
                meta: {
                    id: 'marketfront-3303',
                    issue: 'MARKETVERSTKA-33126',
                },
                params: {
                    expectedGoalName: 'product-page_discount-filter_visible',
                    counterId: nodeConfig.yaMetrika.market.id,
                    waitingGoalTimeout: 6000,
                    selector: DiscountFilterPageObject.root,
                    payloadSchema: schema({
                        reqId: String,
                    }),
                },
                hooks: {
                    async beforeEach() {
                        // Для того, чтобы фильтры отображались на странице
                        // необходимо установить флаг hasBoolNo в положение true
                        // На странице поиска же наоборот
                        const cutpriceOfferStatePatched = mergeState([
                            cutpriceOfferState,
                            createFilter({
                                type: 'boolean',
                                name: 'Состояние товара',
                                hasBoolNo: true,
                            }, 'good-state'),
                        ]);
                        await this.browser.setState('report', cutpriceOfferStatePatched);

                        return this.browser.yaOpenPage('market:product', cutpriceOfferRoute);
                    },
                },
            }),
        })
    ),
});
