const Entity = require('bem-page-object').Entity;
const blocks = require('../blocks/common');
const fInfo = require('../blocks/f-info');
const fRemovable = require('../blocks/f-removable');

const fCandidatesFilter = new Entity({ block: 'f-candidates-filter' });
fCandidatesFilter.field = new Entity({ block: 'f-candidates-filter', elem: 'field' });
fCandidatesFilter.row = new Entity({ block: 'f-candidates-filter', elem: 'row' });

fCandidatesFilter.fieldTypeCity = fCandidatesFilter.field.copy().mods({ type: 'city' });
fCandidatesFilter.fieldTypeCreatedGte = fCandidatesFilter.field.copy().mods({ type: 'created-gte' });
fCandidatesFilter.fieldTypeCreatedLte = fCandidatesFilter.field.copy().mods({ type: 'created-lte' });

fCandidatesFilter.fieldTypeEmployer = fCandidatesFilter.field.copy().mods({ type: 'employer' });
fCandidatesFilter.fieldTypeIgnoreEmployees = fCandidatesFilter.field.copy().mods({ type: 'ignore-employees' });
fCandidatesFilter.fieldTypeIgnoreEmployees.checkbox = blocks.checkbox.copy();
fCandidatesFilter.fieldTypeInstitution = fCandidatesFilter.field.copy().mods({ type: 'institution' });
fCandidatesFilter.fieldTypeIsActive = fCandidatesFilter.field.copy().mods({ type: 'is-active' });
fCandidatesFilter.fieldTypeIsActive.radioButtonNo = blocks.radioButton.radioSideRight.copy();

fCandidatesFilter.fieldTypeModifiedGte = fCandidatesFilter.field.copy().mods({ type: 'modified-gte' });
fCandidatesFilter.fieldTypeModifiedLte = fCandidatesFilter.field.copy().mods({ type: 'modified-lte' });
fCandidatesFilter.fieldTypeOnSiteInterviewsAvgGrade = fCandidatesFilter.field.copy().mods({ type: 'on-site-interviews-avg-grade' });
fCandidatesFilter.fieldTypeProfessions = fCandidatesFilter.field.copy().mods({ type: 'professions' });
fCandidatesFilter.fieldTypeProfessions.input = blocks.input.control.copy();
fCandidatesFilter.fieldTypeProfessions.deletePlateButton = fRemovable.delete.copy();

fCandidatesFilter.fieldTypeResponsibles = fCandidatesFilter.field.copy().mods({ type: 'responsibles' });
fCandidatesFilter.fieldTypeResponsibles.input = blocks.input.control.copy();
fCandidatesFilter.fieldTypeSkills = fCandidatesFilter.field.copy().mods({ type: 'skills' });
fCandidatesFilter.fieldTypeSkills.input = blocks.input.control.copy();
fCandidatesFilter.fieldTypeSkills.deletePlateButton = fRemovable.delete.copy();

fCandidatesFilter.fieldTypeSkypeInterviewsAvgGrade = fCandidatesFilter.field.copy().mods({ type: 'skype-interviews-avg-grade' });
fCandidatesFilter.fieldTypeSort = fCandidatesFilter.field.copy().mods({ type: 'sort' });
fCandidatesFilter.fieldTypeTags = fCandidatesFilter.field.copy().mods({ type: 'tags' });
fCandidatesFilter.fieldTypeTags.input = blocks.input.control.copy();
fCandidatesFilter.fieldTypeTargetCities = fCandidatesFilter.field.copy().mods({ type: 'target-cities' });
fCandidatesFilter.fieldTypeTargetCities.input = blocks.input.control.copy();
fCandidatesFilter.fieldTypeWithoutNohire = fCandidatesFilter.field.copy().mods({ type: 'without-nohire' });
fCandidatesFilter.fieldTypeWithoutNohire.checkbox = blocks.checkbox.copy();

fCandidatesFilter.rowTypeProfessions = fCandidatesFilter.row.copy().mods({ type: 'professions' });
fCandidatesFilter.rowTypeProfessions.info = fInfo.copy();
fCandidatesFilter.rowTypeSkypeInterviewsAvgGrade = fCandidatesFilter.row.copy().mods({ type: 'skype-interviews-avg-grade' });
fCandidatesFilter.rowTypeSkypeInterviewsAvgGrade.info = fInfo.copy();

module.exports = fCandidatesFilter;
