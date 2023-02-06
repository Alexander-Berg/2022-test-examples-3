import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {makeSuite, makeCase} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import productWithCPADO from '@self/project/src/spec/hermione/fixtures/product/productWithCPADO';
import SearchSnippetOfferServices from '@self/platform/spec/page-objects/containers/SearchSnippet/OfferServices';

const SERVICE_ID_MOCK1 = 1;

export default makeSuite('Выдача', {
    id: 'MARKETFRONT-57677',
    environment: 'kadavr',
    feature: 'Доп услуги на выдаче',
    issue: 'MARKETFRONT-57670',
    story: {
        async beforeEach() {
            this.setPageObjects({
                offerServicesInfo: () => this.createPageObject(SearchSnippetOfferServices),
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
                lr: 213,
            });
        },

        'Бейдж на сниппете товара': makeCase({
            async test() {
                await this.offerServicesInfo.getText()
                    .should.eventually.to.be.equal('Есть установка', 'Выводится информация о наличии услуг');
            },
        }),
    },
});
