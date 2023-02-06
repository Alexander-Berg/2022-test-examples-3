specs({
    feature: 'RelatedDesktop',
}, () => {
    it('Блок не рендерится, если в данных приходит блок related', async function() {
        await this.browser.url('/turbo?stub=related%2Fdefault.json');
        await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
        await this.browser.yaWaitForVisible(PO.related());

        const { value: exist } = await this.browser.elements('.related-root');

        assert.isFalse(Boolean(exist.length), 'На странице есть блок related-root');
    });

    it('Блок рендерит сниппеты из autoload', async function() {
        await this.browser.url('/turbo?stub=relateddesktop/no-related.json');
        await this.browser.windowHandleSize({ width: 1500, height: 800 });
        await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась');
        await this.browser.yaWaitForVisible(PO.blocks.related());
        await this.browser.yaWaitForVisible(PO.blocks.relatedList());

        const { value: isSnippets } = await this.browser.elements('.related .snippet');

        assert.isTrue(Boolean(isSnippets.length), 'Нет сниппетов в блоке related');
    });
});
