/* global langs */
import HTML from 'components/doc/html';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

jest.mock('lib/direct');

const runTest = (lang) => {
    global.LANG = lang;
    const state = {
        cfg: {
            ua: {},
            experiments: { flags: {} }
        },
        doc: {
            title: 'вжух-вжух.pdf',
            pages: [{
                index: 1,
                state: 'FAIL'
            }],
            actions: {}
        },
        user: { features: {} }
    };
    const store = init(state);
    const component = render(
        <Provider store={store}>
            <HTML />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

for (let i = 0; i < langs.length; i++) {
    it(langs[i] + '-load-page-error', () => {
        runTest(langs[i]);
    });
}
