'use strict';

specs({ feature: 'Связанные запросы', experiment: 'Объектные рекомендации' }, function() {
    describe('Подскрольные', function() {
        beforeEach(async function() {
            const { PO, browser } = this;

            await browser.yaOpenSerp({
                text: 'джомолунгма',
                exp_flags: ['related-above-enable-for-testing'],
            }, PO.organic());
            await browser.yaScroll(401);
            await browser.yaScroll(0);
            await browser.yaWaitForVisible(PO.relatedAbove.buttonWithThumb(), 'Связанные запросы c ОО обогащением не появились');
        });

        it('Проверка внешного вида', async function() {
            const { PO, browser } = this;

            await browser.yaScroll(PO.relatedAbove.buttonWithThumb());
            await browser.assertView('plain', PO.relatedAbove.buttonWithThumb());
        });

        it('Ссылка содержит параметр для ОО на выдаче', async function() {
            const { PO, browser } = this;

            const url = await browser.getAttribute(PO.relatedAbove.buttonWithThumb(), 'href');

            assert.include(url, 'ento');
        });
    });

    hermione.only.notIn('searchapp-phone', 'В searchapp такая работа со вкладками не допустима');
    describe('Повозвратные', function() {
        beforeEach(async function() {
            const { PO, browser } = this;
            let prevTabId;

            await browser.yaOpenSerp({
                text: 'самые красивые места кольского полуострова',
            }, PO.organic());

            const id = await browser.getCurrentTabId();
            prevTabId = id;
            await browser.click(PO.organic.title.link());
            await browser.switchTab(prevTabId);
            await browser.moveToObject('body', 1, 1);
            await browser.execute(function() {
                document.dispatchEvent(new Event('visibilitychange'), {
                    bubbles: true,
                });
            });

            await browser.yaWaitForVisible(PO.relatedHidden.buttonWithThumb(), 'Связанные запросы c ОО обогащением не появились');
        });

        hermione.also.in('iphone-dark');
        it('Проверка внешного вида', async function() {
            const { PO, browser } = this;

            if (!await browser.isVisibleWithinViewport(PO.relatedHidden.buttonWithThumb())) {
                await browser.yaScroll(PO.relatedHidden.buttonWithThumb());
            }
            await browser.assertView('plain', PO.relatedHidden.buttonWithThumb());
        });

        it('Ссылка содержит параметр для ОО на выдаче', async function() {
            const { PO, browser } = this;

            const url = await browser.getAttribute(PO.relatedHidden.buttonWithThumb(), 'href');

            assert.include(url, 'ento');
        });
    });
});
