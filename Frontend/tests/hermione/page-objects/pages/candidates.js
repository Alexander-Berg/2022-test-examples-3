const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;

const blocks = require('../blocks/common');
const fInfo = require('../blocks/f-info');
const fCandidatesFilter = require('../blocks/f-candidates-filter');
const fCandidatesTable = require('../blocks/f-candidates-table');
const fCandidatesTableRow = require('../blocks/f-candidates-table-row');

blocks.pageCandidates = new Entity({ block: 'f-page-candidates' });

blocks.pageCandidates.createButton = new Entity({ block: 'f-page-candidates', elem: 'create' });
blocks.pageCandidates.filter = fCandidatesFilter.copy();
blocks.pageCandidates.filterInformer = new Entity({ block: 'f-page-candidates', elem: 'filter-informer' });
blocks.pageCandidates.header = new Entity({ block: 'f-page-candidates', elem: 'header' });

blocks.pageCandidates.message = new Entity({ block: 'f-page-candidates', elem: 'message' });
blocks.pageCandidates.message.exampleLink = blocks.link.copy();
blocks.pageCandidates.search = new Entity({ block: 'f-page-candidates', elem: 'search' });
blocks.pageCandidates.search.button = blocks.button2.copy();
blocks.pageCandidates.search.input = blocks.input.control.copy();

blocks.pageCandidates.table = new Entity({ block: 'f-page-candidates', elem: 'table' });
blocks.pageCandidates.table.pager = fCandidatesTable.pager.copy().mix(blocks.staffPager);
blocks.pageCandidates.table.row = fCandidatesTableRow.copy();
blocks.pageCandidates.toggleFilterButton = blocks.input.settings.copy();

blocks.popup2Visible.fInfo = fInfo.copy();
blocks.popup2Visible.fInfoBody = fInfo.body.copy();
blocks.popup2Visible.fInfoBody.link = blocks.link.copy();
blocks.popup2Visible.fInfoCloser = fInfo.closer.copy();

blocks.onSiteInterviewsAvgGradeSelect = blocks.select2Popup.copy();
blocks.onSiteInterviewsAvgGradeSelect.intern = blocks.select2Item.nthType(2);
blocks.onSiteInterviewsAvgGradeSelect.specialist4 = blocks.select2Item.nthType(5);
blocks.sortSelect = blocks.select2Popup.copy();
blocks.sortSelect.fromOldToNew = blocks.select2Item.nthType(2);
blocks.professionsSuggest = blocks.bAutocompletePopup.copy();
blocks.professionsSuggest.first = blocks.bAutocompletePopupItem.nthType(1);
blocks.responsiblesSuggest = blocks.bAutocompletePopup.copy();
blocks.responsiblesSuggest.first = blocks.bAutocompletePopupItem.nthType(1);
blocks.skypeInterviewsAvgGradeSelect = blocks.select2Popup.copy();
blocks.skypeInterviewsAvgGradeSelect.specialist4 = blocks.select2Item.nthType(5);

blocks.skillsSuggest = blocks.bAutocompletePopup.copy();
blocks.skillsSuggest.javascript = blocks.bAutocompletePopupItem.nthType(1);
blocks.tagsSuggest = blocks.bAutocompletePopup.copy();
blocks.tagsSuggest.first = blocks.bAutocompletePopupItem.nthType(1);
blocks.targetCitiesSuggest = blocks.bAutocompletePopup.copy();
blocks.targetCitiesSuggest.first = blocks.bAutocompletePopupItem.nthType(1);

module.exports = pageObject.create(blocks);
