import {makeSuite, makeCase} from 'ginny';
import OfferTypeInformer from '@self/root/src/components/OfferTypeInformer/__pageObject';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';

import * as alcohol from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';
import * as vet from '@self/root/src/spec/hermione/kadavr-mock/report/vitaminsLowCost';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

export default makeSuite('Нотификашка о купонах в корзине.', {
    feature: 'Нотификашка про неприменимость купонов',
    environment: 'kadavr',
    issue: 'BLUEMARKET-7081',
    id: 'bluemarket-2786',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    story: {
        'Надпись о неприменимости купонов.': makeSuite('1 дропшип и 1 не дропшип в корзине', {
            defaultParams: {
                cart: buildCheckouterBucket({
                    items: [{
                        skuMock: alcohol.skuMock,
                        offerMock: alcohol.offerMock,
                    }, {
                        skuMock: vet.skuMock,
                        offerMock: vet.offerMock,
                    }],
                }),
            },
            story: {
                async beforeEach() {
                    this.setPageObjects({
                        offerTypeInformerVet: () => this.createPageObject(OfferTypeInformer,
                            {parent: CartItemGroup.secondGroup}),
                        offerTypeInformerAlco: () => this.createPageObject(OfferTypeInformer,
                            {parent: CartItemGroup.firstGroup}),
                    });

                    const state = await this.browser.yaScenario(
                        this,
                        prepareMultiCartState,
                        [this.params.cart]
                    );
                    await this.browser.yaScenario(this,
                        'cart.prepareCartPageBySkuId',
                        {
                            items: this.params.cart.items.map(item => ({
                                skuId: item.skuMock.id,
                                offerId: item.offerMock.wareId,
                            })),
                            reportSkus: state.reportSkus,
                        }
                    );
                },

                'На не дропшипе': makeCase({
                    async test() {
                        return this.offerTypeInformerVet
                            .isVisible()
                            .should.eventually.be.equal(
                                false,
                                'надпись о неприменимости купонов не должна отображаться'
                            );
                    },
                }),
            },
        }),
    },
});
