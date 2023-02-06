const { ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

elems.statefulEntry = new ReactEntity({ block: 'StatefulEntry' });
elems.statefulEntry.titleLink = new ReactEntity({ block: 'StatefulEntry', elem: 'TitleLink' });

module.exports = elems;
