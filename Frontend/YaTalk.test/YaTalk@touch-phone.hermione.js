'use strict';

const PO = require('./YaTalk.page-object/');

specs({
    feature: 'Спецсниппет Яндекс.Толк',
}, function() {
    describe('Основные проверки', () => {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                { text: 'test', foreverdata: '1543017841', data_filter: { values: ['ya-talk', 'snippet-with-extended-preview', 'extended-snippet'], operation: 'AND' } },
                PO.yaTalk(),
            );
        });

        hermione.also.in(['iphone-dark']);
        it('Обязательные проверки', async function() {
            await this.browser.assertView('plain', PO.yaTalk());
            await this.browser.yaCheckLink2({ selector: PO.yaTalk.meta.link() });
        });
    });

    describe('Проверка гринурла с данными', () => {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                { text: 'test', foreverdata: '3400514339', data_filter: { values: ['ya-talk', 'snippet-with-extended-preview', 'extended-snippet'], operation: 'AND' } },
                PO.yaTalk(),
            );
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('plain', PO.yaTalk());
        });
    });
});
