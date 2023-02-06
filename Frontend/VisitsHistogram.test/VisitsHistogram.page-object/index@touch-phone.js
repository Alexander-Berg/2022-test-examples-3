const { Entity } = require('../../../../../../vendors/hermione');
const { oneOrg, overlayOneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { visitsHistogram } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.visitsHistogram = visitsHistogram.copy();

elems.overlay = new Entity({ block: 'overlay', elem: 'panel' });
elems.overlay.oneOrg = overlayOneOrg.copy();
elems.overlay.oneOrg.visitsHistogram = elems.oneOrg.visitsHistogram.copy();
elems.overlay.back = new Entity({ block: 'navigation-bar', elem: 'action', modName: 'name', modVal: 'back' });

module.exports = elems;
