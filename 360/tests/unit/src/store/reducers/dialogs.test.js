import dialogsReducer from '../../../../../src/store/reducers/dialogs';
import { OPEN_DIALOG, CLOSE_DIALOG } from '../../../../../src/store/types';
import { DIALOG_STATES } from '../../../../../src/consts';

describe('store/reducers/dialogs', () => {
    it('should return empty object by default', () => {
        expect(dialogsReducer(undefined, { type: 'any' })).toEqual({});
    });
    it('OPEN_DIALOG', () => {
        expect(dialogsReducer({}, {
            type: OPEN_DIALOG,
            payload: {
                name: 'noteDeleteConfirmation'
            }
        })).toEqual({
            noteDeleteConfirmation: {
                state: DIALOG_STATES.OPENED
            }
        });
    });
    it('OPEN_DIALOG with data', () => {
        expect(dialogsReducer({}, {
            type: OPEN_DIALOG,
            payload: {
                name: 'attachmentDeleteConfirmation',
                data: { resourceId: 'resource-id', source: 'from list' }
            }
        })).toEqual({
            attachmentDeleteConfirmation: {
                state: DIALOG_STATES.OPENED,
                data: { resourceId: 'resource-id', source: 'from list' }
            }
        });
    });
    it('CLOSE_DIALOG', () => {
        expect(dialogsReducer({
            noteDeleteConfirmation: {
                state: DIALOG_STATES.OPENED
            }
        }, {
            type: CLOSE_DIALOG,
            payload: {
                name: 'noteDeleteConfirmation'
            }
        })).toEqual({
            noteDeleteConfirmation: {
                state: DIALOG_STATES.CLOSED,
                data: {}
            }
        });
    });
});
