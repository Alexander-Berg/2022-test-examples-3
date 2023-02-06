import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {offerMock, skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

const offer = createOffer(offerMock, offerMock.wareId);
const reportState = mergeState([offer]);
const sku = {
    ...skuMock,
    offers: {
        items: [offerMock],
    },
};

export async function prepareState(bonus) {
    const carts = [
        buildCheckouterBucket({
            items: [{
                skuMock,
                offerMock,
                count: 1,
            }],
        }),
    ];

    await this.browser.yaScenario(
        this,
        prepareMultiCartState,
        carts,
        {existingReportState: reportState}
    );

    return this.browser.yaScenario(
        this,
        'cart.prepareCartPageWithBonus',
        {
            items: [{
                skuId: skuMock.id,
                offerId: offerMock.wareId,
                count: 1,
            }],
            region: this.params.region,
            bonuses: {
                applicableCoins: bonus,
            },
            reportSkus: [sku],
        }
    );
}
