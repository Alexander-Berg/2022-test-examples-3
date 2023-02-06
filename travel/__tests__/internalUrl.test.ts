import {TLD} from 'constants/tld';

import {internalUrl} from 'utilities/url';

describe('internalUrl', () => {
    test('Должен вернуть корневой url', () => {
        expect(internalUrl('/')).toBe('/');
    });

    test('Должен вернуть корневой url для пустой строки', () => {
        expect(internalUrl('')).toBe('/');
    });

    test('Должен вернуть /path/', () => {
        expect(internalUrl('/path/')).toBe('/path/');
    });

    test('Должен добавить слеш `/` в конце пути', () => {
        expect(internalUrl('/path')).toBe('/path/');
    });

    test('Должен добавить слеш `/` в начали пути', () => {
        expect(internalUrl('path/')).toBe('/path/');
    });

    test('Должен вернуть /path/?foo=bar', () => {
        expect(internalUrl('/path', {foo: 'bar'})).toBe('/path/?foo=bar');
    });

    test('Должен вернуть /path/?foo для foo в значении null', () => {
        expect(internalUrl('/path', {foo: null})).toBe('/path/?foo');
    });

    test('Должен вернуть /path/ для foo в значении null', () => {
        expect(internalUrl('/path', {foo: null}, {filterNull: true})).toBe(
            '/path/',
        );
    });

    test('Должен вернуть путь с origin', () => {
        expect(
            internalUrl('/path/', null, {withOrigin: true, tld: TLD.RU}),
        ).toBe('https://travel.yandex.ru/path/');
    });
});
