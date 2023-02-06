import notesReducer, { DEFAULT_STATE } from '../../../../../src/store/reducers/notes';
import {
    UPDATE_NOTES,
    UPDATE_NOTE,
    UPDATE_CURRENT_NOTE,
    ADD_NOTE,
    DELETE_CURRENT_NOTE,
    ADD_ATTACHMENTS,
    UPDATE_ATTACHMENT,
    REPLACE_ATTACHMENT_ID,
    UPDATE_NOTES_SLIDER,
    SET_BLOCK_NOTE_SELECTION
} from '../../../../../src/store/types';
import { STATES } from '../../../../../src/consts';

const mockedNotesState = {
    'first-note-id': {
        id: 'first-note-id',
        title: 'My first note',
        snippet: 'Note 1 content',
        tags: {},
        content: {
            state: STATES.INITIAL,
            data: null
        },
        attachments: {},
        attachmentOrder: []
    },
    'second-note-id': {
        id: 'second-note-id',
        title: 'My second note',
        snippet: 'Note 2 content',
        tags: {},
        content: {
            state: STATES.INITIAL,
            data: null
        },
        attachments: {},
        attachmentOrder: []
    },
    'third-note-id': {
        id: 'third-note-id',
        title: 'My third note',
        snippet: 'Note 3 content',
        tags: {
            pin: true
        },
        content: {
            state: STATES.INITIAL,
            data: null
        },
        attachments: {
            'first-attach': {
                resourceId: 'first-attach',
                preview: 'first-preview-url',
                file: 'first-file-url'
            },
            'second-attach': {
                resourceId: 'second-attach',
                preview: 'second-preview-url',
                file: 'second-file-url'
            }
        },
        attachmentOrder: ['first-attach', 'second-attach']
    }
};

const getDefaultState = (current) => Object.assign({}, DEFAULT_STATE, {
    current: current || null,
    notes: mockedNotesState
});

