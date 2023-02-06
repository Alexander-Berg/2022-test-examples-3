import {makeSuite, makeCase} from 'ginny';
import TopOfferSnippetCompact from '@self/platform/spec/page-objects/components/TopOfferSnippetCompact';
import ShopRating from '@self/project/src/components/ShopRating/__pageObject';
import DeliveryInfo from '@self/platform/components/DeliveryInfo/__pageObject';
import OfferDetailsPopup from '@self/platform/widgets/content/OfferDetailsPopup/__pageObject';


export default makeSuite('DSBS компактный офер.', {
    environment: 'kadavr',
    params: {
        gradesCount: 'Количество отзывов',
        price: 'Цена товара',
        shopName: 'Название магазина',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                topOfferSnippetCompact: () => this.createPageObject(TopOfferSnippetCompact, {
                    root: `${TopOfferSnippetCompact.root}:nth-child(1)`,
                }),
                shopRating: () => this.createPageObject(ShopRating, {parent: this.topOfferSnippetCompact}),
                offerInfo: () => this.createPageObject(OfferDetailsPopup),
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {parent: this.tooltip}),
            });
        },

        'Должен содержать основные данные': makeCase({
            id: 'marketfront-4353',
            issue: 'MARKETFRONT-35406',

            async test() {
                const {shopName, gradesCount, price} = this.params;

                await this.topOfferSnippetCompact.priceClickoutLink.isVisible()
                    .should.eventually.be.equal(
                        true,
                        'Цена должна быть видна'
                    );

                await this.topOfferSnippetCompact.getPrice()
                    .should.eventually.be.equal(
                        price,
                        `Цена должна быть равна "${price}"`
                    );

                await this.topOfferSnippetCompact.getShopNameText()
                    .should.eventually.be.equal(
                        shopName,
                        `Название магазина должно содержать "${shopName}"`
                    );

                await this.shopRating.getRatingText()
                    .should.eventually.be.equal(
                        gradesCount,
                        `Текст о количетсве отзывов должен быть "${gradesCount}"`
                    );
            },
        }),
    },
});
