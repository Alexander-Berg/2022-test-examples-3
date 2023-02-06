'use strict';

specs({ feature: 'Связанные запросы', type: 'Объектные рекомендации' }, function() {
    it('Подвальные', async function() {
        const { PO, browser } = this;

        await browser.yaOpenSerp({
            text: 'джомолунгма',
            exp_flags: 'related_entity=1',
            src: 'rec_on_above',
        }, PO.related());

        await browser.yaScroll(PO.related.buttonWithThumb());
        const url = await browser.getAttribute(PO.related.buttonWithThumb(), 'href');
        assert.include(url, 'ento');
    });
});
