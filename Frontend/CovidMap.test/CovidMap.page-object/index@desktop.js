const { Entity } = require('../../../../vendors/hermione');

const elems = {};

elems.covidMap = new Entity({ block: 'virus-map' }).mods({ 't-mod': 'covid-map' });

module.exports = elems;
