import { validateFileId } from '../files';

describe('files', () => {
    it('#validateFileId', () => {
        expect(validateFileId('file/284080ec-20b4-4f41-a6e7-526c3fec6b7d')).toBeTruthy();
        expect(validateFileId('284080ec-20b4-4f41-a6e7-526c3fec6b7d')).toBeFalsy();
        expect(validateFileId('file/284080ec-20b4-4f41-a6e7-526c3fec6')).toBeFalsy();
        expect(validateFileId('file/284080ec-4f41-a6e7-526c3fec6b7d')).toBeFalsy();
        expect(validateFileId('file/284080c-20b4-4f41-a6e7-526c3fec6b7d')).toBeFalsy();
        expect(validateFileId('file/284080EC-20b4-4f41-a6e7-526c3fec6b7d')).toBeFalsy();
    });
});
