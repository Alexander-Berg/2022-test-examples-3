const Entity = require('bem-page-object').Entity;

const blocks = require('../blocks/common');
const sForm = require('../blocks/s-form');

const fCandidateForm = new Entity({ block: 'f-candidate-form' });

fCandidateForm.submit = sForm.submit.copy();
fCandidateForm.submitDisabled = sForm.submitDisabled.copy();

fCandidateForm.fieldFirstName = new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'first-name' });
fCandidateForm.fieldFirstName.input = blocks.input.control.copy();
fCandidateForm.fieldLastName = new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'last-name' });
fCandidateForm.fieldLastName.input = blocks.input.control.copy();
fCandidateForm.fieldBirthday = new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'birthday' });
fCandidateForm.fieldGender = new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'gender' });
fCandidateForm.fieldCountry = new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'country' });
fCandidateForm.fieldCountry.input = blocks.input.control.copy();
fCandidateForm.fieldTargetCities = new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'target-cities' });
fCandidateForm.fieldTargetCities.input = blocks.input.control.copy();
fCandidateForm.fieldRecruiters = new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'recruiters' });
fCandidateForm.fieldRecruiters.input = blocks.input.control.copy();
fCandidateForm.fieldSource = new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'source' });
fCandidateForm.fieldMainContact = new Entity({ block: 'f-candidate-form', elem: 'row' }).mods({ type: 'email' }).descendant(new Entity({ block: 'f-candidate-form', elem: 'field' }).mods({ type: 'main-contact' }));
fCandidateForm.fieldMainContact.input = blocks.input.control.copy();

module.exports = fCandidateForm;
