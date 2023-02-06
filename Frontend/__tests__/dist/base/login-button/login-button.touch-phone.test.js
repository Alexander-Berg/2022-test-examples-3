const login = require('../../../../dist/base/login-button.touch-phone');

const langsAndTlds = require('../../../tlds-and-langs');

const bundles = ['base', 'button', 'button-pic'];

const ctx = {
    origin: 'header',
    retpath: 'https://yandex.ru',
};

describe('login-button', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            bundles.map(key => {
                test(`key ${key}, tld ${tld}, lang ${lang}`, () => {
                    let content = login.getContent({
                        lang,
                        tld,
                        key,
                        content: 'html',
                        ctx,
                    });

                    expect(content).toMatchSnapshot();
                });
            });
        });
    });
});
