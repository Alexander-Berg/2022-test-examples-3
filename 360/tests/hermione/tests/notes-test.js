const { consts, login } = require('../config/index');
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(login);
const PageObjects = require('../page-objects/notes');
const DESKTOP_LAYOUT_THRESHOLD = 590;
const MAX_COMPACT_WIDTH = 839;
const BALLOON_MODE_WIDTH = 320;

const yaWaitNoteListLoaded = async(bro) => {
    await bro.yaWaitForVisible(PageObjects.notes.notesList.notesListItem(), 'Заметка не появилась');
    await bro.pause(300); // надо дождаться подгрузки превью с заберуна
};

const yaWaitNoteLoaded = async(bro, hasAttachments, shouldUseMobileLayoutOnDesktop) => {
    await bro.yaWaitForVisible(PageObjects.notes.note.noteEditor());
    // дождёмся скрытия нотифайки "Заметка загружается"
    await bro.yaWaitForHidden(PageObjects.notes.note.stateIndicator());
    if (hasAttachments) {
        await bro.yaWaitForVisible(
            shouldUseMobileLayoutOnDesktop ?
                PageObjects.notes.note.attachmentsListCarousel() :
                PageObjects.notes.note.attachmentsList()
        );
    }
    await bro.yaWaitForHidden(PageObjects.notes.note.attachmentsList.attachment.spinContainer());
    await bro.pause(300); // надо дождаться подгрузки превью с заберуна
};

