import {makeSuite, mergeSuites} from 'ginny';

// page objects
import SnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';

// imports
import {
    createOfferForProduct,
    createProduct,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import offerKettle from '@self/root/src/spec/hermione/kadavr-mock/report/offer/kettle';
import productKettle from '@self/root/src/spec/hermione/kadavr-mock/report/product/kettle';
import knownThumbnails from '@self/root/src/spec/hermione/kadavr-mock/knownThumbnails';

import {ORDER, ORDER_ID} from './mock';

export default makeSuite('Список купленных товаров.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    snippetCell2: () => this.createPageObject(
                        SnippetCell,
                        {
                            parent: this.snippetList,
                            root: `${SnippetCell.root}:nth-of-type(1)`,
                        }
                    ),
                });

                await this.browser.setState(
                    'Checkouter',
                    {
                        collections: {
                            order: {
                                [ORDER_ID]: ORDER,
                            },
                        },
                    }
                );
                await this.browser.setState(
                    'report',
                    mergeState([
                        {
                            data: {
                                search: {
                                    total: 2,
                                    ...knownThumbnails,
                                },
                            },
                        },
                        createProduct(productKettle, productKettle.id),
                        createOfferForProduct({
                            ...offerKettle,
                            promos: [
                                {
                                    type: 'blue-cashback',
                                    key: 'JwguUZO8-HIaOJ1J4_k0_Q',
                                    description: 'Кэшбэк на все',
                                    shopPromoId: '3vDxyRpFM8ycDVJiunVGRA',
                                    startDate: '2020-09-14T21:00:00Z',
                                    endDate: '2024-12-30T21:00:00Z',
                                    share: 0.05,
                                    version: 1,
                                    priority: 199,
                                    value: 207,
                                },
                            ],
                        }, productKettle.id, offerKettle.wareId),
                    ])
                );

                return this.browser.yaProfile('pan-topinambur', 'market:purchased');
            },
        }

        /** MARKETFRONT-58893: Скип автотестов в релизе 2021.375.0
         * WishlistTumbler перенесен в src
        makeSuite('Тулбар оффера', {
            environment: 'kadavr',
            story: {
                async beforeEach() {
                    this.setPageObjects({
                        toolbar: () => this.createPageObject(OfferToolbar),
                        wishlistTumbler: () => this.createPageObject(
                            WishlistTumbler,
                            {parent: OfferToolbar.root}
                        ),
                    });
                },

                'Кнопка "Добавить в избранное".': {
                    'По умолчанию': {
                        'должна присутствовать на каждом сниппете': makeCase({
                            async test() {
                                const selector = `${OfferToolbar.root} ${WishlistTumbler.root}`;
                                await this.browser.waitForExist(selector);
                                await this.browser.moveToObject(selector, 10, 10);

                                return this.wishlistTumbler.isVisible()
                                    .should.eventually
                                    .be.equal(true, 'Кнопка должна отображаться');
                            },
                        }),
                    },
                },
            },
        }),
        */
    ),
});
