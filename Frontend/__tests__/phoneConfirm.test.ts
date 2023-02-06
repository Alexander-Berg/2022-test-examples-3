import {
    phoneConfirmReducer,
    bindPhone,
    backToBind,
    clearPhoneConfirmData,
    errorConfirmPhone,
    initialState,
} from '../phoneConfirm';

describe('PhoneConfirm reducer', () => {
    const data = {
        number: '78000000000',
        trackId: 'track',
    };

    describe('#bindPhone', () => {
        it('Should return new state with data', () => {
            const newState = phoneConfirmReducer(initialState, bindPhone(data));

            expect(newState).not.toBe(initialState);
            expect(newState).toEqual(data);
        });
    });

    describe('#backToBind', () => {
        it('Should return new state without error and trackId', () => {
            const newState = phoneConfirmReducer({ error: 'Error', ...data }, backToBind());

            expect(newState).toEqual({
                ...data,
                error: undefined,
                trackId: '',
            });
        });
    });

    describe('#clearPhoneConfirmData', () => {
        it('Should return initial state', () => {
            const newState = phoneConfirmReducer(data, clearPhoneConfirmData());

            expect(newState).toEqual(initialState);
        });
    });

    describe('#errorConfirmPhone', () => {
        it('Should return new state with error', () => {
            const newState = phoneConfirmReducer(data, errorConfirmPhone('Error message'));

            expect(newState).toEqual({
                ...data,
                error: 'Error message',
            });
        });
    });
});
