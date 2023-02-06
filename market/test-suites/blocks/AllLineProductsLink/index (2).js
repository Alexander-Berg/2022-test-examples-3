import {makeSuite, makeCase} from 'ginny';
import {filterCorrectRrequestToVendorLine} from '@self/platform/spec/hermione/helpers/filterCorrectRrequestToVendorLine';
import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds/index.js';

export const allLineLinkSuite = makeSuite('Ссылка "Смотреть все" в карусели "Товары из той же линейки"', {
    story: {
        'По умолчанию': {
            'присутствует на странице': makeCase({
                async test() {
                    await this.allLineProductsWidgetWrapper.isLinkVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Ссылка присутсвует на странице.'
                        );
                },
            }),
            'текст ссылки корректный': makeCase({
                async test() {
                    await this.allLineProductsWidgetWrapper.getLinkText()
                        .should.eventually.to.be.equal(
                            this.params.expectedLinkText,
                            'Текст ссылки корректный.'
                        );
                },
            }),
            'url ссылки корректный': makeCase({
                async test() {
                    const actualUrl = await this.allLineProductsWidgetWrapper.getLinkHref();

                    const expectedURL = await this.browser.yaBuildURL(PAGE_IDS_TOUCH.YANDEX_MARKET_VENDOR_LINE, this.params.routeParams);

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
        'По клику': {
            'ссылка ведет на правильную страницу и осуществляется корректный запрос в репорт': makeCase({
                async test() {
                    // скроллим чтобы лучше срабатывал клик
                    const allLineProductsWidgetWrapperSelector = await this.allLineProductsWidgetWrapper.getSelector();
                    await this.browser.scroll(allLineProductsWidgetWrapperSelector);

                    await this.allLineProductsWidgetWrapper.linkClick();

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL(PAGE_IDS_TOUCH.YANDEX_MARKET_VENDOR_LINE, this.params.routeParams);

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
