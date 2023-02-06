const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;

const blocks = require('../blocks/common');
const fTabs = require('../blocks/f-tabs');

blocks.pageProblem = new Entity({ block: 'f-page-problem' });
blocks.pageProblem.tabs = fTabs.copy();
blocks.pageProblem.assignmetsList = new Entity({ block: 'f-problem-assignments' });

module.exports = pageObject.create(blocks);
