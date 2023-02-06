'use strict';

jest.mock('../../../lib/method/base-method.js');
const mockSuper = require('../../../lib/method/base-method.js').prototype;
const TrashMessages = require('./trash-messages.js');

let trashMessages;

beforeEach(() => {
    trashMessages = new TrashMessages();
    mockSuper.fetch.mockReturnValue(Promise.resolve('foo'));
});

describe('when deleting messages by ids', () => {
    it('deletes messages', () => {
        return trashMessages.fetch(1, 2).then((result) => {
            expect(result).toBe('foo');
            expect(mockSuper.fetch).toBeCalledWith(1, 2);
        });
    });
});

describe('when deleting newsletters', () => {
    it('deletes messages', () => {
        const params = { search: 'foo@example.com', messageType: 123 };
        mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({ id: '456' }));
        mockSuper.calls.exec.mockReturnValue(Promise.resolve());

        return trashMessages.fetch(1, params).then((result) => {
            expect(result).toEqual({});
            expect(mockSuper.calls.exec.mock.calls).toEqual([
                [ 'createFilter', 1, params ],
                [ 'applyFilter', 1, { filterId: '456' } ],
                [ 'deleteFilter', 1, { filterId: '456' } ]
            ]);
        });
    });

    it('handles filter creation error', () => {
        const params = { search: 'foo@example.com', messageType: 123 };
        mockSuper.calls.exec.mockReturnValueOnce(Promise.reject('bar'));

        return trashMessages.fetch(1, params).catch((error) => {
            expect(error).toBe('bar');
            expect(mockSuper.calls.exec.mock.calls).toEqual([
                [ 'createFilter', 1, params ]
            ]);
        });
    });

    it('handles filter application error', () => {
        const params = { search: 'foo@example.com', messageType: 123 };
        mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({ id: '456' }));
        mockSuper.calls.exec.mockReturnValueOnce(Promise.reject('bar'));
        mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve());

        return trashMessages.fetch(1, params).catch((error) => {
            expect(error).toBe('bar');
            expect(mockSuper.calls.exec.mock.calls).toEqual([
                [ 'createFilter', 1, params ],
                [ 'applyFilter', 1, { filterId: '456' } ],
                [ 'deleteFilter', 1, { filterId: '456' } ]
            ]);
        });
    });

    it('handles filter deletion error', () => {
        const params = { search: 'foo@example.com', messageType: 123 };
        mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve({ id: '456' }));
        mockSuper.calls.exec.mockReturnValueOnce(Promise.resolve());
        mockSuper.calls.exec.mockReturnValueOnce(Promise.reject('bar'));

        return trashMessages.fetch(1, params).catch((error) => {
            expect(error).toEqual('bar');
            expect(mockSuper.calls.exec.mock.calls).toEqual([
                [ 'createFilter', 1, params ],
                [ 'applyFilter', 1, { filterId: '456' } ],
                [ 'deleteFilter', 1, { filterId: '456' } ]
            ]);
        });
    });
});
