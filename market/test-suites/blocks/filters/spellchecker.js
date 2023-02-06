import {makeSuite, makeCase} from '@yandex-market/ginny';

import {
    reportState,
} from '@self/platform/spec/hermione2/test-suites/blocks/filters/fixtures/productWithOffers';
import {routes} from '@self/platform/spec/hermione/configs/routes';

export default makeSuite('Опечаточник', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-64247',
    story: {
        async beforeEach() {
            await this.browser.setState('report', reportState);
            await this.browser.yaOpenPage('touch:list', {...routes.catalog.phones, was_redir: 1, text: this.params.text, glfilter: '7893318%3A7701962'});
        },

        'Содержит верный текст': makeCase({
            id: 'm-touch-3824',
            async test() {
                await this.browser.yaWaitForPageReady();
                const spellcheckText = await this.searchSpellcheck.getSpellcheckText();
                return this.browser.expect(spellcheckText)
                    .to.be.equal(
                        'Запрос исправлен. Вернуть «ноушники»'
                    );
            },
        }),
        'Содержит верную ссылку': makeCase({
            id: 'm-touch-3824',
            async test() {
                await this.browser.yaWaitForPageReady();
                const spellcheckHref = await this.searchSpellcheck.getHref();
                const isSpellcheckHrefValid = spellcheckHref.includes(encodeURIComponent('ноушники')) && spellcheckHref.includes('noreask=1');
                return this.browser.expect(isSpellcheckHrefValid)
                    .to.be.equal(
                        true
                    );
            },
        }),
    },
});
