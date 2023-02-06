const Entity = require('bem-page-object').Entity;

const sForm = new Entity({ block: 's-form' });
sForm.submit = new Entity({ block: 's-form', elem: 'button' }).mods({ type: 'submit' });
sForm.reset = new Entity({ block: 's-form', elem: 'button' }).mods({ type: 'reset' });
sForm.submitDisabled = sForm.submit.copy().mix(new Entity({ block: 'button2' }).mods({ disabled: 'yes' }));

module.exports = sForm;
