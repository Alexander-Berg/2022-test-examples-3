const inherit = require('inherit');
const { create, Entity } = require('bem-page-object');
const PO = {};

const ReactEntity = inherit(Entity, null, { preset: 'react' });

PO.tree = new ReactEntity({ block: 'Tree' });
PO.tree.services = new ReactEntity({ block: 'AbcTable', elem: 'TBody' });
PO.tree.services.firstService = new ReactEntity({ block: 'AbcTable', elem: 'Tr' }).firstOfType();
PO.tree.services.firstService.openTreeButton = new ReactEntity({ block: 'Icon' }).mods({ direction: 'right' });
PO.tree.services.firstAutotestChild = new Entity('[href="/services/autotest-descendant-1-1/"]');

PO.filters = new ReactEntity({ block: 'Filters' });
PO.filters.externals = new ReactEntity({ block: 'Filters', elem: 'Group' }).mods({ type: 'externals' });
PO.filters.externals.withExternalsCheckbox = new ReactEntity({ block: 'Checkbox' }).firstOfType();
PO.filters.externals.withExternalsCheckbox.input = new Entity('input');

PO.summary = new ReactEntity({ block: 'Catalogue', elem: 'Summary' });
PO.summary.withExternalsLabel = new ReactEntity({ block: 'Summary', elem: 'Item' }).mods({ hasExternalMembers: true });

PO.spinner = new Entity({ block: 'Spin2' });

module.exports = create(PO);
