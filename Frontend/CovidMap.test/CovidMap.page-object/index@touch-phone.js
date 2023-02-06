const { organic } = require('../../../../../hermione/page-objects/common/construct/organic');
const { create, Entity } = require('../../../../vendors/hermione');

const elems = {};

elems.covidMap = new Entity({ block: 'composite' }).mods({ 't-mod': 'covid-map' });

elems.covidMap.organic = organic.copy();

elems.covidMap.map = new Entity({ block: 'map2' });
elems.covidMap.map.link = new Entity({ block: 'link' });

module.exports = create(elems);
