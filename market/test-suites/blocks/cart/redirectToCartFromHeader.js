import {
    makeSuite,
    makeCase,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import Header from '@self/platform/spec/page-objects/header2';

/**
 * Тест на переход в корзину из шапки.
 */

export default makeSuite('Переход в корзину.', {
    feature: 'Корзина',
    story: {
        async beforeEach() {
            await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);

            await this.setPageObjects({
                header: () => this.createPageObject(Header),
            });
        },

        'При клике на иконку корзины в шапке': {
            'просходит переход на страницу корзины': makeCase({
                id: 'bluemarket-2710',
                issue: 'BLUEMARKET-6245',
                environment: 'testing',

                async test() {
                    await this.header.clickIconCart();

                    const [openedUrl, expectedPath] = await Promise.all([
                        this.browser.getUrl(),
                        this.browser.yaBuildURL(PAGE_IDS_COMMON.CART),
                    ]);

                    await this.expect(openedUrl).to.be.link({pathname: expectedPath}, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
