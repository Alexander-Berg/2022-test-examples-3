import {mergeSuites, makeSuite, makeCase} from 'ginny';
// eslint-disable-next-line no-restricted-imports
import _ from 'lodash';

import OfferPicture from '@self/root/src/legacy/components/Offer/OfferPicture/__pageObject';
import UiKitPrice from '@self/root/src/uikit/components/PriceBase/__pageObject';
import CartOfferDetails from '@self/root/src/widgets/content/cart/CartList/components/CartOfferDetails/__pageObject';
import CartOfferPrice from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/__pageObject';

/**
 * Тесты на блок CartOffer.
 */
export default makeSuite('Корзинный оффер.', {
    feature: 'Оффер',
    environment: 'testing',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    offerPicture: () => this.createPageObject(OfferPicture, {parent: this.cartOffer}),
                    cartOfferDetails: () => this.createPageObject(CartOfferDetails, {parent: this.cartOffer}),
                    cartOfferPrice: () => this.createPageObject(CartOfferPrice, {parent: this.cartOffer}),
                    actualPrice: () => this.createPageObject(UiKitPrice, {parent: this.cartOfferPrice}),
                });

                return this.cartOffer.isVisible()
                    .should.eventually.be.equal(true, 'Корзинный оффер должен быть показан');
            },
            'По умолчанию': {
                'отображается изображение': makeCase({
                    id: 'bluemarket-2133',
                    test() {
                        return this.offerPicture.imageContainer.isVisible()
                            .should.eventually.to.be.equal(true, 'Изображение оффера должно быть отображено');
                    },
                }),

                'отображает название и оно совпадает с названием из репорта': makeCase({
                    id: 'bluemarket-2133',
                    test() {
                        const {items, region} = this.params;
                        const offerIds = items.map(item => item.offerId);

                        const correctOfferNamePromise = this.browser.yaScenario(
                            this,
                            'deprecatedReportResource.getOffersById',
                            offerIds,
                            region
                        )
                            .then(offers => _.get(offers, '[0].titles.raw'));

                        return this.cartOfferDetails
                            .isVisible(this.cartOfferDetails.title)
                            .should.eventually.to.be.equal(true, 'Название должно быть видимым')
                            .then(() => Promise.all([
                                correctOfferNamePromise,
                                this.cartOfferDetails.getTitle(),
                            ]))
                            .then(([correctName, name]) => (
                                this.expect(correctName).to.be.equal(
                                    name,
                                    `Название оффера должно быть ${correctName}`
                                )
                            ));
                    },
                }),

                'отображает название и оно является ссылкой на карточку КМ': makeCase({
                    id: 'bluemarket-2133',
                    test() {
                        return this.cartOfferDetails
                            .isVisible(this.cartOfferDetails.title)
                            .should.eventually.to.be.equal(true, 'Название должно быть видимым')
                            .then(() => this.browser.yaWaitForChangeUrl(
                                () => this.cartOfferDetails.clickTitle(),
                                2000
                            ))
                            .should.eventually.to.be.link({
                                // Упрощаем, потому что на белом нет ссылки на SKU
                                // pathname: `^/product--${this.params.items[0].slug}/${this.params.items[0].skuId}`,
                                pathname: '\\/product--[\\w-]+\\/[0-9]+',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipQuery: true,
                            });
                    },
                }),

                'отображает цену и она совпадает с ценой из репорта': makeCase({
                    id: 'bluemarket-2133',
                    test() {
                        const {items, region} = this.params;
                        const offerIds = items.map(item => item.offerId);

                        const correctOfferPricePromise = this.browser.yaScenario(
                            this,
                            'deprecatedReportResource.getOffersById',
                            offerIds,
                            region
                        )
                            .then(offers => (
                                Number(_.get(offers, '[0].prices.value'))
                                || Number(_.get(offers, '[0].price.value'))
                            ));

                        return this.actualPrice
                            .isVisible()
                            .should.eventually.to.be.equal(true, 'Цена должна быть видимой')
                            .then(() => Promise.all([
                                correctOfferPricePromise,
                                this.actualPrice.getPriceValue(),
                            ]))
                            .then(([correctPrice, price]) => (
                                this.expect(correctPrice).to.be.equal(
                                    price,
                                    `Цена оффера должна быть ${price}`
                                )
                            ));
                    },
                }),
            },
        }
    ),
});
