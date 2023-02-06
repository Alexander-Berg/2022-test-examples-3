import { forwardReducer, search, clearSuggest, initialState, ForwardState } from '../forward';
import { RequestStatus } from '../../typings/assistant';

describe('Forward reducer', () => {
    const data = {
        suggest: [{
            entity: 'chat',
            data: {
                name: 'chat',
                version: 0,
                chat_id: 'chat_id',
                description: '',
            },
        }],
        query: 'foo',
        requestStatus: 'progress' as RequestStatus,
    } as any as ForwardState;

    describe('#forwardSearch', () => {
        it('Should return new state with data', () => {
            const newState = forwardReducer(initialState, search(data));

            expect(newState).not.toBe(initialState);
            expect(newState).toEqual({
                prevStatus: initialState.prevStatus,
                ...data,
            });
        });
    });

    describe('#clearForwardSuggest', () => {
        it('Should return clear state', () => {
            const newState = forwardReducer(data, clearSuggest);

            expect(newState).not.toBe(data);
            expect(newState).toEqual(initialState);
        });
    });
});
