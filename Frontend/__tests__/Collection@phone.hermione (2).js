describe('Ecom-tap', function() {
    describe('Collection', function() {
        it('Внешний вид блока', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                expFlags: { 'turbo-app-morda-redesign': 1 },
            });

            await browser.yaWaitForVisible('.RedesignedCollection');
            await browser.yaMockImages();
            await browser.yaScrollPage('.RedesignedCollection', 0);
            await browser.assertView('plain', '.RedesignedCollection');
        });

        [3, 4, 7].forEach(count => {
            it(`Внешний вид карточки с ${count} изображениями`, async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'main',
                    expFlags: { 'turbo-app-morda-redesign': 1 },
                    query: {
                        patch: 'setCollectionItems',
                        imagesCount: count,
                    },
                });

                await browser.yaWaitForVisible('.RedesignedCollection');
                await browser.yaMockImages();
                await browser.yaScrollPage('.RedesignedCollection', 0);
                await browser.assertView('plain', '.RedesignedCollection-Item:first-child');
            });
        });

        hermione.only.notIn('iphone', 'В тестовом iphone нестабильно работает подскролл');
        it('Сохранение позиции скролла при навигации', async function() {
            const browser = this.browser;
            const scrollX = 50;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                expFlags: { 'turbo-app-morda-redesign': 1 },
            });

            await browser.yaWaitForVisible('.EcomScreen_type_main');
            await browser.yaScrollPage('.RedesignedCollection', 0);
            await browser.yaShouldBeScrollable('.RedesignedCollection-Content', { h: true });
            await browser.yaScrollElement('.RedesignedCollection-Content', scrollX);

            // Пробуем стабилизировать тест, чтобы изменение сохранилось, перед уходом со страницы
            await browser.yaWaitUntil('Карусель не прокручена до нужного места первый раз', () =>
                browser.execute(function() {
                    return document.querySelector('.RedesignedCollection-Content').scrollLeft;
                })
                    .then(({ value }) => value === scrollX)
            );
            await browser.click('.RedesignedCollection-Item:nth-child(2)');
            await browser.yaWaitForVisible('.EcomScreen_type_product-list');
            await browser.back();
            await browser.yaWaitForVisible('.EcomScreen_type_main', null, 'Не появилась главная страница после клика назад');
            await browser.yaWaitUntil(
                'Карусель не прокручена до нужного места',
                () => browser.execute(function() {
                    return document.querySelector('.RedesignedCollection-Content').scrollLeft;
                })
                    .then(({ value }) => value === scrollX),
                5000,
                300,
            );
        });

        hermione.only.notIn('iphone', 'В тестовом iphone нестабильно работает подскролл');
        it('Подгрузка данных при скролле', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'bealab.ru',
                pageType: 'main',
                expFlags: { 'turbo-app-morda-redesign': 1 },
            });

            await browser.yaWaitForVisible('.EcomScreen_type_main');
            await browser.yaScrollPage('.RedesignedCollection', 0);

            const { value: itemsCount } = await browser.execute(() => {
                return document.querySelectorAll('.RedesignedCollection-Item').length;
            });
            assert.equal(itemsCount, 4, 'На странице не правильное количество карточек коллекций');

            await browser.yaShouldBeScrollable('.RedesignedCollection-Content', { h: true });
            await browser.yaScrollElement('.RedesignedCollection-Content', 5000);

            await browser.yaWaitUntil(
                'Карточки коллекции не подгрузились после скролла',
                () => browser.execute(function() {
                    return document.querySelectorAll('.RedesignedCollection-Item').length;
                })
                    .then(({ value }) => value === 8),
                5000,
                500,
            );
        });
    });
});
