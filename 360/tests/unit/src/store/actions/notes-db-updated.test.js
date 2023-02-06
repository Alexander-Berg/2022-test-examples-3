import notesDBUpdated from '../../../../../src/store/actions/notes-db-updated';
import { UPDATE_NOTES } from '../../../../../src/store/types';
import { STATES } from '../../../../../src/consts';

jest.mock('../../../../../src/store/actions', () => ({
    updateCurrent: jest.fn()
}));
jest.mock('../../../../../src/store/actions/attachments', () => ({
    toggleSlider: jest.fn(),
    batchRequestAttachments: jest.fn()
}));
jest.mock('@ps-int/ufo-helpers/lib/datasync', () => ({
    processRecord: jest.fn((record) => record)
}));
jest.mock('../../../../../src/helpers/merge-note', () => jest.fn((_, record) => record));

import { updateCurrent } from '../../../../../src/store/actions';
import { toggleSlider, batchRequestAttachments } from '../../../../../src/store/actions/attachments';

describe('notesDBUpdated', () => {
    const getMockedGetState = ({
        current,
        sliderResourceId
    }) => () => ({
        notes: {
            current,
            sliderResourceId,
            notes: {}
        }
    });
    const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch, getMockedGetState({})) : arg);
    const DB_RECORDS = [
        {
            id: 'note1',
            recordId: 'note1',
            attachments: {
                attachment1: {}
            }
        },
        {
            id: 'note2',
            recordId: 'note2',
            attachments: {}
        }
    ];
    const db = {
        forEach(_, callback) {
            DB_RECORDS.forEach(callback);
        }
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should update notes in store and request attachments for them', () => {
        notesDBUpdated(db)(dispatch, getMockedGetState({ current: 'note1', sliderResourceId: null }));
        expect(updateCurrent).not.toBeCalled();
        expect(toggleSlider).not.toBeCalled();
        expect(dispatch.mock.calls).toContainEqual([
            expect.objectContaining({
                type: UPDATE_NOTES,
                payload: { state: STATES.LOADED, notes: expect.anything() }
            })
        ]);
        expect(batchRequestAttachments).toBeCalled();
    });

    it('should deselect note in store if it was remotely deleted', () => {
        notesDBUpdated(db)(dispatch, getMockedGetState({ current: 'note3', sliderResourceId: null }));
        expect(popFnCalls(updateCurrent)[0]).toEqual([null]);
        expect(toggleSlider).not.toBeCalled();
    });

    it('should close slider if an opened attachment in slider was remotely deleted', () => {
        notesDBUpdated(db)(dispatch, getMockedGetState({ current: 'note1', sliderResourceId: 'attachment3' }));
        expect(updateCurrent).not.toBeCalled();
        expect(popFnCalls(toggleSlider)[0]).toEqual([null]);
    });
});
