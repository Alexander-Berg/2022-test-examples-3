const Entity = require('bem-page-object').Entity;

const fCandidatesTable = new Entity({ block: 'f-candidates-table' });
fCandidatesTable.pager = new Entity({ block: 'f-candidates-table', elem: 'pager' });
fCandidatesTable.row = new Entity({ block: 'f-candidates-table', elem: 'row' });

module.exports = fCandidatesTable;
