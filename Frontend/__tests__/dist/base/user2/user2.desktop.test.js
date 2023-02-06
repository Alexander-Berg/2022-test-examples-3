const user2 = require('../../../../dist/base/user2.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

const ctxs = [
    {
        uid: 226291201,
        yu: 7390764501529155693,
        avatarId: '20706/226291201-19837531',
        name: 'Denis Smirnov',
        retpath: "//yandex.ru?text=test'as",
        yaplusAvailable: true,
        customMenuItems: [
            { text: 'Custom menu item', url: '//yandex.ru', action: 'custom' },
            { text: 'Yet another custom menu item', url: '//yandex.ru', action: 'custom' }
        ]
    },
    {
        uid: 226291201,
        yu: 7390764501529155693,
        avatarId: '20706/226291201-19837531',
        name: 'Denis Smirnov',
        retpath: '//yandex.ru',
        hasPlus: true,
        yaplusUrlParams: '?key=value',
    },
    {
        uid: 226291201,
        yu: 7390764501529155693,
        avatarId: '20706/226291201-19837531',
        name: 'Denis Smirnov',
        retpath: '//yandex.ru',
    },
    {
        uid: 226291201,
        yu: 7390764501529155693,
        avatarId: '20706/226291201-19837531',
        name: 'Denis Smirnov',
        retpath: '//yandex.ru',
        exitUrl: '//custom.logout/url',
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

    test('Without avatar', () => {
        let content = user2.getContent({
            content: 'html',
            ctx: {
                uid: 226291201,
                yu: 7390764501529155693,
                avatarId: '0/0-0',
                name: 'Without Avatar',
                retpath: '//yandex.ru',
            },
        });

        expect(content).toMatchSnapshot();
    });

    describe('vanilla js', () => {
        const mockOpen = jest.fn();
        const js = user2.getContent({
            content: 'js',
        });
        beforeEach(() => {
            window.XMLHttpRequest = () => {
                return {
                    open: jest.fn().mockImplementation(mockOpen),
                };
            };
        });

        afterEach(() => {
            mockOpen.mockClear();
        });

        test('get counter and accounts', () => {
            const html = user2.getContent({
                content: 'html',
                ctx: {
                    ...ctxs[0],
                },
            });

            const injectJS = new Function(`
                document.body.innerHTML=\`${html}\`;
                ${js};
            `);
            injectJS();
            expect(mockOpen.mock.calls[0][1]).toBe('https://mail.yandex.ru/api/v2/serp/counters?silent');
            expect(mockOpen.mock.calls[1][1]).toBe('https://api.passport.yandex.ru/all_accounts');
        });

        test('ignore XMLHttpRequest with noCounter and accounts ', () => {
            const html = user2.getContent({
                content: 'html',
                ctx: {
                    ...ctxs[0],
                    noCounter: true,
                    accounts: [
                        {
                            name: 'WALL-E Bot',
                            pic: {
                                avatarId: 'robot-serptools',
                            },
                            ticker: { count: 10 },
                            uid: 123456789,
                        },
                    ],
                },
            });

            const injectJS = new Function(`
                document.body.innerHTML=\`${html}\`;
                ${js};
            `);
            injectJS();
            expect(mockOpen.mock.calls.length).toBe(0);
        });

        test('isInternal return center.yandex-team', () => {
            const html = user2.getContent({
                content: 'html',
                ctx: {
                    ...ctxs[0],
                    noCounter: true,
                    isInternal: true,
                },
            });

            const injectJS = new Function(`
                document.body.innerHTML=\`${html}\`;
                ${js};
            `);
            injectJS();

            expect(document.querySelector('.user-pic__image').getAttribute('src')).toMatch('https://center.yandex-team.ru');
        });

        test('avatarHost return custom host', () => {
            const html = user2.getContent({
                content: 'html',
                ctx: {
                    ...ctxs[0],
                    noCounter: true,
                    avatarHost: '//lego.yandex-team.ru',
                },
            });

            const injectJS = new Function(`
                document.body.innerHTML=\`${html}\`;
                ${js};
            `);
            injectJS();

            expect(document.querySelector('.user-pic__image').getAttribute('src')).toMatch('//lego.yandex-team.ru');
        });

        test('default avatar id', () => {
            const html = user2.getContent({
                content: 'html',
                ctx: {
                    ...ctxs[0],
                },
            });

            const injectJS = new Function(`
                document.body.innerHTML=\`${html}\`;
                ${js};
            `);
            injectJS();

            expect(document.querySelector('.user-pic__image').getAttribute('src')).toBe('https://avatars.mds.yandex.net/get-yapic/20706/226291201-19837531/islands-middle');
        });
    });
});
