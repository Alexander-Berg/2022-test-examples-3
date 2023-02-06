const user2 = require('../../../../dist/organizations/user2.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

const ctxs = [
    {
        uid: 226291201,
        yu: 7390764501529155693,
        secretKey: 'u1234567890abc',
        source: 'turbo',
        preset: 'preset',
        avatarId: '20706/226291201-19837531',
        name: 'Denis Smirnov',
        retpath: '//yandex.ru',
        hasPlus: true,
    },
    {
        uid: 226291201,
        yu: 7390764501529155693,
        secretKey: 'u1234567890abc',
        avatarId: '20706/226291201-19837531',
        name: 'Denis Smirnov',
        retpath: '//yandex.ru',
        hasPlus: true,
    },
];

describe('user2', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            ctxs.forEach((ctx, i) => {
                test(`tld ${tld}, lang ${lang}, ${i}`, () => {
                    let content = user2.getContent({
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
});
