const header = require('../../../../dist/collections/turbo-header.touch-phone');

const langsAndTlds = require('../../../tlds-and-langs');

const ctx = {
    hiddenParams: [
        { name: 'test', value: 1 },
        { name: 'test2', value: 2 },
    ],
    serviceLogoUrl: '/test"<script>alert(1)</script>',
};

describe('turbo-header', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            test(`tld ${tld}, lang ${lang}`, () => {
                let content = header.getContent({
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
