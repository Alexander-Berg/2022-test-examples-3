import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import NewProductUgcVideoSuite from '@self/platform/spec/hermione/test-suites/blocks/NewProductUgcVideo';
import NewProductUgcVideoHeaderSuite from '@self/platform/spec/hermione/test-suites/blocks/NewProductUgcVideo/header';
import NewProductUgcVideoPage from '@self/platform/widgets/pages/NewProductUgcVideoPage/__pageObject';
import NewProductUgcVideoContent from '@self/platform/widgets/parts/NewProductUgcVideoContent/__pageObject';
import UgcVideoInput from '@self/platform/components/UgcVideoInput/__pageObject';

import {
    phoneWithAveragePriceProductRoute as routeMock,
    productWithAveragePriceAndCorrectSlug as productMock,
} from '@self/platform/spec/hermione/fixtures/product';

const testPage = 'touch:blank';
const testPageParams = {
    testParam: '12345',
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница добавления UGC видео.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', productMock);

                await this.browser.yaLogin(
                    profiles.ugctest3.login,
                    profiles.ugctest3.password
                );
            },
        },
        prepareSuite(NewProductUgcVideoHeaderSuite, {
            hooks: {
                async beforeEach() {
                    // Для тестирования перехода на предыдущую страницу
                    await this.browser.yaOpenPage(testPage, testPageParams);

                    return this.browser.yaOpenPage('market:product-video-add', {
                        productId: routeMock.productId,
                        slug: routeMock.slug,
                    });
                },
            },
            pageObjects: {
                page() {
                    return this.createPageObject(NewProductUgcVideoPage);
                },
            },
            params: {
                testPage,
                testPageParams,
            },
        }),
        prepareSuite(NewProductUgcVideoSuite, {
            hooks: {
                async beforeEach() {
                    return this.browser.yaOpenPage('market:product-video-add', {
                        productId: routeMock.productId,
                        slug: routeMock.slug,
                    });
                },
            },
            pageObjects: {
                content() {
                    return this.createPageObject(NewProductUgcVideoContent);
                },
                videoInput() {
                    return this.createPageObject(UgcVideoInput);
                },
            },
            params: {
                productId: routeMock.productId,
                slug: routeMock.slug,
            },
        })
    ),
});

