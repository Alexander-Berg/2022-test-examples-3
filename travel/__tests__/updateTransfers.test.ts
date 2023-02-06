import ITransfer from '../../../interfaces/transfer/ITransfer';
import ISegment from '../../../interfaces/segment/ISegment';
import ISearchMeta from '../../../interfaces/state/search/ISearchMeta';

import updateTransfers from '../updateTransfers';
import updateTransfer from '../updateTransfer';
import patchSegments from '../patchSegments';

jest.mock('../updateTransfer', () => jest.fn(transferA => transferA));
jest.mock('../patchSegments', () => jest.fn(({segments}) => segments));

let idCounter = 1;

function getTransfer(id?: string): ITransfer {
    id = id || `${idCounter++}`;

    return {
        id,
        isTransfer: true,
    } as ITransfer;
}

function getSegment(): ISegment {
    return {} as ISegment;
}

const meta = {} as ISearchMeta;

describe('updateTransfers', () => {
    it('Если новых пересадок нет, то вернет те же сегенты, что были переданы', () => {
        const baseSegments = [getSegment(), getSegment(), getTransfer()];

        const transfers = [];

        const result = updateTransfers(baseSegments, transfers, meta);

        expect(result.length).toBe(3);
        expect(result[0]).toBe(baseSegments[0]);
        expect(result[1]).toBe(baseSegments[1]);
        expect(result[2]).toBe(baseSegments[2]);
        expect(updateTransfer).not.toBeCalled();
        expect(patchSegments).toBeCalledWith(
            expect.objectContaining({segments: []}),
        );
    });

    it('Если нет необходимости обновлять пересадки, то вернет тот же массив, что был передан', () => {
        const baseSegments = [getSegment(), getSegment(), getTransfer('1')];

        const transfers = [getTransfer('1')];

        const result = updateTransfers(baseSegments, transfers, meta);

        expect(result.length).toBe(3);
        expect(result[0]).toBe(baseSegments[0]);
        expect(result[1]).toBe(baseSegments[1]);
        expect(result[2]).toBe(baseSegments[2]);
        expect(updateTransfer).toBeCalledWith(
            baseSegments[2],
            transfers[0],
            meta,
        );
        expect(patchSegments).toBeCalledWith(
            expect.objectContaining({segments: []}),
        );
    });

    it('Если были переданы новые пересадки, то они должны быть добавлены в массив сегментов', () => {
        const baseSegments = [getSegment(), getSegment(), getTransfer('1')];

        const transfers = [getTransfer('2')];

        const result = updateTransfers(baseSegments, transfers, meta);

        expect(result.length).toBe(4);
        expect(result[0]).toBe(baseSegments[0]);
        expect(result[1]).toBe(baseSegments[1]);
        expect(result[2]).toBe(baseSegments[2]);
        expect(result[3]).toBe(transfers[0]);
        expect(updateTransfer).not.toBeCalled();
        expect(patchSegments).toBeCalledWith(
            expect.objectContaining({segments: [transfers[0]]}),
        );
    });

    it('При передаче новых пересадок, должны быть обновлены имеющиеся и добавлены новые', () => {
        const updatedTransfer = getTransfer('1');

        (updateTransfer as jest.Mock).mockReturnValueOnce(updatedTransfer);

        const baseSegments = [getSegment(), getTransfer('1'), getSegment()];

        const transfers = [getTransfer('2'), getTransfer('1')];

        const result = updateTransfers(baseSegments, transfers, meta);

        expect(result.length).toBe(4);
        expect(result[0]).toBe(baseSegments[0]);
        expect(result[1]).toBe(updatedTransfer);
        expect(result[2]).toBe(baseSegments[2]);
        expect(result[3]).toBe(transfers[0]);
        expect(updateTransfer).toBeCalledWith(
            baseSegments[1],
            transfers[1],
            meta,
        );
        expect(patchSegments).toBeCalledWith(
            expect.objectContaining({segments: [transfers[0]]}),
        );
    });
});
