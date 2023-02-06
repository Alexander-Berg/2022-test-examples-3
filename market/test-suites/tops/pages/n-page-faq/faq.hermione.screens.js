import {prepareSuite, mergeSuites, makeSuite} from '@yandex-market/ginny';

import {routes} from '@self/platform/spec/hermione2/configs/routes';
import FooterMobileLinkSuite from '@self/platform/spec/hermione2/test-suites/blocks/footer-market/__mobile-link.screens';
import FooterMarket from '@self/platform/spec/page-objects/footer-market';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница «Словарь терминов».', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Футер', {
            environment: 'testing',
            story: prepareSuite(FooterMobileLinkSuite, {
                pageObjects: {
                    footerMarket() {
                        return this.browser.createPageObject(FooterMarket);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:faq', routes.faq.bicycle);
                    },
                },
            }),
        })
    ),
});
