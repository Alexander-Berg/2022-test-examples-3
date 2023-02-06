import { setError, errorsReducer } from '../error';

describe('Errors reducer', () => {
    describe('#setError', () => {
        it('Should return error', () => {
            const initialState = {
                error: null,
                label: '',
            };
            const error = {
                stack: 'at thisFunction()',
                name: 'out of memory',
                message: 'do something',
            };
            const label = 'something happened';
            const newState = errorsReducer(initialState, setError(error, label));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({ error, label });
        });
    });
});
