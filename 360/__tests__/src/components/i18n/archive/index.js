/* global langs */
import Archive from 'components/archive';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

const runTest = (lang) => {
    global.LANG = lang;
    const state = {
        doc: {
            title: 'archive-1.7z',
            size: 100500
        },
        archive: {
            listing: {},
            nestedCount: 5,
            path: '',
            selectedFile: {
                path: ''
            }
        }
    };
    const store = init(state);
    const component = render(
        <Provider store={store}>
            <Archive />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

for (let i = 0; i < langs.length; i++) {
    it('[' + langs[i].toUpperCase() + '] archive', () => {
        runTest(langs[i]);
    });
}
