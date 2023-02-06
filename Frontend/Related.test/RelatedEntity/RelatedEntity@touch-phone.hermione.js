'use strict';

specs({ feature: 'Связанные запросы', type: 'Объектные рекомендации' }, function() {
    describe('Подвальные', function() {
        beforeEach(async function() {
            const { PO, browser } = this;

            await browser.yaOpenSerp({
                text: 'джомолунгма',
                exp_flags: 'related_entity=1',
                src: 'rec_on_above',
            }, PO.related());

            await browser.yaWaitForVisible(PO.related.buttonWithThumb(), 'Связанные запросы c ОО обогащением не появились');
        });

        hermione.also.in('iphone-dark');
        it('Проверка внешного вида', async function() {
            const { PO, browser } = this;

            await browser.yaScroll(PO.related.buttonWithThumb());
            await browser.assertView('plain', PO.related.buttonWithThumb());
        });

        it('Ссылка содержит параметр для ОО на выдаче', async function() {
            const { PO, browser } = this;

            const url = await browser.getAttribute(PO.related.buttonWithThumb(), 'href');

            assert.include(url, 'ento');
        });
    });

    describe('Залипающие', function() {
        beforeEach(async function() {
            const { PO, browser } = this;

            await browser.yaOpenSerp({
                text: 'джомолунгма',
                src: 'rec_on_above',
            }, PO.organic());
            await browser.yaWaitForVisible(PO.relatedSticky.buttonWithThumb(), 'Связанные запросы c ОО обогащением не появились');
        });

        it('Проверка внешного вида', async function() {
            const { PO, browser } = this;

            await browser.yaScroll(PO.relatedSticky.buttonWithThumb());
            await browser.assertView('plain', PO.relatedSticky.buttonWithThumb(), { allowViewportOverflow: true });
        });

        it('Ссылка содержит параметр для ОО на выдаче', async function() {
            const { PO, browser } = this;

            const url = await browser.getAttribute(PO.relatedSticky.buttonWithThumb(), 'href');

            assert.include(url, 'ento');
        });
    });
});
