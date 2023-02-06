describe('ProductPage / Навигация по SKU', function() {
    beforeEach(async function() {
        const bro = this.browser;
        const browserId = bro.executionContext.browserId;

        await bro.yaOpenPageByUrl(`/products/product/1441144417/sku/101465383818?promo=nomooa&exp_flags=${browserId === 'iphone-dark' ? 'dark_theme_touch=dark;' : ''}PRODUCTS_card_enable_navigation=1`);

        await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
        await bro.yaWaitForVisible('.ProductNavigation', 'Навигационная панель не отобразилась');
        await bro.yaWaitForVisible('.ProductNavigation-More', 'Навигационная панель не отобразилась');
        await bro.yaWaitForVisible('.InlineBorder', 'Псевдоразделитель не отобразился');
        await bro.yaWaitForVisible('.Card-ProductAboutTitle', 'Заголовок характеристик не отобразился');
    });

    hermione.also.in('iphone-dark');
    it('Внешний вид', async function() {
        await this.browser.assertView('navigation', ['.ProductNavigation', '.Card-ProductAboutTitle']);
    });

    hermione.also.in('iphone-dark');
    it('Внешний вид кнопки развертывания скрытых элементов навигации', async function() {
        await this.browser.assertView('navigation-more-button', '.ProductNavigation-More');
    });

    it('Выбор элемента навигации', async function() {
        const bro = this.browser;
        await bro.yaWaitForVisible(
            '.ProductNavigation-Group:nth-child(1) .ProductNavigation-Item:nth-child(3)',
            ' Первый элемент первой группы навигации на отобразился',
        );
        await bro.yaShouldNotExist(
            '.ProductNavigation-Group:nth-child(1) .ProductNavigation-Item:nth-child(3).ProductNavigation-Item_active',
            'Второй элемент первой группы навигации должен быть не активен',
        );

        // Подскраливаем страницу, чтобы убедиться, что после загрузки новой sku позиция скрола сохранится
        await bro.execute(() => window.scroll(0, 250));

        // Подскраливаем группу навигации, чтобы убедиться, что после загрузки новой sku позиция скрола сохранится
        await bro.execute(selector => {
            const largeGroup = document.querySelector(selector);

            largeGroup?.scroll(7, 0);
        }, '.ProductNavigation-Group:nth-child(1) .NativeScroll-Inner'); // в дампе эта группа довольно большая, чтобы у нее был скролл

        await bro.click('.ProductNavigation-More');

        // Сохраняем текст заголовка карточки для проверки загрузки новой sku
        const initialTitle = await bro.execute(selector => {
            const titleNode = document.querySelector(selector);

            return titleNode?.innerHTML || '';
        }, '.Card-Title');

        // Сохраняем позицию скрола страницы для проверки подскролла после загрузки новой sku
        const initialPageScroll = await bro.execute(() => document.documentElement.scrollTop);

        // Сохраняем позицию скрола навигационной группы для проверки подскролла после загрузки новой sku
        const initialNavigationGroupScroll = await bro.execute(selector => {
            const largeGroup = document.querySelector(selector);

            return largeGroup?.scrollLeft || 0;
        }, '.ProductNavigation-Group:nth-child(1) .NativeScroll-Inner'); // в дампе эта группа довольно большая, чтобы у нее был скролл

        await assert.isTrue(initialNavigationGroupScroll > 0, 'Навигационная группа не проскролилась');

        await bro.click('.ProductNavigation-Group:nth-child(1) .ProductNavigation-Item:nth-child(3)');

        // Проверяем текст заголовка карточки чтобы убедиться, что приехали новые данные
        await bro.waitUntil(async() => {
            const resultTitle = await bro.execute(selector => {
                const titleNode = document.querySelector(selector);

                return titleNode?.innerHTML || '';
            }, '.Card-Title');

            return initialTitle !== resultTitle;
        }, {
            timeout: 2000,
            timeoutMsg: 'Данные новой карточки не загрузились',
        });

        // Проверяем восстановление скролла страницы, после загрузки новых данных
        await bro.waitUntil(async() => {
            const resultPageScroll = await bro.execute(() => document.documentElement.scrollTop);

            return initialPageScroll === resultPageScroll;
        }, {
            timeout: 2000,
            timeoutMsg: 'Скролл страницы не восстановился',
        });

        // Проверяем восстановление скролла навигационной группы, после загрузки новых данных
        await bro.waitUntil(async() => {
            const resultNavigationGroupScroll = await bro.execute(selector => {
                const largeGroup = document.querySelector(selector);

                return largeGroup?.scrollLeft || 0;
            }, '.ProductNavigation-Group:nth-child(1) .NativeScroll-Inner'); // в дампе эта группа довольно большая, чтобы у нее был скролл

            return resultNavigationGroupScroll === initialNavigationGroupScroll;
        }, {
            timeout: 2000,
            timeoutMsg: 'Скролл навигационной группы не восстановился',
        });

        // Проверяем восстановление рассхлопа навигации, после загрузки новых данных
        await bro.yaShouldNotExist('.ProductNavigation-More', 'Блок навигации схлопнулся');
    });
});
