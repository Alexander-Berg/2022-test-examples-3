import Pager from 'components/pager';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

jest.mock('lib/helpers/fullscreen');

const runTests = () => {
    global.LANG = 'ru';
    const initStore = (total, current) => init({
        cfg: {
            ua: {},
            isMounted: true
        },
        doc: {
            pages: new Array(total)
        },
        url: {
            query: {
                page: current
            }
        }
    });
    let store;

    store = initStore(1, 1);
    let component = render(
        <Provider store={store}>
            <Pager/>
        </Provider>
    );
    expect(component).toMatchSnapshot();

    store = initStore(5, 1);
    component = render(
        <Provider store={store}>
            <Pager/>
        </Provider>
    );
    expect(component).toMatchSnapshot();

    store = initStore(17, 12);
    component = render(
        <Provider store={store}>
            <Pager/>
        </Provider>
    );
    expect(component).toMatchSnapshot();

    store = initStore(101, 101);
    component = render(
        <Provider store={store}>
            <Pager/>
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

it('pager without fullscreen', () => {
    runTests();
});

it('pager with fullscreen', () => {
    global.ALLOW_FULLSCREEN = true;
    runTests();
    global.ALLOW_FULLSCREEN = false;
});
