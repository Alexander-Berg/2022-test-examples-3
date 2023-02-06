const { Entity } = require('../../../../../../vendors/hermione');
const { serpTitle, showcase } = require('../../../../../../../hermione/page-objects/common/blocks');
const { oneOrg } = require('../../../../Companies.test/Companies.page-object/index@desktop');

const elems = {};

elems.companiesList = new Entity({ block: 't-construct-adapter', elem: 'companies' });
elems.oneOrg = oneOrg.copy();
elems.oneOrg.afishaCinema = new Entity({
    block: 'composite',
    elem: 'item',
    modName: 't-mod',
    modVal: 'afisha-cinema',
});
elems.oneOrg.afishaCinema.title = serpTitle.copy();
elems.oneOrg.afishaCinema.showcase = showcase.copy();

module.exports = elems;
