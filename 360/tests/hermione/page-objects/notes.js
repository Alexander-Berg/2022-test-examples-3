const bemPageObject = require('bem-page-object');

const Entity = bemPageObject.Entity;
const PageObjects = {};

PageObjects.app = new Entity('.app');
PageObjects.notes = new Entity('.notes');
PageObjects.notes.notesList = new Entity('.notes-list');
PageObjects.notes.notesList.spin = new Entity('.spin2');
PageObjects.notes.notesList.notesListItem = new Entity('.notes-list-item');
PageObjects.notes.notesList.notesListItem.secondRow = new Entity('.notes-list-item__second-row');
PageObjects.notes.notesList.createNote = new Entity('.Button2.create-note-button');
PageObjects.notes.notesList.createNoteTouch = new Entity('.Button2.create-note-button_touch');
PageObjects.notes.note = new Entity('.notes__note');
PageObjects.notes.note.toolbar = new Entity('.note-toolbar');
PageObjects.notes.note.toolbar.buttonBack = new Entity('.note-toolbar__button_back');
PageObjects.notes.note.toolbar.pin = new Entity('.note-toolbar__button_pin');
PageObjects.notes.note.toolbar.delete = new Entity('.note-toolbar__button_delete');
PageObjects.notes.note.attachmentsList = new Entity('.attachments-list');
PageObjects.notes.note.attachmentsListCarousel = new Entity('.attachments-list__carousel');
PageObjects.notes.note.attachmentsList.attachment = new Entity('.attachment');
PageObjects.notes.note.attachmentsList.attachment.spinContainer = new Entity('.attachment__spinner-container');
PageObjects.notes.note.noteEditor = new Entity('.note-editor');
PageObjects.notes.note.noteEditor.touchEditor = new Entity('.touch-editor__content');
PageObjects.notes.note.title = new Entity('.Textinput-Control');
PageObjects.notes.note.stateIndicator = new Entity('.note-state-indicator');
PageObjects.dialog = new Entity('.dialog');
PageObjects.dialog.confirm = new Entity('.confirmation-dialog__button_submit');
PageObjects.noteDeleteConfirmationDialog = new Entity('.dialog.note-delete-confirmation-dialog');
PageObjects.tooltip = new Entity('.Tooltip');
PageObjects.notification = new Entity('.notification__popup');

PageObjects.psHeader = new Entity('.PSHeader');
PageObjects.psHeader.user = new Entity('.PSHeader-User');
PageObjects.psHeader.user.unreadTicker = new Entity('.user-account__ticker');

module.exports = bemPageObject.create(PageObjects);
