const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;

const blocks = require('../blocks/common');
const fCandidateForm = require('../blocks/f-candidate-form');

blocks.pageForm = new Entity({ block: 'f-page-form' });

blocks.pageForm.candidateForm = fCandidateForm.copy();

blocks.sourceSelect = blocks.select2Popup.copy();

blocks.sourceSelect.first = blocks.select2Item.nthType(1);
blocks.sourceSelect.second = blocks.select2Item.nthType(2);

blocks.targetCities = blocks.bAutocompletePopup.copy();
blocks.targetCities.moscow = blocks.bAutocompletePopupItem.nthType(1);
blocks.targetCities.peter = blocks.bAutocompletePopupItem.nthType(1);

blocks.responsibles = blocks.bAutocompletePopup.copy();
blocks.responsibles.user3993 = blocks.bAutocompletePopupItem.nthType(1);
blocks.responsibles.olgakozlova = blocks.bAutocompletePopupItem.nthType(1);

module.exports = pageObject.create(blocks);
