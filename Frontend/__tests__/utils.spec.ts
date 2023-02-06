import { combineMatchers, getOriginUrl } from '../utils';

describe('turbojson parser utils', () => {
    describe('combineMatchers', () => {
        const matcherFunc1 = () => {};
        const matcherFunc2 = () => {};
        const matcherFunc3 = () => {};

        it('combineMatchers() корректно объединяет единичные матчеры одного блока', () => {
            const baseMatchers = {
                block1: matcherFunc1,
            };
            const matcherToAdd = {
                block1: matcherFunc2,
            };
            const expectedMatchers = {
                block1: [matcherFunc1, matcherFunc2],
            };

            expect(combineMatchers(baseMatchers, matcherToAdd)).toEqual(expectedMatchers);
        });

        it('combineMatchers() корректно объединяет массивы матчеров одного блока', () => {
            const baseMatchers = {
                block1: [matcherFunc1],
            };
            const matcherToAdd = {
                block1: [matcherFunc2, matcherFunc3],
            };
            const expectedMatchers = {
                block1: [matcherFunc1, matcherFunc2, matcherFunc3],
            };

            expect(combineMatchers(baseMatchers, matcherToAdd)).toEqual(expectedMatchers);
        });

        it('combineMatchers() корректно объединяет матчеры разных блоков', () => {
            const baseMatchers = {
                block1: [matcherFunc1],
            };
            const matcherToAdd = {
                block1: matcherFunc2,
                block2: matcherFunc3,
            };
            const expectedMatchers = {
                block1: [matcherFunc1, matcherFunc2],
                block2: [matcherFunc3],
            };

            expect(combineMatchers(baseMatchers, matcherToAdd)).toEqual(expectedMatchers);
        });
    });

    describe('getOriginUrl', () => {
        it('getOriginUrl() корректно возвращает origin', () => {
            const data = {
                'https://spideradio.github.io/?rnd=2lum7hf3': 'https://spideradio.github.io',
                'https://spideradio.github.io/index.html': 'https://spideradio.github.io',
                'https://spideradio.github.io': 'https://spideradio.github.io',
                'https://spideradio.github.io/': 'https://spideradio.github.io',
                'https://spideradio.github.io/data/product/2': 'https://spideradio.github.io',
                'http://spideradio.github.io/data/product/2': 'http://spideradio.github.io',
            };
            Object.keys(data).forEach(url => {
                expect(getOriginUrl(url)).toEqual(data[url]);
            });
        });
    });
});
