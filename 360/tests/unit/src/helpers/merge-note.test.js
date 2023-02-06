import mergeNote from '../../../../src/helpers/merge-note';
import { singleNote } from '../../../fixtures/notes';
import deepFreeze from 'deep-freeze';
import { TRASH_TAG_CODE, DELETED_TAG_CODE, PIN_TAG_CODE, STATES } from '../../../../src/consts';

describe('src/helpers/merge-note =>', () => {
    // зафризим исходную заметку. Мерж должен создавать новый объект, а не модифицировать существующий
    deepFreeze(singleNote);

    it('should merge note', () => {
        expect(mergeNote(singleNote, {
            title: 'new title',
            snippet: 'new snippet',
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:06.293Z',
            tags: [PIN_TAG_CODE],
            attach_resource_ids: singleNote.attachmentOrder
        })).toMatchSnapshot();
    });

    it('should remove attachments if got none from backend', () => {
        const mergedNote = mergeNote(singleNote, {
            title: singleNote.title,
            snippet: singleNote.snippet,
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:07.000Z',
            tags: [PIN_TAG_CODE],
            attach_resource_ids: []
        });
        expect(mergedNote.attachmentOrder).toEqual([]);
        expect(mergedNote.attachments).toEqual({});
        expect(mergedNote).toMatchSnapshot();
    });

    it('should add new attachments if got some from backend', () => {
        const mergedNote = mergeNote(singleNote, {
            title: singleNote.title,
            snippet: singleNote.snippet,
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:08.000Z',
            tags: [PIN_TAG_CODE],
            attach_resource_ids: singleNote.attachmentOrder.concat('new-attachment')
        });
        expect(mergedNote.attachmentOrder.length).toEqual(2);
        expect(mergedNote).toMatchSnapshot();
    });

    it('should rewrite tags by remote values', () => {
        const mergedNote = mergeNote(singleNote, {
            title: singleNote.title,
            snippet: singleNote.snippet,
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:09.000Z',
            tags: [],
            attach_resource_ids: singleNote.attachmentOrder
        });
        expect(mergedNote.tags).toEqual({});
    });

    it('datasync specific format for tags and attachments', () => {
        expect(mergeNote(singleNote, {
            title: 'new title',
            snippet: 'new snippet',
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:10.000Z',
            tags: [{
                _type: 'list',
                _value: [{
                    _type: 'integer',
                    _value: PIN_TAG_CODE
                }, {
                    type: 'datetime',
                    _value: '2019-05-16T13:25:46.739Z'
                }]
            }],
            attach_resource_ids: [{
                _value: singleNote.attachmentOrder[0]
            }]
        }, true)).toMatchSnapshot();
    });

    it('datasync old (before December, 2017) specific format for tags', () => {
        expect(mergeNote(singleNote, {
            title: 'new title',
            snippet: 'new snippet',
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:11.000Z',
            tags: [{
                _type: 'integer',
                _value: PIN_TAG_CODE
            }],
            attach_resource_ids: [{
                _value: singleNote.attachmentOrder[0]
            }]
        }, true)).toMatchSnapshot();
    });

    it('should not merge deleted note', () => {
        expect(mergeNote(singleNote, {
            title: singleNote.title,
            snippet: singleNote.snippet,
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:12.000Z',
            tags: [DELETED_TAG_CODE],
            attach_resource_ids: singleNote.attachmentOrder
        })).toBeUndefined();
        expect(mergeNote(singleNote, {
            title: singleNote.title,
            snippet: singleNote.snippet,
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:13.000Z',
            tags: [TRASH_TAG_CODE],
            attach_resource_ids: singleNote.attachmentOrder
        })).toBeUndefined();
    });

    it('should not remove creating attachments and should not add new while attachments creating', () => {
        const noteWithCreatingAttachment = Object.assign({}, singleNote, {
            attachments: Object.assign({}, singleNote.attachments, {
                'creating-attachment-1': {
                    state: STATES.CREATING
                },
                'creating-attachment-2': {
                    state: STATES.CREATING
                }
            }),
            attachmentOrder: singleNote.attachmentOrder.concat(['creating-attachment-1', 'creating-attachment-2'])
        });
        deepFreeze(noteWithCreatingAttachment);
        const mergedNote = mergeNote(noteWithCreatingAttachment, {
            title: singleNote.title,
            snippet: singleNote.snippet,
            ctime: singleNote.ctime,
            mtime: '2019-05-16T13:21:14.000Z',
            tags: [PIN_TAG_CODE],
            attach_resource_ids: singleNote.attachmentOrder.concat('some-new-attach-id')
        });
        expect(mergedNote.newAttachments).toEqual(['some-new-attach-id']);
        expect(mergedNote.attachments).toEqual(noteWithCreatingAttachment.attachments);
        expect(mergedNote.attachmentOrder).toEqual(noteWithCreatingAttachment.attachmentOrder);
        expect(mergedNote).toMatchSnapshot();
    });

    it('when new note has come', () => {
        expect(mergeNote(undefined, {
            title: 'title',
            snippet: 'snippet',
            ctime: '2019-05-16T13:21:15.000Z',
            mtime: '2019-05-16T13:21:15.000Z',
            tags: []
        })).toMatchSnapshot();
    });
});
