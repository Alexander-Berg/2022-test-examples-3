import { typingsReducer, add, remove } from '../typings';
import { MessageType } from '../../constants/message';
import { SharedActions } from '..';

const chatId = '0/0/5124bca0-2b6f-40f8-9a06-b55ebd61f7ec';
const guid = 'a89c56c4-5245-4c94-b988-d1c1403971d7';

const now = Date.now();

const message: APIv3.Message = {
    chatId: chatId,
    deleted: false,
    from: {
        display_name: 'John Doe',
        version: 1,
        guid,
    },
    messageId: `id-${now}`,
    type: MessageType.UNKNOWN,
    timestamp: now,
    prevTimestamp: now - 1,
    version: 1,
};

describe('Typings reducer', () => {
    describe('addTyping', () => {
        it('creates queue if state was empty', () => {
            const initialState = {};
            const newState = typingsReducer(initialState, add(chatId, guid));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                [ chatId ]: [guid],
            });
        });

        it('inserts guid in the queue if not exists', () => {
            const initialState = {
                [ chatId ]: [guid],
            };

            const newGuid = guid + '1';
            const newState = typingsReducer(initialState, add(chatId, newGuid));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                [ chatId ]: [guid, newGuid],
            });
        });

        it('returns the same state if guid is exists in the queue', () => {
            const initialState = {
                [ chatId ]: [guid],
            };

            const newState = typingsReducer(initialState, add(chatId, guid));

            expect(newState).toBe(initialState);
        });
    });

    describe('removeTyping', () => {
        it('removes guid from queue if exists', () => {
            const initialState = {
                [ chatId ]: [guid],
            };

            const newState = typingsReducer(initialState, remove(chatId, guid));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                [ chatId ]: [],
            });
        });

        it('returns the same state if guid is not exists in queue', () => {
            const initialState = {
                [ chatId ]: [guid + '1'],
            };

            const newState = typingsReducer(initialState, remove(chatId, guid));

            expect(newState).toBe(initialState);
        });
    });

    describe('replaceMessage', () => {
        it('removes guid from queue if exists', () => {
            const initialState = {
                [ chatId ]: [guid],
            };

            const newState = typingsReducer(
                initialState,
                SharedActions.updateMessages([message], guid),
            );

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                [ chatId ]: [],
            });
        });

        it('returns the same state if guid is not exists in queue', () => {
            const initialState = {
                [ chatId ]: [guid + '1'],
            };

            const newState = typingsReducer(
                initialState,
                SharedActions.updateMessages([message], guid),
            );

            expect(newState).toBe(initialState);
        });
    });
});
