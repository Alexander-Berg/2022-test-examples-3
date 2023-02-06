'use strict';

const PO = require('./Fastres.page-object');

specs('Колдунщик миниблендера', function() {
    describe('Сниппет', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '1974333207',
                text: 'foreverdata',
                data_filter: 'fastres',
            }, PO.fastres());
        });

        it('Гринурл', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.fastres.greenurl.item(),
                target: '_blank',
                clickCoords: [10, 2],
                baobab: {
                    path: '/$page/$main/$result/path/urlnav',
                },
            });
        });
    });
});
