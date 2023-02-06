'use strict';

const PO = require('./OrgTile.page-object/index@desktop');

specs({
    feature: 'Одна организация',
    type: 'Горнолыжный курорт',
}, function() {
    h.it('Карточки услуг – внешний вид и клики', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '2772065109',
            data_filter: 'companies',
        }, PO.OrgTile());

        await this.browser.moveToObject(PO.OrgTileListItem());
        await this.browser.assertView('OrgTile', PO.OrgTile());

        await this.browser.yaCheckLink2({
            selector: PO.OrgTileListItemAvia(),
            target: '_blank',
            baobab: {
                path: '/$page/$parallel/$result/composite/tabs/about/ski-services/avia',
            },
        });

        await this.browser.yaCheckLink2({
            selector: PO.OrgTileListItemHotels(),
            target: '_self',
            baobab: {
                path: '/$page/$parallel/$result/composite/tabs/about/ski-services/hotels',
            },
        });
    });

    h.it('Карточки услуг – мало карточек (без билетов и отелей)', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '1002135799',
            data_filter: 'companies',
        }, PO.OrgTile());

        await this.browser.assertView('OrgTile2', PO.OrgTile());
    });
});
