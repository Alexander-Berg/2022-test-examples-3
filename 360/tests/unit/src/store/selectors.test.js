import React from 'react';
import { i18n } from 'react-tanker';
import { Spin } from '@ps-int/ufo-rocks/lib/components/lego-components/Spin';
import { STATES, CURRENT_NOTE_STATE, MAX_NOTE_CONTENT_LENGTH } from '../../../../src/consts';
import {
    getSortedNoteList,
    getCurrentNote,
    getCurrentNoteAttachments,
    isDialogOpened,
    isSaving,
    getNotLoadedNoteAttachments,
    getNotLoadedAttachments,
    isNoteLoading,
    getCurrentNoteState,
    getSliderResources,
    shouldUseMobileLayout,
    shouldUseMobileLayoutOnDesktop
} from '../../../../src/store/selectors';
import { DIALOG_STATES, DESKTOP_LAYOUT_THRESHOLD } from '../../../../src/consts';

const getStore = (current = 'note-5') => ({
    notes: {
        current,
        notes: {
            'note-1': {
                mtime: '2011-12-21T21:11:55.946Z',
                tags: {
                    deleted: true
                }
            },
            'note-2': {
                mtime: '2018-12-20T14:25:14.488Z',
                saving: true,
                content: {
                    state: STATES.LOADING
                },
                tags: {
                    pin: true
                }
            },
            'note-3': {
                mtime: '2019-12-21T21:11:55.946Z',
                saving: false,
                content: {
                    state: STATES.LOADED,
                    length: MAX_NOTE_CONTENT_LENGTH + 1
                },
                tags: {
                    trash: true
                }
            },
            'note-4': {
                mtime: '2018-11-01T00:00:00.228Z',
                saving: true,
                content: {
                    state: STATES.LOADED
                },
                tags: {}
            },
            'note-5': {
                tags: {
                    pin: true
                },
                mtime: '2018-12-21T21:11:55.946Z',
                attachmentOrder: ['attach-1', 'attach-2', 'attach-3', 'attach-4', 'attach-5'],
                attachments: {
                    'attach-1': {
                        id: 'attach-1',
                        preview: 'some-preview-url-1',
                        file: 'some-file-url-1',
                        state: STATES.LOADED
                    },
                    'attach-2': {
                        id: 'attach-2',
                        preview: 'some-preview-url-2',
                        file: 'some-file-url-2',
                        state: STATES.LOADED,
                        deleted: true
                    },
                    'attach-4': {
                        id: 'attach-4',
                        preview: null,
                        file: 'some-file-url-4',
                        state: STATES.LOADED
                    },
                    'attach-5': {
                        id: 'attach-5',
                        preview: 'some-preview-url-5',
                        file: 'some-file-url-5',
                        state: STATES.LOADED
                    }
                }
            },
            'note-6': {
                mtime: '2018-12-22T21:11:55.946Z',
                tags: {}
            },
            'note-7': {
                mtime: '2010-01-01T00:00:00.228Z',
                tags: {},
                attachmentOrder: ['attach-1'],
                attachments: {
                    'attach-1': {
                        id: 'attach-1',
                        state: STATES.INITIAL
                    }
                }
            },
            'note-8': {
                tags: {
                    pin: true
                },
                mtime: '2018-09-01T21:11:55.946Z',
                attachmentOrder: ['attach-1', 'attach-2', 'attach-3'],
                attachments: {
                    'attach-1': {
                        id: 'attach-1',
                        preview: 'some-preview-url-1',
                        file: 'some-file-url-1',
                        state: STATES.LOADED
                    },
                    'attach-2': {
                        id: 'attach-2',
                        state: STATES.INITIAL
                    },
                    'attach-3': {
                        id: 'attach-3',
                        state: STATES.INITIAL
                    }
                }
            }
        }
    }
});

