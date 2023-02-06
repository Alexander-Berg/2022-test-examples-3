jest.disableAutomock();

import configureStore from 'redux-mock-store';
import createThunkMiddleware from '../../storeMiddleware/createThunkMiddleware';

const middlewares = [createThunkMiddleware()];

export function mockStore(state) {
    return configureStore(middlewares)(state);
}
