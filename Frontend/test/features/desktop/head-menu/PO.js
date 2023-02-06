const { create, Entity } = require('bem-page-object');

const PO = {};

PO.header = new Entity({ block: 'b-page', elem: 'header' });
PO.header.createServiceButton = new Entity('.button[href="/create-service"]');
PO.header.search = new Entity({ block: 'abc-search', elem: 'row' });
PO.header.search.input = new Entity('input');

PO.searchSuggest = new Entity({ block: 'popup_visibility_visible' });
PO.searchSuggest.autotestService = new Entity('[data-data*="Автотестовый сервис для запроса ролей 1"]');

PO.services = new Entity({ block: 'Catalogue' });

module.exports = create(PO);
