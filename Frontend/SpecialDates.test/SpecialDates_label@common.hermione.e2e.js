'use strict';

specs({
    feature: 'Сниппеты',
    type: 'Лейбл метки свежести и отступ',
}, function() {
    it('Базовая проверка', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: 'путин',
            within: '77',
        }, PO.serpList.snippetWithLabel());

        await this.browser.yaWaitForVisible(
            PO.serpList.snippetWithLabel.blueLabel(),
            'Не найдена ни одна метка свежести на странице',
        );

        const text = await this.browser.getText(PO.serpList.snippetWithLabel.blueLabel());
        assert.ok(text, 'Пустая метка свежести');
    });
});
