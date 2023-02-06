const header3 = require('../../../../dist/video/header3.touch-phone');

const langsAndTlds = require('../../../tlds-and-langs');

const ctx = {
    query: 'LEGO-2602: Разъезжается шапка Здоровья в IE11',
    formAction: '/test/',
};

describe('header3', () => {
    describe('l10n & tld', () => {
        langsAndTlds.map(([tld, lang]) => {
            test(`tld ${tld}, lang ${lang}`, () => {
                let content = header3.getContent({
                    lang,
                    tld,
                    content: 'html',
                    ctx,
                });

                expect(content).toMatchSnapshot();
            });
        });
    });

    describe('family filter param', () => {
        test('is true, then family=1 is attached to no services\'s urls ', () => {
            let content = header3.getContent({
                ctx: {
                    isFamily: true,
                },
            });

            const container = document.createElement('div');
            container.innerHTML = content;
            const services = Array.from(container.querySelectorAll('a.header3__service2'));
            const hasFamily1Array = services.map(service => service.href.includes('family=1'));

            expect(hasFamily1Array.every(Boolean)).toBe(false);
        });

        test('is false, then family=1 is attached to no services\'s urls ', () => {
            let content = header3.getContent({
                ctx: {
                    isFamily: false,
                },
            });

            const container = document.createElement('div');
            container.innerHTML = content;
            const services = Array.from(container.querySelectorAll('a.header3__service2'));
            const hasNotFamily1Array = services.map(service => !service.href.includes('family=1'));

            expect(hasNotFamily1Array.every(Boolean)).toBe(true);
        });
    });

    describe('noreask', () => {
        test('noreask=1', () => {
            let content = header3.getContent({
                ctx: {
                    noreask: 1,
                },
            });

            const container = document.createElement('div');
            container.innerHTML = content;
            const services = Array.from(container.querySelectorAll('a.header3__service2'));
            const hasNoreaskArray = services.map(service => service.href.includes('noreask=1'));

            expect(hasNoreaskArray.every(Boolean)).toBe(true);
        });

        test('noreask default ', () => {
            let content = header3.getContent({});

            const container = document.createElement('div');
            container.innerHTML = content;
            const services = Array.from(container.querySelectorAll('a.header3__service2'));
            const hasNoreaskArray = services.map(service => !service.href.includes('noreask=1'));

            expect(hasNoreaskArray.every(Boolean)).toBe(true);
        });
    });
});
