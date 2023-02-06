const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;
const blocks = require('../blocks/common');
const fApplicationWorkflow = require('../blocks/f-application-workflow');

const fMyApplicants = require('../blocks/f-my-applicants');
const fApplication = require('../blocks/f-application');

blocks.pagePersonal = new Entity({ block: 'f-page-personal' });
blocks.pagePersonal.fMyApplicants = fMyApplicants.copy();

blocks.fSidePopupContent.fApplication = fApplication.copy();

blocks.bodyModal.fApplicationWorkflowRowTypeResolution =
    fApplicationWorkflow.fApplicationWorkflowRowTypeResolution.copy();
blocks.bodyModal.button2Submit = blocks.button2Submit.copy();

blocks.popup2Visible.menu = blocks.menu.copy();

blocks.sidePopup = new Entity({
    block: 'f-side-popup',
    elem: 'content',
});

module.exports = pageObject.create(blocks);
