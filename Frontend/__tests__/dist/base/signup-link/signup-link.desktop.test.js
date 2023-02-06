const signup = require('../../../../dist/base/signup-link.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

const ctx = {
    origin: 'header',
    retpath: 'https://yandex.ru',
};

describe('signup-link', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            test(`tld ${tld}, lang ${lang}`, () => {
                let content = signup.getContent({
                    lang,
                    tld,
                    content: 'html',
                    ctx,
                });

                expect(content).toMatchSnapshot();
            });
        });
    });
});
