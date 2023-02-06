import deepFreeze from 'deep-freeze';
import { ADD_WAITING_AUTH_ACTION, CLEAR_WAITING_AUTH_ACTIONS } from '../../../../../src/store/action-types';
import waitingAuthActions from '../../../../../src/store/reducers/waiting-auth-actions';

it('состояние по умолчанию', () => {
    expect(waitingAuthActions(undefined, {})).toEqual({});
});

it('добавление действия, ожидающего авторизацию', () => {
    expect(waitingAuthActions(undefined, {
        type: ADD_WAITING_AUTH_ACTION,
        name: 'actionName',
        value: 'action value'
    })).toEqual({ actionName: 'action value' });
});

it('добавление двух действий, ожидающих авторизацию', () => {
    const firstState = waitingAuthActions(undefined, {
        type: ADD_WAITING_AUTH_ACTION,
        name: 'firstActionName',
        value: 'first action value'
    });
    deepFreeze(firstState);
    const secondState = waitingAuthActions(firstState, {
        type: ADD_WAITING_AUTH_ACTION,
        name: 'secondActionName',
        value: 'second action value'
    });
    expect(secondState).toEqual({
        firstActionName: 'first action value',
        secondActionName: 'second action value'
    });
});

it('очистка действий, ожидающих авторизацию', () => {
    const initState = {
        a: 1,
        b: null,
        c: 'some string'
    };
    deepFreeze(initState);
    const stateAfter = waitingAuthActions(initState, {
        type: CLEAR_WAITING_AUTH_ACTIONS
    });
    expect(stateAfter).toEqual({});
});
