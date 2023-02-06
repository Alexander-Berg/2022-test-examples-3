describe('Categories', function() {
    it('Переход в категорию с главной страницы', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });

        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.assertView('Category', '.CategoryList');
        let title1 = await browser.getText('.CategoryList-ItemContainer:nth-child(2) .Link p');
        await browser.click('.CategoryList-ItemContainer:nth-child(2) .Link');
        await browser.yaWaitForVisible('.ScreenContent .CategoryList');
        await browser.yaScrollPage('.ScreenContent .CategoryList', 0.3);
        await browser.assertView('Screen', ['.ScreenContent .Title', '.ScreenContent .CategoryList']);
        await browser.yaWaitUntil('Заголовок не совпадает с названием выбранной категории', async() => {
            let title2 = await browser.getText('.ScreenContent .Title');
            return title1.indexOf(title2) !== -1;
        });
    });

    it('Переход из категории на главную', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
        });

        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.yaScrollPage('.BottomBar', 0.3);
        await browser.click('.BottomBar-Item_type_main');
        await browser.yaWaitForVisible('.ScreenContent .CategoryList');
        await browser.assertView('Category', '.CategoryList');
        await browser.click('.CategoryList-ItemContainer:nth-child(1) .Link');
        await browser.yaWaitForVisible('.ScreenContent .Title', 'Не открылся список товаров');
        await browser.yaScrollPage('.BottomBar', 0.3);
        await browser.click('.BottomBar-Item_type_main');
        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.yaScrollPage('.BottomBar', 0.3);
        await browser.assertView('Bar', '.BottomBar');
    });

    it('Переход из категории на главную по истории', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });

        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.assertView('Category', '.CategoryList');
        await browser.click('.CategoryList-ItemContainer:nth-child(2) .Link');
        await browser.yaWaitForVisible('.ScreenContent .CategoryList');
        await browser.back();
        await browser.yaScrollPage('.BottomBar', 0.3);
        await browser.assertView('BarMain', '.BottomBar');
    });

    it('Переход из категории в основной каталог', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });

        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.assertView('Category', '.CategoryList');
        await browser.click('.CategoryList-ItemContainer:nth-child(1) .Link');
        await browser.yaWaitForVisible('.ScreenHeaderBack-Wrapper .Button2');
        await browser.yaWaitForHidden('.ScreenContent .CategoryList_skeleton');
        await browser.click('.ScreenHeaderBack-Wrapper .Button2');
        await browser.yaScrollPage('.BottomBar', 0.3);
        await browser.assertView('BarMain', '.BottomBar');
    });

    it('Переход из категории в другую категорию', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            query: {
                category_id: 1,
            },
        });

        await browser.yaWaitForVisible('.ScreenContent .CategoryList');
        await browser.yaScrollPage('.ScreenContent .Title', 0.3);
        await browser.assertView('Screen', ['.ScreenContent .Title', '.ScreenContent .CategoryList']);
        let title = await browser.getText('.ScreenContent .Title');
        await browser.click('.CategoryList-ItemContainer:nth-child(1) .Link');
        await browser.yaWaitForVisible('.ScreenHeaderBack-Wrapper .Button2');
        await browser.assertView('Botton', ['.ScreenContent .Title', '.ScreenHeaderBack-Wrapper']);
        await browser.yaWaitUntil('Cсылка в левом верхнем углу не совпадает с заголовком предыдущей страницы', async() => {
            let link = await browser.getText('.ScreenHeaderBack-Wrapper .Button2 .Button2-Text');
            return title === link;
        });
        await browser.click('.ScreenHeaderBack-Wrapper .Button2');
        await browser.yaWaitForVisible('.ScreenContent .CategoryList', 'Не открылась предыдущая страница');
    });

    it('Категории на главной странице разворачиваются по кнопке', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });

        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.yaScrollPage('.ScreenContent .CategoryList', 0.3);
        await browser.yaWaitForVisible('.CategoryList-ItemMore');
        await browser.assertView('Category', '.CategoryList');
        await browser.click('.CategoryList-ItemMore');
        await browser.yaScrollPage('.ScreenContent .CategoryList', 0.3);
        await browser.assertView('CategoryFull', '.CategoryList');
    });

    it('В каталоге категории всегда развернуты', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });
        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.yaScrollPage('.BottomBar', 0.3);
        await browser.click('.BottomBar-Item_type_catalog');
        await browser.yaWaitForHidden('.ScreenContent .CategoryList_skeleton');
        await browser.yaScrollPage('.ScreenContent .CategoryList', 0.1);
        await browser.assertView('CategoryList', '.CategoryList');
    });
});
