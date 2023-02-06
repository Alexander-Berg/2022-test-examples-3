/* global langs */
import Header from 'components/header';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { i18n } from '@ps-int/react-tanker';
import { Provider } from 'react-redux';

// это как подоменный конфиг, только наоборот - соответствие дефолтного tld конкретному языку
const mapLangToTld = {
    ru: 'ru',
    uk: 'ua',
    en: 'com',
    tr: 'tr'
};

const runTest = (lang, docState, auth, saveState) => {
    global.LANG = lang;
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            tld: mapLangToTld[lang],
            experiments: {
                flags: {
                    dv_history_exp: true
                }
            }
        },
        user: {
            auth,
            accounts: [{
                id: '4004594257',
                login: 'iegit20',
                name: 'iegit20',
                avatar: 'https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-middle'
            }]
        },
        doc: {
            title: 'тюлень_олень_и_еноты.odt',
            state: docState,
            actions: {
                save: {
                    allow: true,
                    state: saveState,
                    folderUrl: 'some/folder/url'
                },
                edit: {
                    allow: true,
                    url: '/some/edit/url'
                },
                share: {
                    allow: true
                },
                print: {
                    allow: true
                },
                download: {
                    allow: true
                }
            }
        }
    };
    const store = init(state);
    const component = render(
        <Provider store={store}>
            <Header />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

for (let i = 0; i < langs.length; i++) {
    it(langs[i] + '-header', () => {
        runTest(langs[i], 'READY', true, '');
        runTest(langs[i], 'READY', false, 'SAVED');
        runTest(langs[i], 'ARCHIVE', true, '');

        // FixMe: это бы надо тестировать в TooltipPopup, но туда текст загоняется через метод класса, а не от props или store
        const tooltipPopupTexts = {
            share: i18n(langs[i], 'share', 'button'),
            print: i18n(langs[i], 'default', 'print'),
            download: i18n(langs[i], 'default', 'download'),
            edit: [
                i18n(langs[i], 'default', 'edit-tooltip-basic'),
                i18n(langs[i], 'default', 'edit-tooltip-will-save')
            ].join('.\n'),
            shareArchive: i18n(langs[i], 'archive', 'share'),
            downloadArchive: i18n(langs[i], 'archive', 'download')
        };
        expect(tooltipPopupTexts).toMatchSnapshot();

        expect(i18n(langs[i], 'auth', 'add-user')).toMatchSnapshot();
    });
}
