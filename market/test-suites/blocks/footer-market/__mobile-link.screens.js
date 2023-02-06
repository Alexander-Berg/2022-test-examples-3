import {makeSuite, makeCase} from '@yandex-market/ginny';
import FooterMarket from '@self/platform/spec/page-objects/footer-market';

/**
 * Тесты на элемент __mobile-link блока footer-market.
 * @param {PageObject.FooterMarket} footerMarket
 */
export default makeSuite('Ссылка на мобильную версию Маркета', {
    feature: 'SEO',
    story: {
        'проверяем скриншот всего футера (и ссылки)': makeCase({
            async test() {
                const mobileLinkSelector = FooterMarket.mobileLink;
                const fullFooterSelector = await this.footerMarket.getSelector();

                await this.browser.assertView('link', mobileLinkSelector, {
                    compositeImage: true,
                    allowViewportOverflow: true,
                });
                await this.browser.assertView('full', fullFooterSelector, {
                    compositeImage: true,
                    allowViewportOverflow: true,
                    ignoreElements: [FooterMarket.stats],
                });
            },
        }),

        'проверяем скриншот мобильной ссылки': makeCase({
            async test() {
                const mobileLinkSelector = FooterMarket.mobileLink;

                await this.browser.assertView('link', mobileLinkSelector, {
                    compositeImage: true,
                    allowViewportOverflow: true,
                });
            },
        }),
    },
});
