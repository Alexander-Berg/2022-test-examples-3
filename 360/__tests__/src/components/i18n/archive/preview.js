/* global langs */
import Preview from 'components/archive/preview';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import _ from 'lodash';
import { Provider } from 'react-redux';

const runTest = (lang, ownState) => {
    global.LANG = lang;
    const state = {
        archive: {
            selectedFile: {
                name: '',
                path: '',
                viewable: false,
                encrypted: false
            }
        }
    };
    const store = init(_.merge({}, state, ownState));
    const component = render(
        <Provider store={store}>
            <Preview />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

for (let i = 0; i < langs.length; i++) {
    const langPrefix = '[' + langs[i].toUpperCase() + '] ';
    it(langPrefix + 'archive-preview (no selected file)', () => {
        runTest(langs[i]);
    });
    it(langPrefix + 'archive-preview (encrypted file)', () => {
        runTest(langs[i], {
            archive: {
                selectedFile: {
                    name: 'some-file.xlsx',
                    path: 'some-file.xlsx',
                    viewable: true,
                    encrypted: true
                }
            }
        });
    });
}
