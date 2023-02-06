const header3 = require('../../../../dist/images/header3.touch-phone');

const langsAndTlds = require('../../../tlds-and-langs');

const ctx = {
    query: 'LEGO-2602: Разъезжается шапка Здоровья в IE11',
    formAction: '/test/',
    clid: 526,
};

const bundles = ['base', 'cbir-page'];

describe('header3', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            bundles.forEach(key => {
                test(`key ${key}, tld ${tld}, lang ${lang}`, () => {
                    const content = header3.getContent({
                        key,
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

    describe('tabsMordaUrls', () => {
        test('force morda urls', () => {
            const content = header3.getContent({
                content: 'html',
                ctx: Object.assign({}, ctx, { tabsMordaUrls: true })
            });

            expect(content).toMatchSnapshot();
        });
    });
});