describe('Yandex.Notes -> ', () => {
    it('disknotes-133: disknotes-134: Просмотр промо-заметки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.loginFast(getUser('yndx-ufo-test-151'));
        await bro.url(consts.NOTES_URL);
        await yaWaitNoteListLoaded(bro);
        if (!isMobile) {
            // на десктопе отерывается первая заметка
            await yaWaitNoteLoaded(bro, true);
        }
        await bro.yaAssertView(isMobile ? 'disknotes-134' : 'disknotes-133', PageObjects.app(), {
            ignoreElements: PageObjects.psHeader.user.unreadTicker()
        });
    });

    hermione.only.in('chrome-desktop');
    it('disknotes-150: Отображение в маленьком окне браузера (десктоп)', async function() {
        const bro = this.browser;
        const { value: { height } } = await bro.windowHandleSize();

        await bro.windowHandleSize({ width: MAX_COMPACT_WIDTH - 10, height });
        await bro.loginFast(getUser('yndx-ufo-test-221'));
        await bro.url(consts.NOTES_URL);
        await yaWaitNoteListLoaded(bro);
        await yaWaitNoteLoaded(bro, true);
        await bro.yaAssertView('disknotes-150', PageObjects.app(), {
            ignoreElements: PageObjects.psHeader.user.unreadTicker()
        });
    });

    hermione.only.in('chrome-desktop');
    it('просмотр заметки в режиме мобильной верстки на десктопе', async function() {
        const bro = this.browser;
        const { value: { height } } = await bro.windowHandleSize();

        await bro.windowHandleSize({ width: DESKTOP_LAYOUT_THRESHOLD - 10, height });
        await bro.loginFast(getUser('yndx-ufo-test-221'));
        await bro.url(consts.NOTES_URL);
        await bro.yaWaitForHidden(PageObjects.notes.notesList.spin());
        await yaWaitNoteLoaded(bro, true, true);
        await bro.yaAssertView('desktop-note-in-mobile-layout', PageObjects.app(), {
            ignoreElements: PageObjects.psHeader.user.unreadTicker()
        });
        await bro.click(PageObjects.notes.note.toolbar.buttonBack());
        await yaWaitNoteListLoaded(bro);
        await bro.yaAssertView('desktop-notes-list-in-mobile-layout', PageObjects.app(), {
            ignoreElements: PageObjects.psHeader.user.unreadTicker()
        });
    });

    hermione.only.in('chrome-phone');
    it('просмотр заметки на тач-устройстве', async function() {
        const bro = this.browser;

        await bro.loginFast(getUser('yndx-ufo-test-221'));
        await bro.url(consts.NOTES_URL);
        await yaWaitNoteListLoaded(bro);
        await bro.yaAssertView('touch-notes-list', PageObjects.app());
        await bro.click(PageObjects.notes.notesList.notesListItem() + ':nth-child(2)');
        await yaWaitNoteLoaded(bro, true);
        await bro.yaAssertView('touch-note', PageObjects.app());
    });

    it('просмотр списка заметок в балун-режиме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        if (!isMobile) {
            const { value: { height } } = await bro.windowHandleSize();

            await bro.windowHandleSize({ width: BALLOON_MODE_WIDTH, height });
        }

        await bro.loginFast(getUser('yndx-ufo-test-221'));
        await bro.url(consts.NOTES_URL_BALLOON);
        await yaWaitNoteListLoaded(bro);
        await bro.yaAssertView('balloon-notes-list', PageObjects.app());
    });

    // бро-режим только на десктопе
    hermione.only.in('chrome-desktop');
    it('отображение заметок в бро-режиме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        if (!isMobile) {
            const { value: { height } } = await bro.windowHandleSize();

            // ябро открывает заметки в компактном режиме верстки
            await bro.windowHandleSize({ width: MAX_COMPACT_WIDTH - 10, height });
        }

        await bro.loginFast(getUser('yndx-ufo-test-221'));
        await bro.url(consts.NOTES_URL_BROMODE);
        await yaWaitNoteListLoaded(bro);
        if (!isMobile) {
            // на десктопе отерывается первая заметка
            await yaWaitNoteLoaded(bro, true);
        }
        await bro.yaAssertView('bro-notes', PageObjects.app(), {
            ignoreElements: [PageObjects.notes.notesList.notesListItem.secondRow()]
        });
    });

    it('отображение различных html-тегов в редакторе заметок', async function() {
        const bro = this.browser;

        await bro.loginFast(getUser('yndx-ufo-test-221'));
        await bro.url(consts.NOTES_URL);
        await yaWaitNoteListLoaded(bro);
        await bro.click(PageObjects.notes.notesList.notesListItem() + ':nth-child(3)');
        await yaWaitNoteLoaded(bro, false);
        await bro.yaAssertView('note-with-different-tags', PageObjects.app(), {
            ignoreElements: PageObjects.psHeader.user.unreadTicker()
        });
    });

    it('отображение форматированного текста в редакторе заметок', async function() {
        const bro = this.browser;

        await bro.loginFast(getUser('yndx-ufo-test-221'));
        await bro.url(consts.NOTES_URL);
        await yaWaitNoteListLoaded(bro);
        await bro.click(PageObjects.notes.notesList.notesListItem() + ':nth-child(4)');
        await yaWaitNoteLoaded(bro, false);
        await bro.yaAssertView('note-with-formatted-text', PageObjects.app(), {
            ignoreElements: PageObjects.psHeader.user.unreadTicker()
        });
    });

    it('открытие слайдера', async function() {
        const bro = this.browser;

        await bro.loginFast(getUser('yndx-ufo-test-221'));
        await bro.url(consts.NOTES_URL);
        await yaWaitNoteListLoaded(bro);
        await bro.click(PageObjects.notes.notesList.notesListItem() + ':nth-child(2)');
        await yaWaitNoteLoaded(bro, true);
        await bro.click(PageObjects.notes.note.attachmentsList.attachment() + ':first-child');
        await bro.pause(5000);
        await bro.yaAssertView('note-opened-slider', PageObjects.app());
    });

    it('disknotes-148, disknotes-239: Попытка создания заметки под переполненным юзером (500+)', async function() {
        const bro = this.browser;

        await bro.loginFast(getUser('yndx-ufo-test-560'));
        await bro.url(consts.NOTES_URL);
        await bro.yaWaitForVisible(PageObjects.notes.notesList.createNote());

        const isMobile = await bro.yaIsMobile();
        if (isMobile) {
            await bro.click(PageObjects.notes.notesList.createNote());
            await bro.yaWaitForVisible(PageObjects.notification());
            await bro.yaAssertView('disknotes-239', PageObjects.notification());
        } else {
            await bro.moveToObject(PageObjects.notes.notesList.createNote());
            await bro.yaWaitForVisible(PageObjects.tooltip());

            // hideElements почему-то не сработал
            await bro.execute((selector) => {
                document.querySelector(selector).style.visibility = 'hidden';
            }, PageObjects.notes.notesList.notesListItem.secondRow());

            await bro.pause(500);
            await bro.yaAssertView('disknotes-148', [
                PageObjects.notes.notesList.createNote(),
                PageObjects.tooltip()
            ]);
        }
    });
});
