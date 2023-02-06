'use strict';

specs({
    feature: 'Метка свежести',
    type: 'Доступность',
}, function() {
    it('Проверка на доступность', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '497091340',
            user_time: '20190610',
            data_filter: 'special-dates',
        }, PO.serpList.snippetWithLabel());

        const ariaLabel = await this.browser.getAttribute(PO.serpList.snippetWithLabel.blueLabel(), 'aria-label');
        assert.equal(ariaLabel, 'Опубликовано вчера');
    });
});
