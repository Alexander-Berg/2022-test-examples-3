describe('b-page__old-browsers', function() {
    var block;

    describe('Для не-Яндекс.Браузера', function() {
        before(function() {
            var ctx = {
                block: 'b-page',
                elem: 'old-browsers',
                updateBrowserLink: 'https://www.google.com/chrome',
                isYaBrowser: false,
                yaBrowserLink: 'https://browser.yandex.ru'
            };

            block = u.createBlock(ctx);
        });

        after(function() {
            block.destruct();
        });

        it('Нет элемента со ссылкой на скачивание обновления', function() {
            expect(block.findBlockInside('update-link', 'link')).to.be.null;
        });

        it('Есть кнопка со ссылкой на установку Я.Браузера', function() {
            expect(block.findElem('ya-browser-button')).not.to.be.null;
        });

        it('На кнопке со ссылкой на установку Я.Браузера верная ссылка', function() {
            expect(block.findElem('ya-browser-button').attr('href')).to.equal('https://browser.yandex.ru');
        });

        it('На кнопке со ссылкой на установку Я.Браузера текст "Установить"', function() {
            expect(block.findElem('ya-browser-button-text').text()).to.equal('Установить');
        });
    });

    describe('Для Яндекс.Браузера', function() {
        before(function() {
            var ctx = {
                block: 'b-page',
                elem: 'old-browsers',
                updateBrowserLink: 'https://browser.yandex.ru',
                isYaBrowser: true,
                yaBrowserLink: 'https://browser.yandex.ru'
            };

            block = u.createBlock(ctx);
        });

        after(function() {
            block.destruct();
        });

        it('Нет элемента со ссылкой на скачивание обновления', function() {
            expect(block.findBlockInside('update-link', 'link')).to.be.null;
        });

        it('Есть кнопка со ссылкой на установку Я.Браузера', function() {
            expect(block.findElem('ya-browser-button')).not.to.be.null;
        });

        it('На кнопке со ссылкой на установку Я.Браузера верная ссылка', function() {
            expect(block.findElem('ya-browser-button').attr('href')).to.equal('https://browser.yandex.ru');
        });

        it('На кнопке со ссылкой на установку Я.Браузера текст "Обновить"', function() {
            expect(block.findElem('ya-browser-button-text').text()).to.equal('Обновить');
        });
    })
});
