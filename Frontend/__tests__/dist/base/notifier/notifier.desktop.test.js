const notifier = require('../../../../dist/base/notifier.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

describe('notifier', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            test(`tld ${tld}, lang ${lang}`, () => {
                let content = notifier.getContent({ lang, tld, content: 'html' });

                expect(content).toMatchSnapshot();
            });
        });
    });
});
