import deepFreeze from 'deep-freeze';
import { createStore, applyMiddleware, compose } from 'redux';
import reducers from '../../../../src/store/reducers';
import getMiddlewares from '../../../../src/store/middlewares';

jest.mock('../../../../src/lib/api');

const getStore = (initialState) => {
    const middlewares = getMiddlewares();
    let actions = [];
    const logAction = () => (next) => (action) => {
        if (typeof action !== 'function') {
            actions.push(action.type);
        }
        return next(action);
    };
    middlewares.push(logAction);

    const enhancers = compose(
        applyMiddleware(...middlewares)
    );

    const store = createStore(
        reducers,
        initialState,
        enhancers
    );

    const originalGetState = store.getState;

    return Object.assign(store, {
        getActions() {
            return actions;
        },
        clearActions() {
            actions = [];
        },
        getState() {
            const state = originalGetState();
            // фризим state чтобы отловаить наркоманские ситуации
            // когда в экшенах кто-то взял кусок стора и дописал туда полей
            deepFreeze(state);
            return state;
        }
    });
};

export default getStore;
