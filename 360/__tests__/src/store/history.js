import * as actions from '../../../src/store/actions';
import TOKEN_ARG from '../../../src/lib/token-arg';
import getStore from '../../../src/store';

const defaultState = {
    url: {
        query: {
            [TOKEN_ARG]: 'token'
        }
    },
    doc: {
        pages: new Array(3)
    }
};

const store = getStore(defaultState);

it('store/history', () => {
    expect(window.history.length).toEqual(1);

    // pushState
    store.dispatch(actions.goToPage(2));
    expect(window.location.toString()).toMatchSnapshot();
    // pushState - длина истории увеличилась на 1
    expect(window.history.length).toEqual(2);

    // replaceState
    store.dispatch(actions.goToPage(3, true));
    expect(window.location.toString()).toMatchSnapshot();
    // replaceState - длина истории не изменилась
    expect(window.history.length).toEqual(2);
});
