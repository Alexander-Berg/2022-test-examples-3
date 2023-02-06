'use strict';

jest.mock('../../../lib/method/base-method.js');

const mockSuper = require('../../../lib/method/base-method.js').prototype;
const UpdateFolder = require('./update-folder.js');

let updateFolder;

beforeEach(() => {
    updateFolder = new UpdateFolder();
    mockSuper.fetch.mockReturnValue(Promise.resolve());
    mockSuper.calls.exec.mockReturnValue(Promise.resolve());
});

test('renames folder', () => {
    const params = { folderId: '2', name: 'foo' };

    return updateFolder.fetch(1, params).then((result) => {
        expect(mockSuper.fetch).toBeCalledWith(1, params);
        expect(result).toEqual({});
    });
});

test('moves folder to other parent', () => {
    const params = { folderId: '2', parentId: '3' };

    return updateFolder.fetch(1, params).then((result) => {
        expect(mockSuper.fetch).toBeCalledWith(1, params);
        expect(result).toEqual({});
    });
});

test('moves folder to the root', () => {
    const params = { folderId: '2', parentId: null };

    return updateFolder.fetch(1, params).then((result) => {
        expect(mockSuper.fetch).toBeCalledWith(1, params);
        expect(result).toEqual({});
    });
});

test('updates folder position', () => {
    const params = { folderId: '2', previousFolderId: '4' };

    return updateFolder.fetch(1, params).then((result) => {
        expect(mockSuper.calls.exec).toBeCalledWith('setPreviousFolder', 1, params);
        expect(result).toEqual({});
    });
});

test('moves folder to the top', () => {
    const params = { folderId: '2', previousFolderId: null };

    return updateFolder.fetch(1, params).then((result) => {
        expect(mockSuper.calls.exec).toBeCalledWith('setPreviousFolder', 1, params);
        expect(result).toEqual({});
    });
});

test('marks folder as visited', () => {
    const params = { folderId: '2', isVisited: true };

    return updateFolder.fetch(1, params).then((result) => {
        expect(mockSuper.calls.exec).toBeCalledWith('markAsVisited', 1, params);
        expect(result).toEqual({});
    });
});

test('supports bulk operations', () => {
    const params = {
        folderId: '2',
        name: 'foo',
        parentId: '3',
        previousFolderId: '4',
        isVisited: true
    };

    return updateFolder.fetch(1, params).then((result) => {
        expect(mockSuper.fetch).toBeCalledWith(1, params);
        expect(mockSuper.calls.exec).toBeCalledWith('setPreviousFolder', 1, params);
        expect(mockSuper.calls.exec).toBeCalledWith('markAsVisited', 1, params);
        expect(result).toEqual({});
    });
});

test('fails on any error', () => {
    const params = {
        folderId: '2',
        name: 'foo',
        parentId: '3',
        previousFolderId: '4',
        isVisited: true
    };
    mockSuper.calls.exec.mockReturnValueOnce(Promise.reject('bar'));

    return updateFolder.fetch(1, params).catch((error) => {
        expect(mockSuper.fetch).toBeCalledWith(1, params);
        expect(mockSuper.calls.exec).toBeCalledWith('setPreviousFolder', 1, params);
        expect(mockSuper.calls.exec).toBeCalledWith('markAsVisited', 1, params);
        expect(error).toEqual('bar');
    });
});
