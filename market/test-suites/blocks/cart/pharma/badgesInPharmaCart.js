import {makeCase, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import * as noPrescriptionFarma from '@self/root/src/spec/hermione/kadavr-mock/report/noPrescriptionFarma';
import CartOfferAvailabilityInfo from '@self/root/src/widgets/content/cart/CartList/components/CartOfferAvailabilityInfo/__pageObject';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';

const farmaCart = buildCheckouterBucket({
    cartIndex: 0,
    items: [{
        skuMock: farma.skuMock,
        offerMock: farma.offerMock,
        count: 1,
    },
    {
        skuMock: noPrescriptionFarma.skuMock,
        offerMock: noPrescriptionFarma.offerMock,
        count: 1,
    }],
});

module.exports = makeSuite('Наличие бейджа у рецептурного лекарства.', {
    environment: 'kadavr',
    id: 'marketfront-5838',
    issue: 'MARKETFRONT-81669',
    story: {
        async beforeEach() {
            this.setPageObjects({
                firstBadge: () => this.createPageObject(
                    CartOfferAvailabilityInfo,
                    {
                        root: `${CartItemGroup.firstGroup} ${CartOfferAvailabilityInfo.root}`,
                    }
                ),
                secondBadge: () => this.createPageObject(
                    CartOfferAvailabilityInfo,
                    {
                        root: `${CartItemGroup.secondGroup} ${CartOfferAvailabilityInfo.root}`,
                    }
                ),
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [farmaCart]
            );

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
            return this.browser.yaScenario(this, waitForCartActualization);
        },

        'У рецептурного препарата': {
            'под названием присутствует бейдж оранжевого цвета "Только самовывоз"': makeCase({
                async test() {
                    await this.firstBadge.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Надпись должна отображаться'
                        );
                    return this.firstBadge.getStatusText()
                        .should.eventually.to.be.include(
                            'Только самовывоз'
                        );
                },
            }),
        },
        'У безрецептурного': {
            'бейдж "Только самовывоз" отсутсвует': makeCase({
                async test() {
                    await this.secondBadge.isVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'Надпись не должна отображаться'
                        );
                },
            }),
        },
    },
});
