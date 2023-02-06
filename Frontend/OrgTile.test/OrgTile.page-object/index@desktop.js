const { create, ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.OrgTile = new ReactEntity({ block: 'OrgTile' });
elems.OrgTileListItem = new ReactEntity({ block: 'OrgTile', elem: 'List-Item' });
elems.OrgTileListItemAvia = elems.OrgTileListItem.mods({ Avia: true });
elems.OrgTileListItemHotels = elems.OrgTileListItem.mods({ Hotels: true });

module.exports = create(elems);
