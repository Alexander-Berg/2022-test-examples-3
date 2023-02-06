const header = require('../../../../dist/tutor/turbo-header.touch-phone');
const user2 = require('../../../../dist/base/user2.touch-phone');
const notifier = require('../../../../dist/base/notifier.touch-phone');

const langsAndTlds = require('../../../tlds-and-langs');

const ctx = {
    favoritesHTML: '',
    query: 'Hello Header',
    hasSearch: true,
    isInputHidden: false,
    hiddenParams: [{ name: 'test', value: 1 }, { name: 'test2', value: 2 }],
    serviceLogoUrl: '/test"<script>alert(1)</script>',
    uid: 226291201,
    notifierHTML: notifier.getContent({ content: 'html' }),
    userHTML: user2.getContent({
        content: 'html',
        ctx: {
            uid: 226291201,
            yu: 7390764501529155693,
            avatarId: '20706/226291201-19837531',
            subname: 'smdenis@yandex-team.ru',
            name: 'Denis Smirnov',
            retpath: '//yandex.ru',
        },
    }),
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
