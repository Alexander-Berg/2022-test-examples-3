/* global langs */
import Pager from 'components/pager';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

const runTests = (lang) => {
    global.LANG = lang;
    const initStore = (total, current) => init({
        cfg: {
            ua: {},
            experiments: { flags: {} }
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

for (let i = 0; i < langs.length; i++) {
    it(langs[i] + '-pager', () => {
        runTests(langs[i]);
    });
}
