jest.disableAutomock();

import {findAction} from '../store';

const actionOne = {type: 'SET_ONE'};
const actionTwo = {type: 'SET_TWO'};

describe('findAction', () => {
    it('Для пустого массива ничего не вернет', () => {
        expect(findAction([], 'SET_ONE')).toBeUndefined();
    });

    it('Если в списке экшенов нет искомого - ничего не вернём', () => {
        expect(findAction([actionTwo], 'SET_ONE')).toBeUndefined();
    });

    it('Если в списке содержится экшен с искомым типом - вернём его', () => {
        expect(findAction([actionOne, actionTwo], 'SET_TWO')).toBe(actionTwo);
    });
});
