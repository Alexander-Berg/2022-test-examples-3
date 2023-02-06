import {prepareSuite, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import ProductSummarySubscribeSutie from '@self/platform/spec/hermione/test-suites/blocks/n-product-summary-subscribe';
import Subscription from '@self/platform/widgets/content/NotOnSale/components/Subscription/__pageObject';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';

import {absentPhone as productMock} from '../fixtures/product';

export default makeSuite('Подписка на КМ', {
    feature: 'Вид подписки на модель на КМ',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                productSummarySubscribe: () => this.createPageObject(Subscription),
            });

            const product = createProduct(productMock, productMock.id);
            await this.browser.setState('report', product);
        },
        'Залогин': prepareSuite(ProductSummarySubscribeSutie, {
            meta: {
                id: 'marketfront-998',
                issue: 'MARKETVERSTKA-31646',
            },
            hooks: {
                async beforeEach() {
                    const url = await this.browser
                        .yaBuildURL('market:product', {
                            productId: productMock.id,
                            slug: productMock.slug,
                        });
                    return this.browser
                        .yaLogin(profiles.dzot61.login, profiles.dzot61.password, url);
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
        'Незалогин': prepareSuite(ProductSummarySubscribeSutie, {
            meta: {
                id: 'marketfront-999',
                issue: 'MARKETVERSTKA-31646',
            },
            hooks: {
                async beforeEach() {
                    return this.browser
                        .yaOpenPage('market:product', {
                            productId: productMock.id,
                            slug: productMock.slug,
                        });
                },
            },
        }),
    },
});
