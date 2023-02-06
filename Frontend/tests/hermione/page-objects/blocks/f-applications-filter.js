const Entity = require('bem-page-object').Entity;
const blocks = require('./common');

const block = 'f-applications-filter';

const fApplicationsFilter = new Entity({ block });

fApplicationsFilter.vacancyFilter = new Entity({ block, elem: 'field' }).mods({ type: 'vacancy' });

fApplicationsFilter.stageFilter = blocks.radioButton;

module.exports = fApplicationsFilter;
