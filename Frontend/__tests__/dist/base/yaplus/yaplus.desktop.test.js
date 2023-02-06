const yaplus = require('../../../../dist/base/yaplus.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

const bundles = ['base', 'white', 'text', 'old-icon'];

const ctx = {
    yaplusUrlParams: '?test=1',
};

describe('yaplus', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            bundles.map(key => {
                test(`key ${key}, tld ${tld}, lang ${lang}`, () => {
                    let content = yaplus.getContent({
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
