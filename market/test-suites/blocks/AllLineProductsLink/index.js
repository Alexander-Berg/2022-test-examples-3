import {makeSuite, makeCase} from 'ginny';
import {PAGE_IDS_DESKTOP} from '@self/root/src/constants/pageIds';
import {filterCorrectRrequestToVendorLine} from '@self/platform//spec/hermione/helpers/filterCorrectRrequestToVendorLine';

/**
 * @param {PageObject.AllLineProductsLink} AllLineProductsLink
 */
export const defaultSuite = makeSuite('Проверка видимости, корректности текста и пути сссылки', {
    story: {
        'По умолчанию': {
            'присутствует на странице': makeCase({
                async test() {
                    await this.allLineProductsLink.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Ссылка присутсвует на странице.'
                        );
                },
            }),
            'текст ссылки корректный': makeCase({
                async test() {
                    await this.allLineProductsLink.getLinkText()
                        .should.eventually.to.be.equal(
                            this.params.expectedLinkText,
                            'Текст ссылки корректный.'
                        );
                },
            }),
            'url ссылки корректный': makeCase({
                async test() {
                    const actualUrl = await this.allLineProductsLink.getLink();
                    const expectedURL = await this.browser.yaBuildURL(PAGE_IDS_DESKTOP.YANDEX_MARKET_VENDOR_LINE, this.params.routeParams);

                    await this.expect(actualUrl)
                        .to.be.deep.link(
                            expectedURL,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                },
            }),
        },
    },
});

export const redirectSuite = makeSuite('Проверка запроса в репорт и фильтров линейки', {
    story: {
        async beforeEach() {
            await this.browser.refresh();
        },
        'По клику': {
            'ссылка ведет на правильную страницу и осуществляется корректный запрос в репорт': makeCase({
                async test() {
                    await this.allLineProductsLink.linkClick();

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL(PAGE_IDS_DESKTOP.YANDEX_MARKET_VENDOR_LINE, this.params.routeParams);

                    await this.expect(currentUrl)
                        .to.be.deep.link(
                            expectedUrl,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            },
                            'ссылка ведет на правильную страницу vendor-line, содержит hid и glfilter в квери-параметрах.'
                        );

                    const requests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');
                    const filteredRequests = filterCorrectRrequestToVendorLine(this.params.routeParams.hid, this.params.routeParams.glfilter, requests);

                    await this.expect(filteredRequests.length)
                        .to.be.not.equal(0, 'Ожидаем ненулевое кол-во запросов в репорт, которые содержат фильтры');
                },
            }),
        },
    },
});
