import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {makeSuite, makeCase} from 'ginny';

import ChoosingService from '@self/root/src/components/ChoosingService/__pageObject/index.desktop';
import OfferServicesInfo from '@self/root/src/components/OfferServicesInfo/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import productWithCPADO from '@self/project/src/spec/hermione/fixtures/product/productWithCPADO';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup
    from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';

const SERVICE_ID_MOCK1 = 1;

export default makeSuite('Выдача', {
    id: 'MARKETFRONT-57677',
    environment: 'kadavr',
    feature: 'Доп услуги на выдаче',
    issue: 'MARKETFRONT-57670',
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton),
                cartPopup: () => this.createPageObject(CartPopup),
                choosingService: () => this.createPageObject(ChoosingService),
                offerServicesInfo: () => this.createPageObject(OfferServicesInfo),
            });

            const {offerMock} = productWithCPADO;
            const offer = createOffer({
                ...offerMock,
                services: [
                    {
                        service_id: SERVICE_ID_MOCK1,
                        title: 'Установка1',
                        price: {
                            value: '42',
                            currency: 'RUR',
                        },
                    },
                ],
            }, offerMock.wareId);
            const dataMixin = {
                data: {
                    search: {
                        total: 1,
                        totalOffers: 1,
                    },
                },
            };
            const state = mergeState([
                offer,
                dataMixin,
            ]);

            await this.browser.setState('Carter.items', []);
            await this.browser.setState('report', state);

            return this.browser.yaOpenPage(PAGE_IDS_COMMON.LIST, {
                nid: '123',
                slug: 'slug',
                onstock: 1,
                viewtype: 'list',
            });
        },

        'Бейдж на сниппете товара': makeCase({
            async test() {
                await this.offerServicesInfo.getText()
                    .should.eventually.to.be.equal('Есть установка', 'Выводится информация о наличии услуг');
            },
        }),

        'Услуги в попапе апсейла': makeCase({
            async test() {
                await this.cartButton.click();
                await this.cartPopup.waitForAppearance();

                await this.expect(this.choosingService.isServiceVisible(SERVICE_ID_MOCK1))
                    .to.be.equal(true, 'Услуга должна быть видна');
            },
        }),
    },
});
