const { create, ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.OrgTile = new ReactEntity({ block: 'OrgTile' });
elems.OrgTileListItem = new ReactEntity({ block: 'OrgTile', elem: 'List-Item' });
elems.OrgTileListItemCamera = elems.OrgTileListItem.mods({ Camera: true });
elems.OrgTileListItemWeather = elems.OrgTileListItem.mods({ Weather: true });

elems.composite = new ReactEntity({ block: 'composite' });
elems.composite.PhoneMore = new ReactEntity({ block: 'OrgContacts', elem: 'PhoneMore' });

module.exports = create(elems);
