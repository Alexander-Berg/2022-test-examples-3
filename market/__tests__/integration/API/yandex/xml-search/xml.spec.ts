import nock from 'nock';

import API from '../../../../../src/API/index';
import raiseMocks from '../../../helpers/raise-mocks';

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const xmlOk = require('./__mocks__/xml-search-ok');
const xmlFail = require('./__mocks__/xml-search-fail');

describe('XML API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation', async () => {
        const mockScopes = raiseMocks(xmlOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.images['v1.1.0'].xml({});
        } catch (error) {
            errorCatched = true;
        }

        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should find image by text', async () => {
        const mockScopes = raiseMocks(xmlOk);
        const response = await API.yandex.images['v1.1.0'].xml({ text: 'Бумага и бумажные изделия' });

        expect(response).toEqual('https://s-trade54.ru/sites/default/files/img-news/123_0.jpg');
        mockScopes.forEach((scope) => scope.done());
    });
    test('should fail', async () => {
        const mockScopes = raiseMocks(xmlFail);
        const response = await API.yandex.images['v1.1.0'].xml({ text: 'Бумага и бумажные изделия' });

        expect(response).toEqual('');
        mockScopes.forEach((scope) => scope.done());
    });
});
