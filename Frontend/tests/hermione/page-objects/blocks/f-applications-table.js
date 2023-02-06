const Entity = require('bem-page-object').Entity;
const blocks = require('../blocks/common');

const fApplicationsTable = new Entity({ block: 'f-applications-table' });
const fApplicationsTableLoadingYes = new Entity({ block: 'f-applications-table' }).mods({ loading: 'yes' });

fApplicationsTable.firstApplication = new Entity({ block: 'f-applications-table-row' }).nthChild(1);
fApplicationsTable.secondApplication = new Entity({ block: 'f-applications-table-row' }).nthChild(2);

fApplicationsTable.message = new Entity({ block: 'f-applications-table', elem: 'message'});

fApplicationsTable.firstApplication.details = new Entity({ block: 'f-applications-table-row', elem: 'cell' }).mods({ type: 'id' });
fApplicationsTable.firstApplication.details.link = blocks.link.copy();

fApplicationsTable.firstApplication.candidate = new Entity({ block: 'f-applications-table-row', elem: 'cell' }).mods({ type: 'candidate' });
fApplicationsTable.firstApplication.candidate.link = blocks.link.copy();

fApplicationsTable.firstApplication.status = new Entity({ block: 'f-applications-table-row', elem: 'cell' }).mods({ type: 'status' });
fApplicationsTable.firstApplication.status.text = new Entity({ block: 'f-application-field' }).mods({ type: 'status' });
fApplicationsTable.firstApplication.status.link = blocks.link.copy();

fApplicationsTable.firstApplication.vacancy = new Entity({ block: 'f-applications-table-row', elem: 'cell' }).mods({ type: 'vacancy' });
fApplicationsTable.firstApplication.vacancy.link = blocks.link.copy();
fApplicationsTable.firstApplication.vacancy.id = new Entity({ block: 'f-id' }).mods({ type: 'vacancy' });
fApplicationsTable.firstApplication.modified = new Entity({ block: 'f-applications-table-row', elem: 'cell' }).mods({ type: 'modified' });

fApplicationsTable.firstApplication.closeAction = new Entity({ block: 'f-application-workflow' }).mods({ action: 'close' });
fApplicationsTable.firstApplication.actions = new Entity({ block: 'f-applications-table-row', elem: 'cell' }).mods({ type: 'actions' });
fApplicationsTable.firstApplication.actions.button2 = blocks.button2.copy();
fApplicationsTable.firstApplication.actions.dropdown2 = blocks.dropdown2.copy();

fApplicationsTable.pager = new Entity({ block: 'f-applications-table', elem: 'pager' });
fApplicationsTable.pager.page2 = new Entity({ block: 'staff-pager', elem: 'page' }).nthChild(2);

fApplicationsTable.fTable = new Entity({ block: 'f-table' });

module.exports = {
    fApplicationsTableLoadingYes,
    fApplicationsTable,
};
