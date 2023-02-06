/* global langs */
import Title from 'components/doc/title';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

const runTest = (lang, protocol) => {
    global.LANG = lang;
    const state = {
        doc: {
            protocol,
            serpUrl: (/s:$/.test(protocol) ? 'https' : 'http') + '://ya.ru/docs/doc-from-serp.pdf',
            serpHost: 'ya.ru',
            serpLastAccess: 1488292801428,
            title: 'doc-from-serp.pdf'
        }
    };
    const store = init(state);

    const component = render(
        <Provider store={store}>
            <Title/>
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

const protocols = ['http:', 'https:', 'ya-serp:', 'ya-serps:'];

for (let i = 0; i < langs.length; i++) {
    for (let j = 0; j < protocols.length; j++) {
        const lang = langs[i];
        const proto = protocols[j];
        it('[' + lang.toUpperCase() + '] ' + proto + ' Title', () => {
            runTest(lang, proto);
        });
    }
}
