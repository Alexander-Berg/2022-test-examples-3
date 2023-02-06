import { collectFileName } from './get-static-info';

describe('middlewares/get-query-string-from-request', () => {
    it('collectFileName: Должны корректно формироваться пути без параметров', () => {
        expect(collectFileName(
            'file.js',
        )).toEqual('file.js');
    });

    it('collectFileName: Должны корректно формироваться пути (с языком)', () => {
        expect(collectFileName(
            'file.js',
            'language',
        )).toEqual('file.language.js');
    });

    it('collectFileName: Должны корректно формироваться URL (с языком)', () => {
        expect(collectFileName(
            'https://example.smth/smth.smth/smth/file.js',
            'language',
        )).toEqual('https://example.smth/smth.smth/smth/file.language.js');
    });
});
