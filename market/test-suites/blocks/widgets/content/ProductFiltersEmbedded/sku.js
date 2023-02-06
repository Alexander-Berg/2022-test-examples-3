import {makeSuite, makeCase} from 'ginny';
import VisualEnumFilter from '@self/platform/containers/VisualEnumFilter/_pageObject/';

/**
 * @property {PageObject.VisualEnumFilter} visualEnumFilter
 */
export default makeSuite('Фильтр товара с sku', {
    story: {
        'При выборе быстрого фильтра с sku': {
            'в репорт уходит запрос с правильными параметрами': makeCase({
                id: 'm-touch-3448',
                issue: 'MARKETFRONT-21578',
                async test() {
                    await this.browser.allure.runStep('Проверям видимость цветового фильтра', () =>
                        this.visualEnumFilter.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.visualEnumFilter.clickOnValueById(this.params.applyFilterValueIds[1])
                    );
                    const requests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');
                    const urlWithSkuParam = findUrlWithParam(requests);
                    const skuParamValue = getSkuParamValue(urlWithSkuParam);
                    return this.expect(Number(skuParamValue)).to.not.be.NaN;
                },
            }),
            'пробрасываются параметры sku и glfilter в урл страницы': makeCase({
                id: 'm-touch-3449',
                issue: 'MARKETFRONT-21578',
                async test() {
                    await this.browser.allure.runStep('Проверям видимость цветового фильтра', () =>
                        this.visualEnumFilter.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.visualEnumFilter.clickOnValueById(this.params.applyFilterValueIds[0])
                    );
                    const {query} = await this.browser.yaParseUrl();
                    return this.expect(Boolean(query.sku && query.glfilter))
                        .to.be.equal(true, 'параметры находятся в url');
                },
            }),
            'отсутствуют кнопки сброса быстрых фильтров': makeCase({
                id: 'm-touch-3450',
                issue: 'MARKETFRONT-21578',
                async test() {
                    await this.browser.allure.runStep('Проверям видимость цветового фильтра', () =>
                        this.visualEnumFilter.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.visualEnumFilter.clickOnValueById(this.params.applyFilterValueIds[0])
                    );
                    return this.browser.isExisting(VisualEnumFilter.resetControl)
                        .should.eventually.to.be.equal(false, 'Кнопка сброса не оторажается');
                },

            }),
        },

        'При выборе быстрого фильтра с новым sku': {
            'в репорт уходит запрос с новым sku': makeCase({
                id: 'm-touch-3451',
                issue: 'MARKETFRONT-21578',
                async test() {
                    await this.browser.allure.runStep('Проверям видимость цветового фильтра', () =>
                        this.visualEnumFilter.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );

                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.visualEnumFilter.clickOnValueById(this.params.applyFilterValueIds[0])
                    );
                    let requests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');
                    const urlWithSkuParam = findUrlWithParam(requests);
                    const prevSkuParamValue = getSkuParamValue(urlWithSkuParam);
                    await this.browser.yaWaitForChangeUrl(() =>
                        this.browser.yaOpenPage(
                            'touch:product',
                            Object.assign({}, this.params.initialPageUrl, {sku: prevSkuParamValue})
                        )
                    );
                    requests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');
                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.visualEnumFilter.clickOnValueById(this.params.applyFilterValueIds[1])
                    );

                    let newRequests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');
                    newRequests = clearResponseLogFromOldData(newRequests, requests);
                    const newUrlWithSkuParam = findUrlWithParam(newRequests);
                    const skuParamValue = getSkuParamValue(newUrlWithSkuParam);
                    await this.expect(Number(skuParamValue)).to.not.be.NaN;
                    return this.expect(skuParamValue !== prevSkuParamValue)
                        .to.be.equal(true, 'параметр репорта market-sku должен отличаться от предыдущего запроса');
                },
            }),
        },

        'При открытии страницы с параметром sku': {
            'отсутствуют кнопки сброса быстрых фильтров': makeCase({
                id: 'm-touch-3452',
                issue: 'MARKETFRONT-21578',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу с параметром SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'touch:product',
                                Object.assign({}, this.params.initialPageUrl, {sku: 1})
                            )
                        )
                    );

                    await this.browser.allure.runStep('Проверям видимость цветового фильтра', () =>
                        this.visualEnumFilter.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                    return this.browser.isExisting(VisualEnumFilter.resetControl)
                        .should.eventually.to.be.equal(false, 'Кнопка сброса не оторажается');
                },

            }),
        },

        'При выборе быстрого фильтра без sku': {
            'в репорт уходит запрос без sku': makeCase({
                id: 'm-touch-3453',
                issue: 'MARKETFRONT-21578',
                async test() {
                    await this.browser.allure.runStep('Проверям видимость цветового фильтра', () =>
                        this.visualEnumFilter.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.visualEnumFilter.clickOnValueById(this.params.applyFilterValueIds[0])
                    );
                    let requests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');
                    const urlWithSkuParam = findUrlWithParam(requests);
                    const prevSku = getSkuParamValue(urlWithSkuParam);
                    await this.browser.yaWaitForChangeUrl(() =>
                        this.browser.yaOpenPage(
                            'touch:product',
                            Object.assign({}, this.params.initialPageUrl, {sku: prevSku})
                        )
                    );
                    requests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');
                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.visualEnumFilter.clickOnValueById(this.params.applyFilterValueIds[2])
                    );

                    let newRequests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');
                    newRequests = clearResponseLogFromOldData(newRequests, requests);
                    const newUrlWithSkuParam = findUrlWithParam(newRequests);
                    return this.expect(Boolean(newUrlWithSkuParam))
                        .to.be.equal(false, 'параметр репорта market-sku должен отсутствовать в запросе репорта');
                },
            }),
        },
        'При клике по кнопке +N вариантов и применении фильтров': {
            'в урл проставляется новое значение SKU': makeCase({
                id: 'm-touch-3773',
                issue: 'MARKETFRONT-57276',
                async test() {
                    await this.visualEnumFilter.waitForVisible();
                    await this.visualEnumFilter.clickOnShowAllValuesControl();

                    const valueIndex = 2;

                    await this.selectFilter.waitForVisible();
                    await this.browser.allure.runStep(
                        `Применяем ${valueIndex}-оe значение фильтра`,
                        () => this.selectFilter.getZonedItemByIndex(valueIndex).click()
                    );
                    await this.filterPopup.apply();
                    await this.visualEnumFilter.waitForVisible();

                    return this.browser.yaParseUrl()
                        .should.eventually.be.link({
                            query: {
                                sku: valueIndex - 1,
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            skipPathname: true,
                        });
                },
            }),
        },
    },
});


function getSkuParamValue(urlWithSkuParam) {
    if (!urlWithSkuParam) {
        return undefined;
    }
    return urlWithSkuParam.match(/market-sku=([0-9]+)/)[1];
}


function findUrlWithParam(requests) {
    return requests
        .map(request => request.request.url)
        .find(url => /market-sku=[0-9]+/.test(url));
}

function clearResponseLogFromOldData(response, prevResponse) {
    return response.reduce((result, currentResponseElement, index) => {
        const prevResponseElement = prevResponse[index];
        if (!prevResponseElement || (JSON.stringify(currentResponseElement) !== JSON.stringify(prevResponseElement))) {
            result.push(currentResponseElement);
        }
        return result;
    }, []);
}
