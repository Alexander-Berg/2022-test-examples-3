const { consts, login } = require('../config/index');
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(login);
const PageObjects = require('../page-objects/notes');
const { notesTouchBrowsersList } = require('@ps-int/ufo-hermione/browsers');

hermione.only.in(notesTouchBrowsersList);
describe('Тач-редактор заметок -> ', () => {
    it('disknotes-227: создание и удаление заметки', async function() {
        const bro = this.browser;

        await bro.loginFast(getUser('yndx-ufo-test-233'));
        await bro.url(consts.NOTES_URL);
        // создаем заметку
        await bro.yaWaitForVisible(PageObjects.notes.notesList.createNoteTouch(), 'Кнопка создания заметки не появилась');
        await bro.click(PageObjects.notes.notesList.createNoteTouch());
        // вводим текст
        await bro.yaWaitForVisible(PageObjects.notes.note.title(), 'Поле ввода заголовка не отобразилось');
        await bro.click(PageObjects.notes.note.title());
        await bro.keys(['TEST TOUCH EDITOR']);
        await bro.click(PageObjects.notes.note.noteEditor.touchEditor());
        await bro.keys(['Тестирование создания заметки в тач-редакторе', 'Enter', 'Текст для теста']);
        await bro.yaWaitForHidden(PageObjects.notes.note.stateIndicator());
        await bro.yaAssertView('disknotes-227', PageObjects.app());
        // удаляем заметку
        await bro.click(PageObjects.notes.note.toolbar.delete());
        await bro.yaWaitForVisible(PageObjects.noteDeleteConfirmationDialog(), 'Диалог подтверждения удаления не появился');
        await bro.click(PageObjects.dialog.confirm());
    });

    it('disknotes-229: удаление заметки в тач-редакторе', async function() {
        const bro = this.browser;

        await bro.loginFast(getUser('yndx-ufo-test-233'));
        await bro.url(consts.NOTES_URL);
        // создаем заметку
        await bro.yaWaitForVisible(PageObjects.notes.notesList.createNoteTouch(), 'Кнопка создания заметки не появилась');
        await bro.click(PageObjects.notes.notesList.createNoteTouch());
        // удаляем заметку
        await bro.yaWaitForVisible(PageObjects.notes.note.toolbar.delete(), 'Кнопка удаления не появилась');
        await bro.click(PageObjects.notes.note.toolbar.delete());
        await bro.pause(5000); // дождемся, пока исчезнет нотификация сохранения
        await bro.yaWaitForVisible(PageObjects.noteDeleteConfirmationDialog(), 'Диалог подтверждения удаления не появился');
        await bro.yaAssertView('disknotes-229', PageObjects.app());
        await bro.click(PageObjects.dialog.confirm());
        // проверяем, что произошел возврат к списку заметок
        await bro.yaWaitForVisible(PageObjects.notes.notesList(), 'Возврат к списку заметок не произошел');
        await bro.yaWaitForHidden(PageObjects.notes.note(), 'Заметка не была удалена');
    });

    it('disknotes-228: закрепление заметки', async function() {
        const bro = this.browser;

        await bro.loginFast(getUser('yndx-ufo-test-234'));
        await bro.url(consts.NOTES_URL);
        // создаем заметку
        await bro.yaWaitForVisible(PageObjects.notes.notesList.createNoteTouch(), 'Кнопка создания заметки не появилась');
        await bro.click(PageObjects.notes.notesList.createNoteTouch());
        // закрепим заметку
        await bro.yaWaitForVisible(PageObjects.notes.note.toolbar.pin(), 'Кнопка закрепления не появилась');
        await bro.click(PageObjects.notes.note.toolbar.pin());
        await bro.pause(2000); // дождемся окончания загрузки заметки
        await bro.yaAssertView('note-pin-in-touch-editor', PageObjects.app());
        // открепим заметку
        await bro.click(PageObjects.notes.note.toolbar.pin());
        await bro.pause(1000);
        await bro.yaAssertView('note-unpin-in-touch-editor', PageObjects.app());
        // удалим заметку
        await bro.yaWaitForVisible(PageObjects.notes.note.toolbar.delete(), 'Кнопка удаления не появилась');
        await bro.click(PageObjects.notes.note.toolbar.delete());
        await bro.yaWaitForVisible(PageObjects.noteDeleteConfirmationDialog(), 'Диалог подтверждения удаления не появился');
        await bro.click(PageObjects.dialog.confirm());
    });
});
