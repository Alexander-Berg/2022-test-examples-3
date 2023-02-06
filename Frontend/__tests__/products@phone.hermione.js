specs({
    feature: 'products',
}, () => {
    it('Внешний вид с пустым списком', function() {
        return this.browser
            .url('/turbo?stub=products/empty.json')
            .yaWaitForVisible(PO.products(), 'Карточки товаров не появились на странице')
            .assertView('plain', PO.products());
    });

    describe('Лейауты', function() {
        it('Дефолтные карточки', function() {
            return this.browser
                .url('/turbo?stub=products/default.json')
                .yaWaitForVisible(PO.products(), 'Карточки товаров не появились на странице')
                // Не получается снять PO.products(), выдаётся ошибка:
                // Position of the region is outside of the viewport left, top or right bounds
                // В блоке используются отрицательные маргины для компенсации .grid_wrap
                // Они указаны в точности такие, как в .grid_wrap, но он hermione считает,
                // что происходит выход за пределы viewport.
                // https://st.yandex-team.ru/FEI-12959
                .assertView('plain', 'html');
        });
        hermione.only.in(['chrome-phone', 'safari13'], 'setOrientation() используем только в chrome-phone и safari13');
        it('Дефолтные карточки - горизонтальная ориентация', function() {
            return this.browser
                .url('/turbo?stub=products/default.json&hermione_no-lazy=1')
                .setOrientation('landscape')
                .yaWaitForVisible(PO.products(), 'Карточки товаров не появились на странице')
                .yaIndexify(PO.blocks.products.item())
                .assertView('plain', [
                    PO.blocks.products.item0(),
                    PO.blocks.products.item1(),
                ]);
        });

        it('Тип list', function() {
            return this.browser
                .url('/turbo?stub=products/list.json')
                .yaWaitForVisible(PO.products(), 'Карточки товаров не появились на странице')
                .assertView('plain', 'html');
        });

        hermione.only.in(['chrome-phone', 'safari13'], 'setOrientation() используем только в chrome-phone и safari13');
        it('Тип list - горизонтальная ориентация', function() {
            return this.browser
                .url('/turbo?stub=products/list.json')
                .setOrientation('landscape')
                .yaWaitForVisible(PO.products(), 'Карточки товаров не появились на странице')
                .yaIndexify(PO.blocks.products.item())
                .assertView('plain', [
                    PO.blocks.products.item0(),
                    PO.blocks.products.item1(),
                ]);
        });

        it('Тип big-list', function() {
            return this.browser
                .url('/turbo?stub=products/big-list.json')
                .yaWaitForVisible(PO.products(), 'Карточки товаров не появились на странице')
                .assertView('plain', 'html');
        });

        hermione.only.in(['chrome-phone', 'safari13'], 'setOrientation() используем только в chrome-phone и safari13');
        it('Тип big-list - горизонтальная ориентация', function() {
            return this.browser
                .url('/turbo?stub=products/big-list.json&hermione_no-lazy=1')
                .setOrientation('landscape')
                .yaWaitForVisible(PO.products(), 'Карточки товаров не появились на странице')
                .yaIndexify(PO.blocks.products.item())
                .assertView('plain', [
                    PO.blocks.products.item0(),
                    PO.blocks.products.item1(),
                ]);
        });
    });

    describe('Загрузка товаров', function() {
        it('Загрузка всех предыдущих страниц', function() {
            return this.browser
                .url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&page=3&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS&turbo_enable_cgi_forwarding=1#p532')
                .yaWaitForVisible(PO.blocks.products.page3())
                .yaWaitForVisible(PO.blocks.products.page2())
                .yaWaitForVisible(PO.blocks.products.page1())
                .yaWaitForVisible(PO.blocks.products.page0())
                .yaIndexify(PO.blocks.products.item())
                .yaShouldBeVisible(PO.blocks.products.item0())
                .yaShouldBeVisible(PO.blocks.products.item1())
                .yaShouldNotBeVisible(PO.blocks.products.item0.imageStub())
                .yaShouldNotBeVisible(PO.blocks.products.item1.imageStub());
        });

        it('Загрузка следующих страниц', async function() {
            const browser = this.browser;
            await browser.url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&srcrwr=SEARCH_SAAS_LISTINGS%3ASAAS_ANSWERS&turbo_enable_cgi_forwarding=1');

            await browser.yaWaitForVisible(PO.blocks.products.page0());
            const elementsAfterScroll = await browser.elements(PO.blocks.products.item());
            assert.equal(elementsAfterScroll.value.length, 10, 'Товары на нулевой странице не загрузились');

            await browser.yaScrollPageToBottom();
            await browser.yaWaitForVisible(PO.blocks.products.page1(), 'Вторая страница не загрузилась');

            const elementsAfter = await browser.elements(PO.blocks.products.item());
            assert.equal(elementsAfter.value.length, 20, 'Товары на второй странице не загрузились');

            await browser.yaScrollPageToBottom();
            await browser.yaWaitForVisible(PO.blocks.products.page2(), 'Третья страница не загрузилась');
        });

        it('Спиннер при загрузке следующей страницы', async function() {
            return this.browser
                .url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&srcrwr=SEARCH_SAAS_LISTINGS%3ASAAS_ANSWERS')
                .yaMockFetch({
                    status: 500,
                    delay: 2000,
                    urlDataMap: {
                        // Мокаем следующую страницу.
                        '&page=1&ajax=1': '{}',
                    },
                })
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .yaScrollPageToBottom()
                .yaWaitForVisible(PO.blocks.autoload2.spinner(), 'Спиннер загрузки следующей страницы не появился')
                .yaAssertViewportView('loading');
        });

        it('Загрузка нечетного количества товаров', function() {
            return this.browser
                .url('/turbo?stub=products%2Fodd-count-page-0.json')
                .yaWaitForVisible(PO.blocks.products.page0(), 'Не загрузилась предыдущая страница')
                .yaScrollPageToBottom()
                .yaWaitForVisible(PO.blocks.products.page1(), 'Не загрузилась следующая страница')
                .assertView('plain', PO.page.container());
        });

        it('Без сохранения позиции', function() {
            return this.browser
                .url('/turbo?stub=products/autoload-without-saving-position.json')
                .yaWaitForVisible(PO.products(), 'Карточки товаров не появились на странице')
                .yaScrollPageToBottom()
                .yaWaitForVisible(PO.products.autoload.content(), 4000, 'Данные не подгрузились')
                .url()
                .then(({ value }) => {
                    assert.notInclude(value, '#', 'В урл добавился якорь к элементу');
                    assert.notInclude(value, '&page', 'К урлу добавилась информация о странице');
                });
        });
    });
});
