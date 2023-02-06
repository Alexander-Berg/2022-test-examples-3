specs({
    feature: 'ProductsControls',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=productspage/index.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.productsControls());
    });

    hermione.only.notIn('safari13');
    it('Проверка работы сортировки по возрастанию цены', function() {
        return this.browser
            .url('/turbo?stub=productspage/index.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .getValue(PO.blocks.productsControls.sort())
            .then(value => assert.strictEqual(value, 'rating'))
            .selectByValue(PO.blocks.productsControls.sort(), 'price')
            .getUrl() // @TODO [https://st.yandex-team.ru/TURBOUI-1149] при переходе ошибка, нет tpid, темплар не может так подмешать данные в ссылки
            .then(url => assert.isTrue(url.includes('stub=productspage%2Fstub_sort_price.json')));
    });

    hermione.only.notIn('safari13');
    it('Проверка работы сортировки по убыванию цены', function() {
        return this.browser
            .url('/turbo?stub=productspage/index.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .selectByValue(PO.blocks.productsControls.sort(), '-price')
            .getUrl() // @TODO [https://st.yandex-team.ru/TURBOUI-1149] при переходе ошибка, нет tpid, темплар не может так подмешать данные в ссылки
            .then(url => assert.isTrue(url.includes('stub=productspage%2Fstub_sort_-price.json')));
    });

    hermione.only.notIn('safari13');
    it('Ссылка на фильтры', function() {
        return this.browser
            .url('/turbo?stub=productspage/index.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.blocks.productsControls.filters())
            .getUrl() // @TODO [https://st.yandex-team.ru/TURBOUI-1149] при переходе ошибка, нет tpid, темплар не может так подмешать данные в ссылки
            .then(url => assert.isTrue(url.includes('stub=productfilterspage%2Fdefault.json')));
    });

    hermione.only.notIn('safari13');
    it('Ссылка на фильтры на красивом URL', async function() {
        await this.browser.url('/turbo/n/ymturbo.t-dir.com/yandexturbocatalog/?srcrwr=SAAS%3ASAAS_ANSWERS');
        await this.browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        await this.browser.click(PO.blocks.productsControls.filters());
        const url = await this.browser.getUrl();
        assert.isTrue(url.includes('/turbo/n/ymturbo.t-dir.com/yandexturbocatalog/filters'));
        await this.browser.yaShouldBeVisible(PO.blocks.filters(), 'Не отобразились фильтры');
    });
});
