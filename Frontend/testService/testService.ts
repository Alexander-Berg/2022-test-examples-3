import { ServiceAction } from 'redux/redux';

import { State } from 'schema/state/State';

export function testService<T>(state: any, service: ServiceAction<T>): State {
    let dispatch = (action) => {
        state = action.payload;
    };

    let getState = () => state;
    let setState = (newState) => state = newState;

    service({ dispatch, getState, setState });

    return state;
}
