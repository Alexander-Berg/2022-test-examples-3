'use strict';

const PO = require('../../page-objects/desktop').PO;

specs('Поисковая стрелка', function() {
    const firstRequest = 'test';
    const secondRequest = 'testqz';

    beforeEach(function() {
        return this.browser
            .yaOpenSerp({ text: firstRequest, exp_flags: 'hide-popups=1' })
            .yaWaitForVisible(PO.search(), 'Стрелка не появилась');
    });

    it('Проверка BackSpace', function() {
        return this.browser
            .setValue(PO.search.input(), 'qz')
            .yaKeyPress('BACKSPACE')
            .getValue(PO.search.input()).then(text =>
                assert.equal(text, 'q', 'Поле запроса содержит неверное значение')
            );
    });

    it('Очистка инпута', function() {
        return this.browser
            .click(PO.searchClear())
            .getValue(PO.search.input()).then(text =>
                assert.equal(text, '', 'Поле запроса не очищено после клика на крестик')
            );
    });

    it('Выполняется перезапрос с другим запросом', function() {
        return this.browser
            .setValue(PO.search.input(), secondRequest)
            .getValue(PO.search.input()).then(text =>
                assert.equal(text, secondRequest, 'Не удалось поменять запрос')
            )
            .yaWaitUntilPageReloaded(function() {
                this.click(PO.search.button());
            })
            .yaParseUrl().then(url =>
                assert.equal(url.query.text, secondRequest, 'Значение параметра text не изменилось')
            );
    });

    it("Запрос меняется при переходе по кнопке браузера 'назад'", function() {
        return this.browser
            .click(PO.searchClear())
            .setValue(PO.search.input(), secondRequest)
            .getValue(PO.search.input()).then(text => {
                assert.equal(text, secondRequest, 'Не удалось поменять запрос');
            })
            .yaWaitUntilPageReloaded(() => {
                this.browser.click(PO.search.button());
            })
            .getValue(PO.search.input()).then(text => {
                assert.equal(text, secondRequest, 'Потеряли измененный запрос после поиска');
            })
            .back()
            .yaWaitUntil('Запрос не стал прежним при переходе назад',
                () => this.browser.getValue(PO.search.input())
                    .then(text => text === firstRequest)
            );
    });

    it('Выполняется перезапрос с пустым запросом', function() {
        return this.browser
            .click(PO.searchClear())
            .yaWaitUntilPageReloaded(function() {
                this.click(PO.search.button());
            })
            .yaParseUrl().then(url =>
                assert.equal(url.query.text, '', 'Значение параметра text не изменилось')
            );
    });

    it('Проверка ссылок и счётчиков', function() {
        return this.browser
            .yaCheckBaobabCounter(PO.searchClear(), { path: '/$page/$header/arrow/clear' },
                'Не сработал счетчик на крестике в поисковой стрелке'
            )
            .yaCheckBaobabCounter(PO.search.button(),
                { path: '/$page/$header/arrow/button' },
                'Не сработал счетчик на кнопке найти в поисковой стрелке'
            );
    });

    it('Правая колонка', function() {
        return this.browser
            .yaShouldAllBeVisible(
                PO.rightColumn.adv.found(),
                'Элемент \'Нашлось n результатов\' должен отображаться'
            )
            .yaShouldAllBeVisible(
                PO.rightColumn.adv.wordstat(),
                'Количество показов в месяц должно отображаться'
            );
    });

    it('На пустом запросе статистика запроса не показывается', function() {
        return this.browser
            .yaOpenSerp({ text: '', exp_flags: 'hide-popups=1' })
            .yaShouldAllBeInvisible(PO.rightColumn.adv.found())
            .yaShouldAllBeInvisible(PO.rightColumn.adv.wordstat());
    });
});
