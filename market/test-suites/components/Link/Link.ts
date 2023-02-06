'use strict';

import url from 'url';

import {makeSuite, makeCase} from 'ginny';

/**
 * @param {Object} [params]
 * @param {boolean} [params.external] - неконтролируемая ссылка на стороннем ресурсе
 * @param {string} [params.caption] - текст в ссылке
 * @param {string} [params.target]
 * @param {string} [params.url]
 * @param {boolean} [params.exist]
 * @param {Object} [params.comparison]
 */
export default makeSuite('Ссылка', {
    environment: 'testing',
    params: {
        caption: 'Текст ссылки',
    },
    story: {
        корректная: makeCase({
            async test() {
                if (this.params.exist === false) {
                    return this.link.isExisting().should.eventually.be.equal(false, 'Ссылка отсутствует');
                }

                // Если мы разобьем каждую проверку на отдельный story
                // в пальме мы увидим очень много одинаковых проверок
                // на самом деле это будут разные проверки, но называться они будут одинаково
                // чтобы избежать такой ботвы, все проверки в одной стори
                await this.link.isVisible().should.eventually.be.equal(true, 'Ссылка отображается');

                await this.link.getText().should.eventually.be.equal(this.params.caption, 'Текст корректный');

                await this.link
                    .getTarget()
                    .should.eventually.be.equal(
                        this.params.target,
                        this.params.target ? 'target корректный' : 'target не указан',
                    );

                /*
                 * Матчер link не умеет обрабатывать адреса без протокола HTTP(S),
                 * поэтому разбираем ссылку самостоятельно.
                 */
                const parsedUrl = url.parse(this.params.url, true, true);

                /*
                 * По умолчанию пропускаем проверку протокола,
                 * так как он отсутствует в ссылках, созданных с помощью buildURL
                 */
                const {browser} = this;
                const comparison = {
                    skipProtocol: true,
                    ...this.params.comparison,
                };

                const currentUrl = await browser.getUrl().then(browserUrl => url.parse(browserUrl, true, true));

                // когда ссылка внешняя или мы уже находимся на этом url, просто проверяем адрес ссылки
                if (this.params.external || currentUrl.path === this.params.url) {
                    return browser.allure.runStep('Проверяем корректность url', () =>
                        this.link.getUrl().should.eventually.be.link(parsedUrl, comparison),
                    );
                }

                const method = this.params.target === '_blank' ? 'vndWaitForChangeTab' : 'vndWaitForChangeUrl';
                const actionAndResultUrl = () =>
                    // @ts-expect-error(TS7053) найдено в рамках VNDFRONT-4580
                    browser[method](() => this.link.root.click());

                await browser.allure.runStep('Кликаем по ссылке', () =>
                    actionAndResultUrl().should.eventually.be.link(parsedUrl, comparison),
                );
            },
        }),
    },
});
