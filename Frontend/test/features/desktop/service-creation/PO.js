const { create, Entity } = require('bem-page-object');

const PO = {};

PO.bPage = new Entity('.b-page');
PO.serviceCreation = new Entity('.ServiceCreation');
PO.serviceCreation.title = new Entity('.ServiceCreation-Title');

PO.serviceCreation.wizard = new Entity('.ServiceCreation-Wizard');
PO.serviceCreation.wizard.nextButton = new Entity('.Wizard-FooterButton_next');
PO.serviceCreation.wizard.backButton = new Entity('.Wizard-FooterButton_back');
PO.serviceCreation.wizard.submitButton = new Entity('.Wizard-FooterButton_submit');
PO.serviceCreation.wizard.menu = new Entity('.Menu');
PO.serviceCreation.wizard.menu.description = new Entity('[id="description"]');

PO.serviceCreation.nameInput = new Entity('.MainStep-Input_field_name');
PO.serviceCreation.nameInput.control = new Entity('.Textinput-Control');
PO.serviceCreation.nameInput.hint = new Entity('.Textinput-Hint');
PO.serviceCreation.englishNameInput = new Entity('.MainStep-Input_field_englishName');
PO.serviceCreation.englishNameInput.control = new Entity('.Textinput-Control');
PO.serviceCreation.englishNameInput.hint = new Entity('.Textinput-Hint');
PO.serviceCreation.slugInput = new Entity('.MainStep-Input_field_slug');
PO.serviceCreation.slugInput.control = new Entity('.Textinput-Control');
PO.serviceCreation.slugInput.hint = new Entity('.Textinput-Hint');
PO.serviceCreation.slugSuggestion = new Entity('.MainStep-Row_type_suitableSlug');
PO.serviceCreation.slugSuggestion.presetItem = new Entity('.Preset-Item');
PO.serviceCreation.ownerInput = new Entity('.MainStep-Input_field_owner');
PO.serviceCreation.ownerInput.control = new Entity('.Textinput-Control');

PO.serviceCreation.parentField = new Entity('.MainStep-Parent');
PO.serviceCreation.parentInput = new Entity('.MainStep-Input_field_parent');
PO.serviceCreation.parentInput.control = new Entity('.Textinput-Control');
PO.serviceCreation.parentSuggestion = new Entity('.MainStep-Row_type_suitableParents');
PO.serviceCreation.parentSuggestion.presetItem = new Entity('.Preset-Item');
PO.serviceCreation.tagsInput = new Entity('.MainStep-Input_field_tags');
PO.serviceCreation.tagsInput.control = new Entity('.Textinput-Control');

PO.serviceCreation.descriptionInput = new Entity('.DescriptionStep-Input_field_description');
PO.serviceCreation.descriptionInput.control = new Entity('.Textarea-Control');

PO.serviceCreation.englishDescriptionInput = new Entity('.DescriptionStep-Input_field_englishDescription');
PO.serviceCreation.englishDescriptionInput.control = new Entity('.Textarea-Control');

PO.martyTagChoice = new Entity('.Choice_type_Tags[title="MARTY"]');
PO.orangeParentChoice = new Entity('.Choice_type_ParentServices[title="Orange"]');

PO.serviceCreation.preview = new Entity('.PreviewStep');

PO.serviceCreation.preview.description = new Entity('.PreviewStep-Field_fieldId_description');
PO.serviceCreation.preview.description.wikiDocInited = new Entity('.wiki-doc_js_inited');
PO.serviceCreation.preview.englishDescription = new Entity('.PreviewStep-Field_fieldId_englishDescription');
PO.serviceCreation.preview.englishDescription.wikiDocInited = new Entity('.wiki-doc_js_inited');

PO.requests = new Entity('.Requests');
PO.requests.spin = new Entity('.Requests-Spin');
PO.requests.directFilterButton = new Entity('.Requests-FilterButton_type_direct');
PO.requests.checkedDirectFilterButton = new Entity('.Requests-FilterButton_type_direct.Button2_checked');
PO.requests.hierarchyFilterButton = new Entity('.Requests-FilterButton_type_hierarchy');
PO.requests.checkedHierarchyFilterButton = new Entity('.Requests-FilterButton_type_hierarchy.Button2_checked');

PO.requests.tableInfo = new Entity('.Requests-TableInfo');
PO.requests.firstRequest = new Entity('.AbcTable-TBody .AbcTable-Tr:first-child');

PO.requests.firstRequest.approveLink = new Entity('.Requests-DecisionLink_type_approve');
PO.requests.firstRequest.approvedLink = new Entity('.Requests-DecisionLink_approved');

PO.requests.firstRequest.rejectLink = new Entity('.Requests-DecisionLink_type_reject');
PO.requests.firstRequest.rejectedLink = new Entity('.Requests-DecisionLink_rejected');

PO.suggestPopup = new Entity('.ToolsSuggest-Popup');
PO.suggestPopup.suggestChoices = new Entity('.ToolsSuggest-Choices');
PO.suggestPopup.suggestChoices.staffChoice = new Entity('.Choice_type_staff');

PO.service = new Entity({ block: 'abc-service' });
PO.serviceDescription = new Entity({ block: 'abc-service', elem: 'description' });

module.exports = create(PO);