describe('store/reducers/dialogs', () => {
    it('should return DEFAULT_STATE by default', () => {
        expect(notesReducer(undefined, { type: 'any' })).toEqual(DEFAULT_STATE);
    });
    it('UPDATE_CURRENT_NOTE', () => {
        const defaultState = getDefaultState();
        const newState = notesReducer(defaultState, {
            type: UPDATE_CURRENT_NOTE,
            payload: 'first-note-id'
        });
        expect(newState).toEqual(Object.assign({}, defaultState, { current: 'first-note-id' }));
        expect(newState).not.toBe(defaultState);
        expect(newState.notes).toBe(defaultState.notes);
    });
    describe('UPDATE_NOTE =>', () => {
        it('simple', () => {
            const defaultState = getDefaultState();
            const newState = notesReducer(defaultState, {
                type: UPDATE_NOTE,
                payload: {
                    note: {
                        id: 'second-note-id',
                        snippet: 'changed-snippet'
                    }
                }
            });
            expect(newState).toEqual(Object.assign({}, defaultState, {
                notes: Object.assign({}, defaultState.notes, {
                    'second-note-id': Object.assign(
                        {},
                        defaultState.notes['second-note-id'],
                        { snippet: 'changed-snippet' }
                    )
                })
            }));
            expect(newState.notes).not.toBe(defaultState.notes);
            expect(newState.notes['first-note-id']).toBe(defaultState.notes['first-note-id']);
            expect(newState.notes['second-note-id']).not.toBe(defaultState.notes['second-note-id']);
            expect(newState.notes['second-note-id'].tags).toBe(defaultState.notes['second-note-id'].tags);
            expect(newState.notes['second-note-id'].content).toBe(defaultState.notes['second-note-id'].content);
            expect(newState.notes['second-note-id'].attachments).toBe(defaultState.notes['second-note-id'].attachments);
            expect(newState.notes['second-note-id'].attachmentOrder).toBe(defaultState.notes['second-note-id'].attachmentOrder);
            expect(newState.notes['third-note-id']).toBe(defaultState.notes['third-note-id']);
        });
        it('change tags', () => {
            const defaultState = getDefaultState();
            const newState = notesReducer(defaultState, {
                type: UPDATE_NOTE,
                payload: {
                    note: {
                        id: 'first-note-id',
                        tags: {
                            pin: true
                        }
                    }
                }
            });
            expect(newState).toEqual(Object.assign({}, defaultState, {
                notes: Object.assign({}, defaultState.notes, {
                    'first-note-id': Object.assign(
                        {},
                        defaultState.notes['first-note-id'],
                        { tags: { pin: true } }
                    )
                })
            }));
            expect(newState.notes).not.toBe(defaultState.notes);
            expect(newState.notes['first-note-id']).not.toBe(defaultState.notes['first-note-id']);
            expect(newState.notes['first-note-id'].tags).not.toBe(defaultState.notes['first-note-id'].tags);
            expect(newState.notes['first-note-id'].content).toBe(defaultState.notes['first-note-id'].content);
            expect(newState.notes['first-note-id'].attachments).toBe(defaultState.notes['first-note-id'].attachments);
            expect(newState.notes['first-note-id'].attachmentOrder).toBe(defaultState.notes['first-note-id'].attachmentOrder);
            expect(newState.notes['second-note-id']).toBe(defaultState.notes['second-note-id']);
            expect(newState.notes['third-note-id']).toBe(defaultState.notes['third-note-id']);
        });
        it('change content', () => {
            const defaultState = getDefaultState();
            const newState = notesReducer(defaultState, {
                type: UPDATE_NOTE,
                payload: {
                    note: {
                        id: 'third-note-id',
                        content: {
                            state: STATES.LOADED,
                            data: null
                        }
                    }
                }
            });
            expect(newState).toEqual(Object.assign({}, defaultState, {
                notes: Object.assign({}, defaultState.notes, {
                    'third-note-id': Object.assign(
                        {},
                        defaultState.notes['third-note-id'],
                        {
                            content: {
                                state: STATES.LOADED,
                                data: null
                            }
                        }
                    )
                })
            }));
            expect(newState.notes).not.toBe(defaultState.notes);
            expect(newState.notes['first-note-id']).toBe(defaultState.notes['first-note-id']);
            expect(newState.notes['second-note-id']).toBe(defaultState.notes['second-note-id']);
            expect(newState.notes['third-note-id']).not.toBe(defaultState.notes['third-note-id']);
            expect(newState.notes['third-note-id'].tags).toBe(defaultState.notes['third-note-id'].tags);
            expect(newState.notes['third-note-id'].content).not.toBe(defaultState.notes['third-note-id'].content);
            expect(newState.notes['third-note-id'].attachments).toBe(defaultState.notes['third-note-id'].attachments);
            expect(newState.notes['third-note-id'].attachmentOrder).toBe(defaultState.notes['third-note-id'].attachmentOrder);
        });
    });
    it('UPDATE_NOTES', () => {
        const defaultState = getDefaultState();
        const newNotes = {
            'new-note-id': {
                id: 'new-note-id',
                snippet: 'some-snippet',
                title: 'some-title'
            }
        };
        const newState = notesReducer(defaultState, {
            type: UPDATE_NOTES,
            payload: {
                state: STATES.LOADED,
                notes: newNotes
            }
        });
        expect(newState).toEqual(Object.assign({}, defaultState, {
            state: STATES.LOADED,
            notes: newNotes
        }));
        expect(newState.notes).not.toBe(defaultState.notes);
        expect(newState.notes).toBe(newNotes);
    });
    it('ADD_NOTE', () => {
        const defaultState = getDefaultState();
        const newNote = {
            id: 'new-note-id',
            title: 'My first note',
            snippet: 'Note content',
            content: {
                state: STATES.INITIAL,
                data: null
            }
        };
        const newState = notesReducer(defaultState, {
            type: ADD_NOTE,
            payload: {
                note: newNote
            }
        });
        expect(newState).toEqual(Object.assign({}, defaultState, {
            notes: Object.assign({}, defaultState.notes, {
                [newNote.id]: newNote
            })
        }));
        expect(newState.notes).not.toBe(defaultState.notes);
        expect(newState.notes['first-note-id']).toBe(defaultState.notes['first-note-id']);
        expect(newState.notes['second-note-id']).toBe(defaultState.notes['second-note-id']);
        expect(newState.notes['third-note-id']).toBe(defaultState.notes['third-note-id']);
    });
    it('DELETE_CURRENT_NOTE', () => {
        const defaultState = getDefaultState('first-note-id');
        const newState = notesReducer(defaultState, { type: DELETE_CURRENT_NOTE });
        expect(newState).toEqual(Object.assign({}, defaultState, {
            current: null,
            notes: {
                'first-note-id': Object.assign({}, defaultState.notes['first-note-id'], { tags: { deleted: true } }),
                'second-note-id': defaultState.notes['second-note-id'],
                'third-note-id': defaultState.notes['third-note-id']
            }
        }));
        expect(newState.notes).not.toBe(defaultState.notes);
        expect(newState.notes['first-note-id']).not.toBe(defaultState.notes['first-note-id']);
        expect(newState.notes['first-note-id'].tags).not.toBe(defaultState.notes['first-note-id'].tags);
        expect(newState.notes['first-note-id'].content).toBe(defaultState.notes['first-note-id'].content);
        expect(newState.notes['first-note-id'].attachments).toBe(defaultState.notes['first-note-id'].attachments);
        expect(newState.notes['first-note-id'].attachmentOrder).toBe(defaultState.notes['first-note-id'].attachmentOrder);
        expect(newState.notes['second-note-id']).toBe(defaultState.notes['second-note-id']);
        expect(newState.notes['third-note-id']).toBe(defaultState.notes['third-note-id']);
    });
    it('ADD_ATTACHMENTS', () => {
        const defaultState = getDefaultState();
        const newAttachments = [{
            resourceId: 'third-attach',
            preview: 'third-preview-url',
            file: 'third-file-url'
        }, {
            resourceId: 'fourth-attach',
            preview: 'fourth-preview-url',
            file: 'fourth-file-url'
        }];
        const newState = notesReducer(defaultState, {
            type: ADD_ATTACHMENTS,
            payload: {
                noteId: 'third-note-id',
                attachments: newAttachments
            }
        });
        expect(newState).toEqual(Object.assign({}, defaultState, {
            notes: {
                'first-note-id': defaultState.notes['first-note-id'],
                'second-note-id': defaultState.notes['second-note-id'],
                'third-note-id': Object.assign(
                    {},
                    defaultState.notes['third-note-id'],
                    {
                        attachments: {
                            'first-attach': mockedNotesState['third-note-id'].attachments['first-attach'],
                            'second-attach': mockedNotesState['third-note-id'].attachments['second-attach'],
                            'third-attach': newAttachments[0],
                            'fourth-attach': newAttachments[1]
                        },
                        attachmentOrder: ['first-attach', 'second-attach', 'third-attach', 'fourth-attach']
                    }
                )
            }
        }));
        expect(newState.notes).not.toBe(defaultState.notes);
        expect(newState.notes['first-note-id']).toBe(defaultState.notes['first-note-id']);
        expect(newState.notes['second-note-id']).toBe(defaultState.notes['second-note-id']);
        expect(newState.notes['third-note-id']).not.toBe(defaultState.notes['third-note-id']);
        expect(newState.notes['third-note-id'].attachments).not.toBe(defaultState.notes['third-note-id'].attachments);
        expect(newState.notes['third-note-id'].attachmentOrder).not.toBe(defaultState.notes['third-note-id'].attachmentOrder);
        expect(newState.notes['third-note-id'].tags).toBe(defaultState.notes['third-note-id'].tags);
        expect(newState.notes['third-note-id'].content).toBe(defaultState.notes['third-note-id'].content);
    });
    it('UPDATE_ATTACHMENT', () => {
        const defaultState = getDefaultState();
        const newState = notesReducer(defaultState, {
            type: UPDATE_ATTACHMENT,
            payload: {
                noteId: 'third-note-id',
                resourceId: 'second-attach',
                data: {
                    state: STATES.LOADING,
                    preview: null
                }
            }
        });
        expect(newState).toEqual(Object.assign({}, defaultState, {
            notes: {
                'first-note-id': defaultState.notes['first-note-id'],
                'second-note-id': defaultState.notes['second-note-id'],
                'third-note-id': Object.assign(
                    {},
                    defaultState.notes['third-note-id'],
                    {
                        attachments: {
                            'first-attach': mockedNotesState['third-note-id'].attachments['first-attach'],
                            'second-attach': {
                                resourceId: 'second-attach',
                                state: STATES.LOADING,
                                preview: null,
                                file: 'second-file-url'
                            }
                        },
                        attachmentOrder: ['first-attach', 'second-attach']
                    }
                )
            }
        }));
        expect(newState.notes).not.toBe(defaultState.notes);
        expect(newState.notes['first-note-id']).toBe(defaultState.notes['first-note-id']);
        expect(newState.notes['second-note-id']).toBe(defaultState.notes['second-note-id']);
        expect(newState.notes['third-note-id']).not.toBe(defaultState.notes['third-note-id']);
        expect(newState.notes['third-note-id'].attachments).not.toBe(defaultState.notes['third-note-id'].attachments);
        expect(newState.notes['third-note-id'].attachmentOrder).toBe(defaultState.notes['third-note-id'].attachmentOrder);
        expect(newState.notes['third-note-id'].tags).toBe(defaultState.notes['third-note-id'].tags);
        expect(newState.notes['third-note-id'].content).toBe(defaultState.notes['third-note-id'].content);
    });
    it('REPLACE_ATTACHMENT_ID', () => {
        const defaultState = getDefaultState();
        const newState = notesReducer(defaultState, {
            type: REPLACE_ATTACHMENT_ID,
            payload: {
                noteId: 'third-note-id',
                oldResourceId: 'second-attach',
                newResourceId: 'new-second-attach-id'
            }
        });
        expect(newState).toEqual(Object.assign({}, defaultState, {
            notes: {
                'first-note-id': defaultState.notes['first-note-id'],
                'second-note-id': defaultState.notes['second-note-id'],
                'third-note-id': Object.assign(
                    {},
                    defaultState.notes['third-note-id'],
                    {
                        attachments: {
                            'first-attach': mockedNotesState['third-note-id'].attachments['first-attach'],
                            'new-second-attach-id': {
                                resourceId: 'new-second-attach-id',
                                preview: 'second-preview-url',
                                file: 'second-file-url'
                            }
                        },
                        attachmentOrder: ['first-attach', 'new-second-attach-id']
                    }
                )
            }
        }));
        expect(newState.notes).not.toBe(defaultState.notes);
        expect(newState.notes['first-note-id']).toBe(defaultState.notes['first-note-id']);
        expect(newState.notes['second-note-id']).toBe(defaultState.notes['second-note-id']);
        expect(newState.notes['third-note-id']).not.toBe(defaultState.notes['third-note-id']);
        expect(newState.notes['third-note-id'].attachments).not.toBe(defaultState.notes['third-note-id'].attachments);
        expect(newState.notes['third-note-id'].attachmentOrder).not.toBe(defaultState.notes['third-note-id'].attachmentOrder);
        expect(newState.notes['third-note-id'].tags).toBe(defaultState.notes['third-note-id'].tags);
        expect(newState.notes['third-note-id'].content).toBe(defaultState.notes['third-note-id'].content);
    });
    it('UPDATE_NOTES_SLIDER', () => {
        const defaultState = getDefaultState();
        const newState = notesReducer(defaultState, {
            type: UPDATE_NOTES_SLIDER,
            payload: 'first-attach'
        });
        expect(newState.sliderResourceId).toEqual('first-attach');
        expect(newState.notes).toBe(defaultState.notes);
    });
    describe('SET_BLOCK_NOTE_SELECTION', () => {
        const defaultState = getDefaultState();

        it('add block', () => {
            const newState = notesReducer(defaultState, {
                type: SET_BLOCK_NOTE_SELECTION,
                payload: true
            });

            expect(newState.notes).toBe(defaultState.notes);
            expect(newState.current).toBe(defaultState.current);
            expect(newState.blockNoteSelection).toBe(true);
        });
        it('remove block', () => {
            const newState = notesReducer(defaultState, {
                type: SET_BLOCK_NOTE_SELECTION,
                payload: false
            });

            expect(newState.notes).toBe(defaultState.notes);
            expect(newState.current).toBe(defaultState.current);
            expect(newState.blockNoteSelection).toBe(false);
        });
    });
});
