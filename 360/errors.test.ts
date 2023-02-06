import { errors } from '@duffman-int/core';
import { DirectoryError, isDirectoryHttpError } from './errors';

describe('DirectoryError', () => {
    it('httpError 422', () => {
        const res = DirectoryError.httpError({
            error: { code: 422 },
            body: {},
        } as any);

        expect(res).toBeInstanceOf(DirectoryError);
        expect(res.type).toEqual('INVALID_PARAMS');
    });

    it('httpError 403', () => {
        const res = DirectoryError.httpError({
            error: { code: 403 },
            body: {},
        } as any);

        expect(res).toBeInstanceOf(DirectoryError);
        expect(res.type).toEqual('PERMISSION_DENIED');
    });

    it('httpError 404', () => {
        const res = DirectoryError.httpError({
            error: { code: 404 },
            body: {},
        } as any);

        expect(res).toBeInstanceOf(DirectoryError);
        expect(res.type).toEqual('NOT_FOUND');
    });

    it('httpError 409', () => {
        const res = DirectoryError.httpError({
            error: { code: 409 },
            body: {},
        } as any);

        expect(res).toBeInstanceOf(DirectoryError);
        expect(res.type).toEqual('CONFLICT');
    });

    it('httpError other', () => {
        const res = DirectoryError.httpError({
            error: { code: 502 },
            body: {},
        } as any);

        expect(res).toBeInstanceOf(DirectoryError);
        expect(res.type).toEqual('SERVER_ERROR');
    });

    it('validateError coverage', () => {
        const res = DirectoryError.validateError('code', 'message', { test: 1 });

        expect(res).toBeInstanceOf(DirectoryError);
        expect(res.type).toEqual('INVALID_PARAMS');
        expect(res.error).toEqual({
            type: 'INVALID_PARAMS',
            code: 'code',
            message: 'message',
            params: { test: 1 },
        });
    });
});

describe('isDirectoryHttpError', () => {
    it('checks true', () => {
        expect(isDirectoryHttpError(new errors.HTTP_ERROR({} as any))).toBe(true);
    });

    it('checks false', () => {
        expect(isDirectoryHttpError(new errors.EXTERNAL_ERROR({} as any))).toBe(false);
    });
});
