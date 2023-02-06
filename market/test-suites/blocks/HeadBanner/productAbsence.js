import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import HeadBannerAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/absence';
import HeadBanner from '@self/platform/spec/page-objects/head-banner';

/**
 * Этот кейс правильнее было бы положить к топ-сьютам восле страницы, которую он тестирует.
 * Но из-за того, что нам нужно проверить тест-кейс на всех вкладках КМ, он лежит здесь, чтобы избежать
 * большого кол-ва копипасты.
 *
 * Подобное решение не стоит где-либо переиспользовать.
 */
export default makeSuite('Баннеры.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Баннер-растяжка.', {
            params: {
                slug: 'slug продукта',
                productId: 'id продукта',
            },
            story: prepareSuite(HeadBannerAbsenceSuite, {
                pageObjects: {
                    headBanner() {
                        return this.createPageObject(HeadBanner);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const slug = this.params.slug || 'test';
                        const productId = this.params.productId || 123;

                        if (!this.params.productId) {
                            const product = createProduct({slug}, productId);
                            await this.browser.setState('report', product);
                        }

                        await this.browser.yaOpenPage(this.params.pageId, {productId, slug});
                    },
                },
            }),
        })
    ),
});
