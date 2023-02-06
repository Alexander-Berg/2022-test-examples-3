/* global langs */
import NoUrl from 'components/no-url';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { i18n } from '@ps-int/react-tanker';
import { Provider } from 'react-redux';

const runTest = (lang) => {
    global.LANG = lang;
    const title = i18n(lang, 'default', 'title');
    expect(title).toMatchSnapshot();

    const store = init({});
    const component = render(
        <Provider store={store}>
            <NoUrl />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

for (let i = 0; i < langs.length; i++) {
    it(langs[i] + '-no-url', () => {
        runTest(langs[i]);
    });
}
