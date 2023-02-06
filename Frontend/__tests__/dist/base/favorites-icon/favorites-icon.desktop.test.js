const favorites = require('../../../../dist/base/favorites-icon.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

describe('favorites-icon', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            test(`tld ${tld}, lang ${lang}`, () => {
                let content = favorites.getContent({ lang, tld, content: 'html' });

                expect(content).toMatchSnapshot();
            });
        });
    });
});