describe('store/selectors =>', () => {
    describe('getSortedNoteList =>', () => {
        const store = getStore();
        const sortedNotes = getSortedNoteList(store);
        it('должен фильтровать удалённые заметки', () => {
            expect(sortedNotes.length).toEqual(6);
            expect(sortedNotes.includes(store.notes.notes['note-1'])).toBe(false);
            expect(sortedNotes.includes(store.notes.notes['note-3'])).toBe(false);
        });
        it('запиненные заметки должны идти раньше не-запиненных', () => {
            ['note-2', 'note-5', 'note-8'].forEach((pinnedNoteId) => {
                ['note-4', 'note-6'].forEach((notPinnedNoteId) => {
                    expect(sortedNotes.indexOf(store.notes.notes[pinnedNoteId]))
                        .toBeLessThan(sortedNotes.indexOf(store.notes.notes[notPinnedNoteId]));
                });
            });
        });
        it('заметки должны сортироваться по дате модификации (позже созданные - раньше)', () => {
            expect(sortedNotes.indexOf(store.notes.notes['note-6']))
                .toBeLessThan(sortedNotes.indexOf(store.notes.notes['note-4']));
            expect(sortedNotes.indexOf(store.notes.notes['note-6']))
                .toBeLessThan(sortedNotes.indexOf(store.notes.notes['note-7']));
            expect(sortedNotes.indexOf(store.notes.notes['note-4']))
                .toBeLessThan(sortedNotes.indexOf(store.notes.notes['note-7']));
        });
        it('запиненные заметки между собой тоже должны сортироваться по дате модификации', () => {
            expect(sortedNotes.indexOf(store.notes.notes['note-5']))
                .toBeLessThan(sortedNotes.indexOf(store.notes.notes['note-2']));
            expect(sortedNotes.indexOf(store.notes.notes['note-5']))
                .toBeLessThan(sortedNotes.indexOf(store.notes.notes['note-8']));
            expect(sortedNotes.indexOf(store.notes.notes['note-2']))
                .toBeLessThan(sortedNotes.indexOf(store.notes.notes['note-8']));
        });
    });

    describe('getCurrentNote =>', () => {
        it('должен вернуть текущую заметку', () => {
            const store = getStore();
            expect(getCurrentNote(store)).toEqual(store.notes.notes[store.notes.current]);
        });
        it('должен вернуть null если ни одна заметка не выбрана', () => {
            expect(getCurrentNote(getStore(null))).toEqual(null);
        });
    });

    describe('getCurrentNoteAttachments', () => {
        it('должен вернуть пустой массив если ни одна заметка не выбрана', () => {
            expect(getCurrentNoteAttachments(getStore(null))).toEqual([]);
        });
        it('должен вернуть аттачи, отфильтровав удалённые и несуществующие', () => {
            expect(getCurrentNoteAttachments(getStore())).toEqual([
                {
                    id: 'attach-1',
                    preview: 'some-preview-url-1',
                    file: 'some-file-url-1',
                    state: STATES.LOADED
                }, {
                    id: 'attach-4',
                    preview: null,
                    file: 'some-file-url-4',
                    state: STATES.LOADED
                }, {
                    id: 'attach-5',
                    preview: 'some-preview-url-5',
                    file: 'some-file-url-5',
                    state: STATES.LOADED
                }
            ]);
        });
        it('для слайдера должен вернуть аттачи, дополнительно отфильтровав аттачи без превью', () => {
            expect(getCurrentNoteAttachments(getStore(), true)).toEqual([
                {
                    id: 'attach-1',
                    preview: 'some-preview-url-1',
                    file: 'some-file-url-1',
                    state: STATES.LOADED
                }, {
                    id: 'attach-5',
                    preview: 'some-preview-url-5',
                    file: 'some-file-url-5',
                    state: STATES.LOADED
                }
            ]);
        });
    });

    describe('isDialogOpened', () => {
        let testStore;

        beforeEach(() => {
            testStore = {
                dialogs: {
                    a: { state: DIALOG_STATES.CLOSED }
                }
            };
        });

        it('Если не был открыт ни один диалог, то возврашает false', () => {
            testStore.dialogs = {};
            expect(isDialogOpened(testStore, 'a')).toBe(false);
        });

        it('Возвращает false, если указанный диалог закрыт', () => {
            expect(isDialogOpened(testStore, 'a')).toBe(false);
        });

        it('Возвращает true, если указанный диалог открыт', () => {
            testStore.dialogs.a.state = DIALOG_STATES.OPENED;
            expect(isDialogOpened(testStore, 'a')).toBe(true);
        });
    });

    describe('isSaving', () => {
        it('should return false if note is null', () => {
            expect(isSaving(getStore(null))).toBe(false);
        });

        it('should return true if a note is loaded and has saving property set to true', () => {
            expect(isSaving(getStore('note-4'))).toBe(true);
        });

        it('should return false if a note is loaded and has saving property set to false', () => {
            expect(isSaving(getStore('note-3'))).toBe(false);
        });

        it('should return false if a note\'s state is not LOADED', () => {
            expect(isSaving(getStore('note-2'))).toBe(false);
        });
    });

    describe('getNotLoadedNoteAttachments', () => {
        it('should return an empty array if noteId is not provided', () => {
            expect(getNotLoadedNoteAttachments(getStore())).toEqual([]);
        });

        it('should return an empty array if a note does not have attachments property', () => {
            expect(getNotLoadedNoteAttachments(getStore(), 'note-1')).toEqual([]);
        });

        it('should return a list of attachment ids of the attachments not yet loaded', () => {
            const ids = getNotLoadedNoteAttachments(getStore(), 'note-8');
            expect(ids.length).toBe(2);
            expect(ids.includes('attach-1')).toBe(false);
        });
    });

    describe('getNotLoadedAttachments', () => {
        it('should return the list of all not loaded first attachments', () => {
            expect(getNotLoadedAttachments(getStore())).toEqual([['note-7', 'attach-1']]);
        });
        it('should return the list of all not loaded first attachments + all not loaded attachments of the current note', () => {
            expect(getNotLoadedAttachments(getStore('note-8'))).toEqual([
                ['note-7', 'attach-1'],
                ['note-8', 'attach-2'],
                ['note-8', 'attach-3']
            ]);
        });
    });

    describe('isNoteLoading', () => {
        it('should return true if the note\'s state is `LOADING`', () => {
            const note = {
                mtime: '2018-12-20T19:25:14.488Z',
                content: {
                    state: STATES.LOADING
                }
            };

            expect(isNoteLoading(note)).toBe(true);
        });

        it('should return true if the note\'s state is `INITIAL`', () => {
            const note = {
                mtime: '2018-12-20T19:25:14.488Z',
                content: {
                    state: STATES.INITIAL
                }
            };

            expect(isNoteLoading(note)).toBe(true);
        });

        it('should return false otherwise', () => {
            const note = {
                mtime: '2018-12-20T19:25:14.488Z',
                content: {
                    state: STATES.LOADED
                }
            };

            expect(isNoteLoading(note)).toBe(false);
        });
    });

    describe('getCurrentNoteState', () => {
        it('should return `LOADING`', () => {
            expect(getCurrentNoteState(getStore('note-2'))).toBe(CURRENT_NOTE_STATE.LOADING);
        });

        it('should return `MAX_NOTE_CONTENT_LENGTH`', () => {
            expect(getCurrentNoteState(getStore('note-3'))).toBe(CURRENT_NOTE_STATE.SIZE_LIMIT_EXCEEDED);
        });

        it('should return `SAVING`', () => {
            expect(getCurrentNoteState(getStore('note-4'))).toBe(CURRENT_NOTE_STATE.SAVING);
        });

        it('should return `DEFAULT`', () => {
            expect(getCurrentNoteState(getStore('note-1'))).toBe(CURRENT_NOTE_STATE.DEFAULT);
        });
    });

    describe('getSliderResources', () => {
        const standardAttachment = {
            resourceId: 'attach-1',
            preview: 'some-preview-url-1',
            file: 'some-file-url-1',
            state: STATES.LOADED
        };
        const attachmentWithError = {
            resourceId: 'attach-2',
            preview: 'some-preview-url-2',
            file: 'some-file-url-2',
            error: true,
            state: STATES.LOADED
        };
        const attachmentWithoutFile = {
            resourceId: 'attach-3',
            preview: 'some-preview-url-3',
            state: STATES.LOADED
        };
        const attachments = [standardAttachment, attachmentWithError, attachmentWithoutFile];

        it('should map an attachment to a resource for a slider', () => {
            expect(getSliderResources([standardAttachment])).toEqual([{
                previewBaseUrl: standardAttachment.file,
                originalUrl: standardAttachment.file,
                resourceId: standardAttachment.resourceId,
                state: standardAttachment.state
            }]);
        });

        it('should map an attachment with error to a resource with an error message', () => {
            expect(getSliderResources([attachmentWithError])).toEqual([{
                resourceId: attachmentWithError.resourceId,
                content: i18n(LANG, 'entities', 'ufo_error')
            }]);
        });

        it('should map an attachment without file url to a resource with a spinner', () => {
            expect(getSliderResources([attachmentWithoutFile])).toEqual([{
                resourceId: attachmentWithoutFile.resourceId,
                content: <Spin view="default" size="m" progress />
            }]);
        });

        it('should map all received attachments to slider resources', () => {
            expect(getSliderResources(attachments).length).toBe(attachments.length);
        });
    });

    describe('shouldUseMobileLayout', () => {
        const getStoreWithWindowWidth = (windowWidth = DESKTOP_LAYOUT_THRESHOLD) => ({
            environment: { windowWidth }
        });

        it('should return always true on touch devices', () => {
            global.IS_TOUCH = true;
            expect(shouldUseMobileLayout(getStoreWithWindowWidth(DESKTOP_LAYOUT_THRESHOLD + 100))).toBe(true);
            global.IS_TOUCH = false;
        });

        it('should return true if window width < desktop layout threshold', () => {
            expect(shouldUseMobileLayout(getStoreWithWindowWidth(DESKTOP_LAYOUT_THRESHOLD - 1))).toBe(true);
        });

        it('should return false if window width >= desktop layout threshold', () => {
            expect(shouldUseMobileLayout(getStoreWithWindowWidth())).toBe(false);
        });
    });

    describe('shouldUseMobileLayoutOnDesktop', () => {
        it('should return always false on touch devices', () => {
            global.IS_TOUCH = true;
            expect(shouldUseMobileLayoutOnDesktop({
                environment: { windowWidth: DESKTOP_LAYOUT_THRESHOLD - 100 }
            })).toBe(false);
            global.IS_TOUCH = false;
        });
    });
});
