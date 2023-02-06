const header = require('../../../../dist/health/yandex-header.desktop');
const user2 = require('../../../../dist/base/user2.desktop');
const notifier = require('../../../../dist/base/notifier.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

const bundles = ['base', 'no-search'];

const ctx = {
    nav: [
        { href: '/', text: 'Мои организации', active: true },
        { href: '/', text: 'Мои заявки' },
        { href: '/', text: 'Правки' },
    ],
    query: '/test"<script>alert(1)</script>',
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

describe('yandex-header', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            bundles.map(key => {
                test(`key ${key}, tld ${tld}, lang ${lang}`, () => {
                    let content = header.getContent({
                        key,
                        lang,
                        tld,
                        content: 'html',
                        ctx: Object.assign({ hasSearch: key === 'base' }, ctx),
                    });

                    expect(content).toMatchSnapshot();
                });
            });
        });
    });
});
