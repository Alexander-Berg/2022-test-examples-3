const header = require('../../../../dist/web4/serp-header.desktop');

const langsAndTlds = require('../../../tlds-and-langs');

const ctx = {
    query: 'Яндекс Шапка',
    nonce: 123,
};

describe('serp-header', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            test(`tld ${tld}, lang ${lang}`, () => {
                let content = header.getContent({
                    lang,
                    tld,
                    content: 'html',
                    hiddenParams: [{ name: 'test', value: 1 }],

                    ctx,
                })
                    .replace(/(uniq\d.+?)[\\|\s]/g, '');

                expect(content).toMatchSnapshot();
            });
        });
    });

    describe('paint tracking', () => {
        test('browserName === YandexBrowser', () => {
            var content = header.getContent({
                content: 'html',
                ctx: {
                    browserName: 'YandexBrowser'
                }
            })
                .replace(/(uniq\d.+?)[\\|\s]/g, '');

            expect(content).toMatchSnapshot();
        });

        test('browserName === YandexBrowser && addTrackingForHeader', () => {
            var content = header.getContent({
                content: 'html',
                ctx: {
                    browserName: 'YandexBrowser',
                    addTrackingForHeader: true
                }
            })
                .replace(/(uniq\d.+?)[\\|\s]/g, '');

            expect(content).toMatchSnapshot();
        });
    });

    describe('escaping', () => {
        test('quotes', () => {
            var content = header.getContent({
                content: 'html',
                ctx: {
                    query: 'This is a \'"quoted"\' text',
                },
            })
                .replace(/(uniq\d.+?)[\\|\s]/g, '');

            expect(content).toMatchSnapshot();
        });

        test('tags', () => {
            var content = header.getContent({
                content: 'html',
                ctx: {
                    query: 'Hello "/> <strong>world</strong>',
                },
            })
                .replace(/(uniq\d.+?)[\\|\s]/g, '');

            expect(content).toMatchSnapshot();
        });
    });

    describe('cgi params', () => {
        test('noreask=1', () => {
            let content = header.getContent({
                lang: 'ru',
                tld: 'ru',
                content: 'html',
                hiddenParams: [{ name: 'test', value: 1 }],

                ctx: {
                    ...ctx,
                    clid: '123',
                    noreask: '1',
                },
            });

            const container = document.createElement('div');
            container.innerHTML = content;
            const services = Array.from(container.querySelectorAll('a.service__url'));
            const hasNoreaskArray = services.map(service => service.href.includes('noreask=1'));

            expect(hasNoreaskArray.every(Boolean)).toBe(true);
        });

        test('noreask=0', () => {
            let content = header.getContent({
                lang: 'ru',
                tld: 'ru',
                content: 'html',
                hiddenParams: [{ name: 'test', value: 1 }],

                ctx: {
                    ...ctx,
                    clid: '123',
                },
            });

            const container = document.createElement('div');
            container.innerHTML = content;
            const services = Array.from(container.querySelectorAll('a.service__url'));
            const hasNoreaskArray = services.map(service => !service.href.includes('noreask=1'));

            expect(hasNoreaskArray.every(Boolean)).toBe(true);
        });
    });
});
