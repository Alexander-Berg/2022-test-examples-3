import { IEcomDoc, IData } from '~/types';
import { parseMeta, parsePageMeta } from '../common';
import * as url from '../../utils/url';
import * as cgi from '../../lib/cgi';

jest.mock('../../server/config', () => {
    return {
        version: 'test',
    };
});

describe('turbojson parsers', () => {
    let data = {} as IData;
    let doc = {} as IEcomDoc;

    describe('parseMeta', () => {
        let normalizeUrl: ReturnType<typeof jest.spyOn>;
        let buildUserFriendlyUrl: ReturnType<typeof jest.spyOn>;

        beforeEach(() => {
            doc = {
                url: 'https://anyurl.ru/blabla',
                merged_host_data: {},
            } as IEcomDoc;

            data = {
                env: {},
                reqdata: {
                    unparsed_uri: 'https://yandex.ru/turbo/products/178726',
                },
            } as IData;

            normalizeUrl = jest.spyOn(url, 'normalizeUrl');
            normalizeUrl.mockReturnValue('https://ya.ru/normalizeUrl');
            buildUserFriendlyUrl = jest.spyOn(cgi, 'buildUserFriendlyUrl');
            buildUserFriendlyUrl.mockReturnValue('https://ya.ru/buildUserFriendlyUrl');
        });

        afterEach(() => {
            normalizeUrl.mockClear();
            buildUserFriendlyUrl.mockClear();
        });

        it('Передаем адрес хоста как originalUrl в meta', () => {
            const meta = parseMeta(doc, data);
            expect(meta.originalUrl).toEqual('https://anyurl.ru');
        });
    });

    describe('parsePageMeta', () => {
        it('Достаем из документа его оригинальный урл', () => {
            doc = {
                url: 'https://yandex.ru/blabla',
            } as IEcomDoc;
            const result = parsePageMeta(doc);
            expect(result.originalUrl).toEqual('https://yandex.ru/blabla');
        });
    });
});
