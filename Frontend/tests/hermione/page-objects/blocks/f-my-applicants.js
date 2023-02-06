const Entity = require('bem-page-object').Entity;

const { fApplicationsTable, fApplicationsTableLoadingYes } = require('./f-applications-table');
const fApplicationFilter = require('./f-applications-filter');

const fMyApplicants = new Entity({ block: 'f-my-applicants' });

fMyApplicants.header = new Entity({ block: 'f-my-applicants', elem: 'header' });
fMyApplicants.boardLink = new Entity({ block: 'f-my-applicants', elem: 'board-link' });

fMyApplicants.fApplicationsTable = fApplicationsTable.copy();
fMyApplicants.fApplicationsTableLoadingYes = fApplicationsTableLoadingYes.copy();

fMyApplicants.fApplicationFilter = fApplicationFilter.copy();

module.exports = fMyApplicants;
