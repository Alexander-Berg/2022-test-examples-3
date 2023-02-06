import {EDocumentType} from 'constants/document/documentTypes';

import {formatDocumentNumber} from 'utilities/documents/formatDocumentNumber';

describe('formatDocumentNumber', () => {
    describe('российский паспорт', () => {
        it('данные соответствуют документу - вернёт отформатированное значение', () => {
            expect(
                formatDocumentNumber(
                    '5577123456',
                    EDocumentType.RU_NATIONAL_PASSPORT,
                ),
            ).toEqual('5577 123456');
        });

        it('данные не соответствуют документу - вернёт исходное значение', () => {
            expect(
                formatDocumentNumber(
                    'FF123456',
                    EDocumentType.RU_NATIONAL_PASSPORT,
                ),
            ).toEqual('FF123456');
        });
    });

    describe('заграничный паспорт', () => {
        it('данные соответствуют документу - вернёт отформатированное значение', () => {
            expect(
                formatDocumentNumber(
                    '789123456',
                    EDocumentType.RU_FOREIGN_PASSPORT,
                ),
            ).toEqual('78 9123456');
        });

        it('данные не соответствуют документу - вернёт исходное значение', () => {
            expect(
                formatDocumentNumber(
                    'FF123456',
                    EDocumentType.RU_FOREIGN_PASSPORT,
                ),
            ).toEqual('FF123456');
        });
    });

    describe('свидетельство о рождении', () => {
        it('данные соответствуют документу - вернёт отформатированное значение', () => {
            expect(
                formatDocumentNumber(
                    'XXАА123456',
                    EDocumentType.RU_BIRTH_CERTIFICATE,
                ),
            ).toEqual('XX-АА №123456');
            expect(
                formatDocumentNumber(
                    'XIVББ654321',
                    EDocumentType.RU_BIRTH_CERTIFICATE,
                ),
            ).toEqual('XIV-ББ №654321');
            expect(
                formatDocumentNumber(
                    'XXXIIIВВ123123',
                    EDocumentType.RU_BIRTH_CERTIFICATE,
                ),
            ).toEqual('XXXIII-ВВ №123123');
        });

        it('данные не соответствуют документу - вернёт исходное значение', () => {
            expect(
                formatDocumentNumber(
                    '5577123456',
                    EDocumentType.RU_BIRTH_CERTIFICATE,
                ),
            ).toEqual('5577123456');
        });
    });

    describe('паспорт моряка', () => {
        it('данные соответствуют документу - вернёт отформатированное значение', () => {
            expect(
                formatDocumentNumber(
                    '077712345',
                    EDocumentType.RU_SEAMAN_PASSPORT,
                ),
            ).toEqual('07 7712345');
            expect(
                formatDocumentNumber(
                    'МФ7712345',
                    EDocumentType.RU_SEAMAN_PASSPORT,
                ),
            ).toEqual('МФ 7712345');
            expect(
                formatDocumentNumber(
                    'RT7712345',
                    EDocumentType.RU_SEAMAN_PASSPORT,
                ),
            ).toEqual('RT 7712345');
        });

        it('данные не соответствуют документу - вернёт исходное значение', () => {
            expect(
                formatDocumentNumber(
                    '1234567YY',
                    EDocumentType.RU_SEAMAN_PASSPORT,
                ),
            ).toEqual('1234567YY');
        });
    });

    describe('иной документ', () => {
        it('вернёт исходную строку, т.к. отсутствуют данные для форматирования', () => {
            expect(
                formatDocumentNumber('АА123456', EDocumentType.RU_MILITARY_ID),
            ).toEqual('АА123456');
        });
    });
});
