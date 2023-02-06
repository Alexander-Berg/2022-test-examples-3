const Entity = require('../Entity').ReactEntity;

const sForm = new Entity({ block: 'SForm' });

sForm.sField = new Entity({ block: 'SField' });

sForm.sFieldTypeSelect = new Entity({ block: 'SField' }).mods({ type: 'select' });
sForm.sFieldTypeSelect.Button = new Entity({ block: 'Select2', elem: 'Button' });

module.exports = sForm;
