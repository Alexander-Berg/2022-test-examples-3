const inherit = require('inherit');
const { create, Entity } = require('bem-page-object');
const PO = {};

const ReactEntity = inherit(Entity, null, { preset: 'react' });

PO.pagerBlock = new ReactEntity({ block: 'AbcPagination' });
PO.pagerBlock.loadMoreButton = new ReactEntity({ block: 'AbcPagination', elem: 'LoadMore' });
PO.pagerBlock.nextPageButton = new ReactEntity({ block: 'AbcPagination', elem: 'Next' });
PO.pagerBlock.prevPageButton = new ReactEntity({ block: 'AbcPagination', elem: 'Prev' });

PO.resourcesInService = new Entity({ block: 'resources', elem: 'content' });
PO.resourcesTable = new Entity({ block: 'abc-resources-table', elem: 'tbody' });
PO.resourcesTable.rowsInResourceTable = new Entity({ block: 'abc-resources-table', elem: 'tr_type_body' });

PO.resourceSpinner = new Entity({ block: 'resources', elem: 'spin' });

PO.resourceModal = new Entity({ block: 'abc-resource-view' });
PO.resourceModalAttributes = new Entity({ block: 'abc-resource-view', elem: 'attributes' });
PO.resourceModal.resourceSpinner = new Entity({ block: 'abc-resource-view', elem: 'spin' });
PO.resourceModal.deleteOldSecretButton = new Entity('[data-action="delete_old_secret"]');
PO.resourceModal.recreateSecretButton = new Entity('[data-action="recreate_secret"]');
PO.resourceModal.closeModal = new Entity({ block: 'abc-resource-view', elem: 'cancel' });
PO.resourceModal.emptyAttribute = new Entity({ block: 'abc-resource-view', elem: 'null' });

module.exports = create(PO);
