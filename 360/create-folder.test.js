'use strict';

jest.mock('../../../i18n');
jest.mock('../../../lib/error/api-error.js');
jest.mock('../../../lib/method/base-method.js');

const mockI18n = require('../../../i18n');
const mockSuper = require('../../../lib/method/base-method.js').prototype;
const CreateFolder = require('./create-folder.js');

let createFolder;

beforeEach(() => {
    createFolder = new CreateFolder();
    mockSuper.fetch.mockReturnValue(Promise.resolve('foo'));
});

describe('for user folder', () => {
    it('creates folder', () => {
        return createFolder.fetch(1, 2).then((result) => {
            expect(result).toBe('foo');
            expect(mockSuper.fetch).toBeCalledWith(1, 2);
        });
    });
});

describe('for system folder', () => {
    const ctx = { headers: { 'x-lang': 'ru' } };
    const params = { type: 'system', symbol: 'alpha' };

    describe('with `pickFreeName` strategy', () => {
        beforeEach(() => {
            mockSuper._config.get.mockReturnValue({
                method: '_pickFreeName',
                names: [ 'Alpha Centauri', 'Beta', 'Gamma' ]
            });
        });

        it('picks first free name', () => {
            const promise = Promise.resolve({ folders: [] });
            mockSuper.calls.exec.mockReturnValueOnce(promise);

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toBe('foo');
                expect(mockSuper.fetch).toBeCalledWith(ctx, {
                    name: 'Alpha Centauri',
                    symbol: 'alpha',
                    parentId: null
                });
            });
        });

        it('falls back to alternative name', () => {
            mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
                folders: [
                    { name: 'Alpha Centauri', parentId: null },
                    { name: 'Beta', parentId: null }
                ]
            }));

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toBe('foo');
                expect(mockSuper.fetch).toBeCalledWith(ctx, {
                    name: 'Gamma',
                    symbol: 'alpha',
                    parentId: null
                });
            });
        });

        it('falls back for no-brake spaces', () => {
            mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
                folders: [
                    { name: 'Alpha\u00a0Centauri', parentId: null },
                    { name: 'Beta', parentId: null }
                ]
            }));

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toBe('foo');
                expect(mockSuper.fetch).toBeCalledWith(ctx, {
                    name: 'Gamma',
                    symbol: 'alpha',
                    parentId: null
                });
            });
        });

        it('falls back to counter', () => {
            mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
                folders: [
                    { name: 'Alpha Centauri', parentId: null },
                    { name: 'Beta', parentId: null },
                    { name: 'Gamma', parentId: null },
                    { name: 'Alpha Centauri (1)', parentId: null }
                ]
            }));

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toBe('foo');
                expect(mockSuper.fetch).toBeCalledWith(ctx, {
                    name: 'Alpha Centauri (2)',
                    symbol: 'alpha',
                    parentId: null
                });
            });
        });
    });

    describe('with `reuseUserFolder` strategy', () => {
        beforeEach(() => {
            mockSuper._config.get.mockReturnValue({
                method: '_reuseUserFolder',
                name: 'Alpha'
            });
            mockSuper.calls.exec.mockReturnValue(Promise.resolve());
        });

        it('creates system folder', () => {
            const promise = Promise.resolve({ folders: [] });
            mockSuper.calls.exec.mockReturnValueOnce(promise);

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toBe('foo');
                expect(mockSuper.fetch).toBeCalledWith(ctx, {
                    name: 'Alpha',
                    symbol: 'alpha',
                    parentId: null
                });
            });
        });

        it('handles getFolders error', () => {
            mockSuper.calls.exec.mockReturnValueOnce(Promise.reject());

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toBe('foo');
                expect(mockSuper.fetch).toBeCalledWith(ctx, {
                    name: 'Alpha',
                    symbol: 'alpha',
                    parentId: null
                });
            });
        });

        it('converts namesake user folder to system one', () => {
            mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
                folders: [
                    { id: '123', name: 'Alpha', parentId: null }
                ]
            }));

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toEqual({ fid: '123' });
                expect(mockSuper.calls.exec).toBeCalledWith('setSymbol', ctx, {
                    folderId: '123',
                    symbol: 'alpha'
                });
            });
        });

        it('converts i18n-namesake user folder to system one', () => {
            mockI18n.get.mockReturnValue('Альфа');
            mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
                folders: [
                    { id: '123', name: 'Альфа', parentId: null }
                ]
            }));

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toEqual({ fid: '123' });
                expect(mockI18n.get).toBeCalledWith('ru', 'folders', 'alpha');
                expect(mockSuper.calls.exec).toBeCalledWith('updateFolder', ctx, {
                    folderId: '123',
                    name: 'Alpha'
                });
                expect(mockSuper.calls.exec).toBeCalledWith('setSymbol', ctx, {
                    folderId: '123',
                    symbol: 'alpha'
                });
            });
        });
    });

    describe('with `reuseUserFolder` strategy + parent', () => {
        beforeEach(() => {
            mockSuper._config.get.mockReturnValue({
                method: '_reuseUserFolder',
                name: 'Alpha',
                parentSymbol: 'beta'
            });
            mockSuper.calls.exec.mockReturnValue(Promise.resolve());
        });

        it('creates nested system folder', () => {
            mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
                folders: [
                    { id: '123', name: 'Beta', parentId: null, symbol: 'beta' }
                ]
            }));

            return createFolder.fetch(ctx, params).then((result) => {
                expect(result).toBe('foo');
                expect(mockSuper.fetch).toBeCalledWith(ctx, {
                    name: 'Alpha',
                    symbol: 'alpha',
                    parentId: '123'
                });
            });
        });

        it('handlers getFolders error for nested folder', () => {
            mockSuper.calls.exec.mockReturnValueOnce(Promise.reject());

            return createFolder.fetch(ctx, params).catch((error) => {
                expect(error.message).toBe('Parent folder \'beta\' not found');
            });
        });
    });
});
