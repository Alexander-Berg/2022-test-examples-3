const { create, Entity } = require('bem-page-object');

const PO = {};

PO.tree = new Entity({ block: 'Tree' });
PO.treeDisabled = PO.tree.mods({ disabled: true });

PO.tree.row = new Entity('.Tree-Row');
PO.tree.message = new Entity('.Tree-Message');

PO.catalogue = new Entity({ block: 'Catalogue' });
PO.catalogue.aside = new Entity('.Catalogue-Aside');
PO.catalogue.spin = new Entity('.Spin2');
PO.catalogue.treeRow = PO.tree.row;

PO.preset = new Entity({ block: 'Preset' });
PO.preset.item = new Entity('.Preset-Item');

PO.filters = new Entity({ block: 'Filters' });
PO.filters.group = new Entity('.Filters-Group');

PO.filters.groupTypeSearch = PO.filters.group.mods({ type: 'search' });
PO.filters.groupTypeSearch.textinputControl = new Entity('.Textinput-Control');

PO.filters.groupTypeMember = PO.filters.group.mods({ type: 'member' });
PO.filters.groupTypeMember.textinputControl = new Entity('.textinput__control');
PO.filters.groupTypeMember.presetItem = PO.preset.item;

PO.filters.groupTypeOwner = PO.filters.group.mods({ type: 'owner' });
PO.filters.groupTypeOwner.textinputControl = new Entity('.textinput__control');

PO.filters.groupTypeDepartment = PO.filters.group.mods({ type: 'department' });
PO.filters.groupTypeDepartment.textinputControl = new Entity('.textinput__control');

PO.filters.groupTypeStates = PO.filters.group.mods({ type: 'states' });
PO.filters.groupTypeStates.inDevelopment = new Entity('#label-states-develop');

PO.filters.groupTypeIsSuspicious = PO.filters.group.mods({ type: 'isSuspicious' });
PO.filters.groupTypeIsSuspicious.perfect = new Entity('#label-is-suspicious-false');
PO.filters.groupTypeIsSuspicious.withProblems = new Entity('#label-is-suspicious-true');

PO.filters.groupTypeExternals = PO.filters.group.mods({ type: 'externals' });
PO.filters.groupTypeExternals.withExternals = new Entity('#label-externals-true');

PO.filters.groupTypeTags = PO.filters.group.mods({ type: 'tags' });
PO.filters.groupTypeTags.textinputControl = new Entity('.textinput__control');

PO.suggestVisible = new Entity('.ta-suggest-control__popup.popup2_visible_yes');
PO.suggestVisible.firstItem = new Entity('.ta-suggest-item').firstChild();

module.exports = create(PO);
