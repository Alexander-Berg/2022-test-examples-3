specs({
    feature: 'autoload2',
}, () => {
    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Отправка parent-reqid при листании вниз', async function() {
        const browser = this.browser;
        let lastReqid;

        await browser.url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS');
        await browser.execute(function() {
            window.__hermioneAutoload2Requests = [];
        });
        await browser.yaProxyFetch({
            test: 'ajax_type=related',
            contentType: 'json',
            then: function(url, data) {
                // Сохраняем данные о запросах за связанными статьями.
                window.__hermioneAutoload2Requests.push({
                    url: url,
                    reqid: data.reqid,
                });
            },
        });
        await browser.yaWaitForVisible(PO.page(), 'Не загрузилась первая страница');

        // Получаем reqid первой страницы.
        lastReqid = await browser.yaGetReqId();

        // Загружаем вторую страницу.
        await browser.yaScrollPageToBottom();
        await browser.yaIndexify(PO.blocks.products.item());
        await browser.yaWaitForVisible(PO.blocks.products.page1(), 'Не загрузилась вторая страница');
        await browser.yaWaitUntil('Не был отправлен запрос за второй страницей с parent-reqid первой страницы', async() => {
            const requestInfo = await browser.execute(function() {
                return window.__hermioneAutoload2Requests[0];
            });

            return requestInfo.value.url.indexOf(lastReqid) !== -1;
        });

        // Получаем reqid второй страницы.
        lastReqid = (await browser.execute(function() {
            return window.__hermioneAutoload2Requests[0].reqid;
        })).value;

        // Загружаем третью страницу.
        await browser.yaScrollPageToBottom();
        await browser.yaIndexify(PO.blocks.products.item());
        await browser.yaWaitForVisible(PO.blocks.products.page2(), 'Не загрузилась третья страница');
        await browser.yaWaitUntil('Не был отправлен запрос за третьей страницей с parent-reqid второй страницы', async() => {
            const requestInfo = await browser.execute(function() {
                return window.__hermioneAutoload2Requests[window.__hermioneAutoload2Requests.length - 1];
            });

            return requestInfo.value.url.indexOf(lastReqid) !== -1;
        });
    });

    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Отправка parent-reqid при листании вверх', async function() {
        const browser = this.browser;

        // Открываем четвертую страницу.
        await browser.url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&page=4&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS');
        await browser.execute(function() {
            window.__hermioneAutoload2Requests = [];
        });
        await browser.yaProxyFetch({
            test: 'ajax_type=related',
            contentType: 'json',
            then: function(url, data) {
                // Сохраняем данные о запросах за связанными статьями.
                window.__hermioneAutoload2Requests.push({
                    url: url,
                    reqid: data.reqid,
                });
            },
        });
        await browser.yaWaitForVisible(PO.page(), 'Не загрузилась первая страница');

        // Загружаем все страницы сверху.
        await browser.yaScrollPage(0);
        await browser.yaIndexify(PO.blocks.products.item());
        await browser.yaWaitForVisible(PO.blocks.products.page0(), 'Не загрузилась первая страница');

        const requests = (await browser.execute(function() {
            return window.__hermioneAutoload2Requests;
        })).value;

        for (let i = 1; i < requests.length; i++) {
            const currentRequest = requests[i];
            const previousRequest = requests[i - 1];

            assert.include(
                currentRequest.url,
                previousRequest.reqid,
                `Не был отправлен запрос за страницей ${i} с parent-reqid страницы ${i - 1}`
            );
        }
    });
});
