/* global langs */
import EditPromo from 'components/edit-promo';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

const runTest = (lang) => {
    global.LANG = lang;
    const store = init({
        doc: {
            title: '',
            actions: {
                edit: {
                    url: 'edit-url'
                }
            }
        }
    });
    const component = render(
        <Provider store={store}>
            <EditPromo/>
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

for (let i = 0; i < langs.length; i++) {
    it('[' + langs[i].toUpperCase() + '] edit-promo', () => {
        runTest(langs[i]);
    });
}
