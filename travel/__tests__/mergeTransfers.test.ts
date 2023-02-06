import ITransferFromApi from '../../../interfaces/transfer/ITransferFromApi';

import mergeTransfers, {ErrorMergeTransfer} from '../mergeTransfers';

describe('mergeTransfers', () => {
    it('Если id пересадок не совпадают, то бросит exception', () => {
        expect(() =>
            mergeTransfers(
                {id: '1'} as ITransferFromApi,
                {id: '2'} as ITransferFromApi,
            ),
        ).toThrow(ErrorMergeTransfer);
    });

    it('Если у пересадок с идентичным id разные id сегментов, то бросит exception', () => {
        expect(() =>
            mergeTransfers(
                {
                    id: '1',
                    segments: [{id: '1'}, {id: '2'}],
                } as ITransferFromApi,
                {
                    id: '1',
                    segments: [{id: '1'}, {id: '3'}],
                } as ITransferFromApi,
            ),
        ).toThrow(ErrorMergeTransfer);
    });

    it('Если у сегмента пересадки отсутствует id, то бросит exception', () => {
        expect(() =>
            mergeTransfers(
                {
                    id: '1',
                    segments: [{id: '1'}, {id: '2'}],
                } as ITransferFromApi,
                {
                    id: '1',
                    segments: [{id: '1'}, {}],
                } as ITransferFromApi,
            ),
        ).toThrow(ErrorMergeTransfer);
    });

    it('Если у пересадок с идентичным id разное количество сегментов, то бросит exception', () => {
        expect(() =>
            mergeTransfers(
                {
                    id: '1',
                    segments: [{id: '1'}, {id: '2'}],
                } as ITransferFromApi,
                {
                    id: '1',
                    segments: [{id: '1'}, {id: '2'}, {id: '3'}],
                } as ITransferFromApi,
            ),
        ).toThrow(ErrorMergeTransfer);
    });

    it('В случае идентичных пересадок, вернет первую', () => {
        const transferA = {
            id: '1',
            segments: [{id: '1'}, {id: '2', tariffs: {}}],
        } as ITransferFromApi;
        const transferB = {
            id: '1',
            segments: [{id: '1'}, {id: '2', tariffs: {}}],
        } as ITransferFromApi;

        const transfer = mergeTransfers(transferA, transferB);

        expect(transfer).toBe(transferA);
        expect(transfer.segments).toBe(transferA.segments);
        expect(transfer.segments.length).toBe(transferA.segments.length);
        expect(transfer.segments[0]).toBe(transferA.segments[0]);
        expect(transfer.segments[1]).toBe(transferA.segments[1]);
        expect(transfer.segments[1].tariffs).toBe(
            transferA.segments[1].tariffs,
        );
    });

    it(`Если во второй пересадке есть тарифы, которых нет в первой пересадке, то 
    вернет новую пресадку, в которой будут сегменты с тарифами из обоих пересадок`, () => {
        const transferA = {
            id: '1',
            segments: [{id: '1', tariffs: {}}, {id: '2'}, {id: '3'}],
        } as ITransferFromApi;

        const transferB = {
            id: '1',
            segments: [{id: '1'}, {id: '2', tariffs: {}}, {id: '3'}],
        } as ITransferFromApi;

        const transfer = mergeTransfers(transferA, transferB);

        expect(transfer).not.toBe(transferA);
        expect(transfer).not.toBe(transferB);
        expect(transfer.segments).not.toBe(transferA.segments);
        expect(transfer.segments).not.toBe(transferB.segments);
        expect(transfer.segments[0]).toBe(transferA.segments[0]);
        expect(transfer.segments[1]).toBe(transferB.segments[1]);
        expect(transfer.segments[2]).toBe(transferA.segments[2]);
    });

    it(`Если в первой пересадке тарифов нет, а во второй есть, то вернет новую пересадку с тарифами.
    При этом будут смержены тарифы сегментов по существующим правилам.`, () => {
        const transferA = {
            id: '1',
            segments: [{id: '1'}],
        } as ITransferFromApi;

        const transferB = {
            id: '1',
            segments: [{id: '1', tariffs: {}}],
            tariffs: {},
        } as ITransferFromApi;

        const transfer = mergeTransfers(transferA, transferB);

        expect(transfer).not.toBe(transferA);
        expect(transfer).not.toBe(transferB);
        expect(transfer.segments).not.toBe(transferA.segments);
        expect(transfer.segments).not.toBe(transferB.segments);
        expect(transfer.tariffs).toBe(transferB.tariffs);
        expect(transfer.segments.length).toBe(1);
        expect(transfer.segments[0]).toBe(transferB.segments[0]);
    });

    it(`Если в первой пересадке есть тарифы, а во второй нет, при этом во второй пересадке 
    есть новые тарифы в сегментах, то вернет новую пересадку в которой сохранится тариф на
    пересадку (интерлайн) и смержатся тарифы сегментов`, () => {
        const transferA = {
            id: '1',
            segments: [{id: '1'}],
            tariffs: {},
        } as ITransferFromApi;

        const transferB = {
            id: '1',
            segments: [{id: '1', tariffs: {}}],
        } as ITransferFromApi;

        const transfer = mergeTransfers(transferA, transferB);

        expect(transfer).not.toBe(transferA);
        expect(transfer).not.toBe(transferB);
        expect(transfer.segments).not.toBe(transferA.segments);
        expect(transfer.segments).not.toBe(transferB.segments);
        expect(transfer.tariffs).toBe(transferA.tariffs);
        expect(transfer.segments.length).toBe(1);
        expect(transfer.segments[0]).toBe(transferB.segments[0]);
    });

    it(`Если и в первой и во второй пересадке идентичны тарифы пересадки и ее сегментов, 
    то вернет первую пересадку`, () => {
        const transferA = {
            id: '1',
            segments: [{id: '1', tariffs: {}}],
            tariffs: {},
        } as ITransferFromApi;

        const transferB = {
            id: '1',
            segments: [{id: '1', tariffs: {}}],
            tariffs: {},
        } as ITransferFromApi;

        const transfer = mergeTransfers(transferA, transferB);

        expect(transfer).toBe(transferA);
        expect(transfer.segments).toBe(transferA.segments);
        expect(transfer.tariffs).toBe(transferA.tariffs);
        expect(transfer.segments.length).toBe(1);
    });
});
