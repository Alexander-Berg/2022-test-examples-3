const header = require('../../../../dist/mail/yandex-header.desktop');
const user2 = require('../../../../dist/mail/user2.desktop');
const notifier = require('../../../../dist/base/notifier.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

const ctx = {
    query: '/test"<script>alert(1)</script>',
    hiddenParams: [{ name: 'test', value: 1 }, { name: 'test2', value: 2 }],
    serviceLogoUrl: '/test"<script>alert(1)</script>',
    uid: 226291201,
    hasServiceAll: true,
    allServicesUrl: '//ya.ru',
    nav: [{ href: '/', text: 'Контакты', active: false }],
    extra: [
        { href: '/', text: 'Диск', icon: '' },
        { href: '/', text: 'Заметки', icon: '' },
        { href: '/', text: 'Календарь', icon: '' },
    ],
    customLogoImageUrl: '//ya.ru',
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
