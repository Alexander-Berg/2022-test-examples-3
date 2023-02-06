const URL = require('url');

specs({
    feature: 'ProductFiltersPage',
}, () => {
    hermione.only.notIn('safari13');
    it('Проверка работы фильтрации по цене', function() {
        let host;

        return this.browser
            .url('/')
            .url().then(res => {
                const url = URL.parse(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                []
            ))
            .url('/turbo?stub=productfilterspage/empty-to-price.json')
            .yaWaitForVisible(PO.blocks.filters(), 'Фильтры не загрузились')
            .setValue(PO.priceRange.firstInput(), 2)
            .setValue(PO.priceRange.secondInput(), 10)
            .yaWaitUntil('Не произошел запрос с правильными параметрами', () =>
                this.browser
                    .then(() => this.browser
                        .yaGetExternalResourcesRequests(`https://${host}/filters`)
                    )
                    .then(requests =>
                        requests.find(item => JSON.parse(item.text).price[1] === 10)
                    )
            )
            .click(PO.blocks.filtersActions.button())
            .yaWaitUntil('Не произошел переход на страницы каталога', () =>
                this.browser
                    .getUrl() // @TODO [https://st.yandex-team.ru/TURBOUI-1149] при переходе ошибка, нет tpid, темплар не может так подмешать данные в ссылки
                    .then(url =>
                        url.includes('productspage/index.json')
                    )
            );
    });

    hermione.only.notIn('safari13');
    it('Проверка работы фильтрации с одним чекбоксом', function() {
        return this.browser
            .url('/turbo?stub=productfilterspage/empty-to-single.json')
            .yaWaitForVisible(PO.blocks.filters(), 'Фильтры не загрузились')
            .yaIndexify(PO.checkboxGroup.item())
            .click(PO.checkboxGroup.itemFirst.checkbox2())
            .pause(1500) // ждем, пока состоится запрос за урлом для кнопки
            .assertView('clicked', PO.blocks.filters())
            .click(PO.blocks.filtersActions.button())
            .yaWaitUntil('Не произошел переход на страницы каталога', () =>
                this.browser
                    .getUrl() // @TODO [https://st.yandex-team.ru/TURBOUI-1149] при переходе ошибка, нет tpid, темплар не может так подмешать данные в ссылки
                    .then(url => url.includes('stub-filter-single.json'))
            );
    });

    hermione.only.notIn('safari13');
    it('Проверка работы фильтрации с несколькими чекбоксами', function() {
        let host;

        return this.browser
            .url('/')
            .url().then(res => {
                const url = URL.parse(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                []
            ))
            .url('/turbo?stub=productfilterspage/empty-to-multiple.json')
            .yaWaitForVisible(PO.blocks.filters(), 'Фильтры не загрузились')
            .yaIndexify(PO.checkboxGroup.item())
            .click(PO.checkboxGroup.itemFirst.checkbox2())
            .click(PO.checkboxGroup.itemSecond.checkbox2())
            .yaWaitUntil('Не произошел запрос с правильными параметрами', () =>
                this.browser
                    .then(() => this.browser
                        .yaGetExternalResourcesRequests(`https://${host}/filters`)
                    )
                    .then(requests => {
                        const rightRequest = requests.find(item => JSON.parse(item.text).manufacturer[1] === 'asus');

                        if (!rightRequest) {
                            return false;
                        }

                        const requestParams = JSON.parse(rightRequest.text);

                        assert.isTrue(
                            requestParams.price[0] === null && requestParams.price[1] === 12000,
                            'Неправильно передан диапазон цен'
                        );

                        assert.equal(requestParams.manufacturer.length, 2, 'Неправильное количество производителей');

                        assert.isTrue(
                            requestParams.manufacturer[0] === 'apple' && requestParams.manufacturer[1] === 'asus',
                            'Неправильно переданы производители');

                        return true;
                    })
            )
            .click(PO.blocks.filtersActions.button())
            .yaWaitUntil('Не произошел переход на страницы каталога', () =>
                this.browser
                    .getUrl() // @TODO [https://st.yandex-team.ru/TURBOUI-1149] при переходе ошибка, нет tpid, темплар не может так подмешать данные в ссылки
                    .then(url => url.includes('productspage/index.json'))
            );
    });

    hermione.only.notIn('safari13');
    it('Проверка работы фильтрации с переходом в полный список чекбоксов', function() {
        return this.browser
            .url('/turbo?stub=productfilterspage/empty-to-full-list.json')
            .yaWaitForVisible(PO.blocks.filters(), 'Фильтры не загрузились')
            .click(PO.checkboxGroup.showAll.button())
            .assertView('group-page', PO.blocks.page())
            .yaIndexify(PO.checkboxGroup.item())
            .click(PO.checkboxGroup.itemFifth.checkbox2())
            .click(PO.checkboxGroup.itemSixth.checkbox2())
            .click(PO.blocks.filtersActions.button())
            .assertView('applied', PO.blocks.page());
    });
});
