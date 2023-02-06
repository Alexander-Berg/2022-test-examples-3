'use strict';

jest.mock('../../../lib/method/base-method.js');

const mockSuper = require('../../../lib/method/base-method.js').prototype;
const DeleteFolder = require('./delete-folder.js');

let deleteFolder;

beforeEach(() => {
    deleteFolder = new DeleteFolder();
    mockSuper.fetch.mockReturnValue(Promise.resolve('foo'));
    mockSuper._config.get.mockReturnValue([ 'bar', 'baz' ]);
});

test('deletes folder', () => {
    mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
        folders: [ { id: '2' } ]
    }));

    return deleteFolder.fetch(1, { folderId: '2' }).then((result) => {
        expect(mockSuper.calls.exec).toBeCalledWith('getFolders', 1);
        expect(mockSuper.fetch).toBeCalledWith(1, { folderId: '2' });
        expect(result).toBe('foo');
    });
});

test('unsets system folder symbol', () => {
    mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
        folders: [ { id: '2', symbol: 'baz' } ]
    }));
    mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve());

    return deleteFolder.fetch(1, { folderId: '2' }).then((result) => {
        expect(mockSuper.calls.exec).toBeCalledWith('getFolders', 1);
        expect(mockSuper.calls.exec).toBeCalledWith('unsetSymbol', 1, { folderId: '2' });
        expect(mockSuper.fetch).toBeCalledWith(1, { folderId: '2' });
        expect(result).toBe('foo');
    });
});

test('handles folder list error', () => {
    mockSuper.calls.exec.mockReturnValueOnce(Promise.reject());

    return deleteFolder.fetch(1, { folderId: '2' }).then((result) => {
        expect(mockSuper.calls.exec).toBeCalledWith('getFolders', 1);
        expect(mockSuper.fetch).toBeCalledWith(1, { folderId: '2' });
        expect(result).toBe('foo');
    });
});

test('handles inability to unset symbol', () => {
    mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({
        folders: [ { id: '2', symbol: 'baz' } ]
    }));
    mockSuper.calls.exec.mockReturnValueOnce(Promise.reject());

    return deleteFolder.fetch(1, { folderId: '2' }).then((result) => {
        expect(mockSuper.calls.exec).toBeCalledWith('getFolders', 1);
        expect(mockSuper.calls.exec).toBeCalledWith('unsetSymbol', 1, { folderId: '2' });
        expect(mockSuper.fetch).toBeCalledWith(1, { folderId: '2' });
        expect(result).toBe('foo');
    });
});
