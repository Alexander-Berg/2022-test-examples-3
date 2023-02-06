// Тесты для страниц со списками товаров

import {
    makeCase,
    mergeSuites,
    makeSuite,
} from 'ginny';

import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';

import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import Footer from '@self/platform/spec/page-objects/Footer';
import Snippet from '@self/root/src/components/Snippet/__pageObject';
import COOKIE_NAME from '@self/root/src/constants/cookie';

export default makeSuite('Деградация.', {
    environment: 'testing',
    feature: 'Деградация',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    header: () => this.createPageObject(Header),
                    footer: () => this.createPageObject(Footer),
                    snippet: () => this.createPageObject(Snippet),
                });
            },
        },

        makeSuite('Отказ лоялти.', {
            id: 'bluemarket-2614',
            issue: 'BLUEMARKET-6792',
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: {
                        name: COOKIE_NAME.AT_ENDPOINTS_SETTINGS,
                        value: JSON.stringify({
                            [BACKENDS_NAME.LOYALTY]: {
                                isCrashed: true,
                            },
                        }),
                    },
                },
            },
            story: getTests(),
        }),

        makeSuite('Отказ картера.', {
            id: 'bluemarket-2669',
            issue: 'BLUEMARKET-6792',
            environment: 'kadavr',
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: {
                        name: COOKIE_NAME.AT_ENDPOINTS_SETTINGS,
                        value: JSON.stringify({
                            [BACKENDS_NAME.CARTER]: {
                                isCrashed: true,
                            },
                        }),
                    },
                },
            },
            story: getTests(),
        })
    ),
});

function getTests() {
    return {
        'Страница должна содержать шапку и футер': makeCase({
            async test() {
                await this.header.isVisible()
                    .should.eventually.to.be.equal(true, 'Шапка должна быть видна');

                await this.browser.yaSlowlyScroll(Footer.root);

                await this.footer.isVisible()
                    .should.eventually.to.be.equal(true, 'Футер должен быть виден');
            },
        }),

        'Страница должна содержать хотя бы один сниппет': makeCase({
            async test() {
                await this.browser.yaSlowlyScroll(Snippet.root);

                let snippetVisibility = await this.snippet.isVisible();
                if (!Array.isArray(snippetVisibility)) {
                    snippetVisibility = [snippetVisibility];
                }

                await this.expect(snippetVisibility).to.be.include(true, 'Хотя бы один сниппет должен быть виден');
            },
        }),
    };
}
