'use strict';

const TranslatePO = require('./Translate.page-object');
let PO;

specs('Колдунщик переводов', function() {
    beforeEach(function() {
        PO = TranslatePO(this.currentPlatform);

        return this.browser;
    });

    it('При вводе слова \'test\' появляется результат с переводом', async function() {
        await this.browser.yaOpenSerp({ text: 'переводчик' }, PO.translate());
        await this.browser.setValue(PO.translate.textArea.control(), 'test');

        // execute нужен, чтобы убрать фокус и каретку в инпуте со скриншота
        await this.browser.execute(function(input) {
            window.$(input).blur();
        }, PO.translate.textArea.control());

        await this.browser.yaWaitForVisible(PO.translate.resultDict(), 'Не появился результат с переводом');

        await this.browser.yaWaitUntil('В результате нет перевода', function() {
            return this.getText(PO.translate.resultText()).then(value => value === 'тест');
        });
    });

    it('Проверка заполненного к-ка', async function() {
        await this.browser.yaOpenSerp({ text: 'кот перевод' }, PO.translate());
        await this.browser.yaWaitForVisible(PO.translate.resultText(), 'Не появился результат с переводом');
        const text = await this.browser.getText(PO.translate.result());
        assert.include(text, 'cat', 'В результате нет перевода');
    });
});
