const Entity = require('bem-page-object').Entity;

const sForm = require('../blocks/s-form');

const fMessageForm = new Entity({ block: 'f-message-form' });

fMessageForm.submit = sForm.submit.copy();
fMessageForm.submitDisabled = sForm.submitDisabled.copy();

fMessageForm.fieldSubject = new Entity({ block: 'f-message-form', elem: 'field' }).mods({ type: 'subject' });
fMessageForm.fieldText = new Entity({ block: 'f-message-form', elem: 'field' }).mods({ type: 'text' });

const fMessageFormFocused = fMessageForm.copy().mods({ focused: '' });

module.exports = { fMessageForm, fMessageFormFocused };
