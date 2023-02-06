import ITransfer from '../../../interfaces/transfer/ITransfer';
import ISearchMeta from '../../../interfaces/state/search/ISearchMeta';

import updateTransfer from '../updateTransfer';
import mergeTransfers from '../../search/mergeTransfers';
import patchSegments from '../patchSegments';

jest.mock('../../search/mergeTransfers', () => jest.fn());
jest.mock('../patchSegments', () =>
    jest.fn(({segments: [transfer]}) => [transfer]),
);

const transferA = {
    isTransfer: true,
} as ITransfer;
const transferB = {
    isTransfer: true,
} as ITransfer;
const mergedTransfer = {
    isTransfer: true,
} as ITransfer;
const meta = {} as ISearchMeta;

describe('updateTransfer', () => {
    it('В случае, если пересадка не нуждается в обновлении, то вернет оригинальную пересадку', () => {
        (mergeTransfers as jest.Mock).mockReturnValueOnce(transferA);

        const transfer = updateTransfer(transferA, transferB, meta);

        expect(transfer).toBe(transferA);
        expect(mergeTransfers).toBeCalledWith(transferA, transferB);
        expect(patchSegments).not.toBeCalled();
    });

    it('В случае, если пересадка была обновлена, то убедимся, что будет вызван патч сегмента', () => {
        (mergeTransfers as jest.Mock).mockReturnValueOnce(mergedTransfer);

        const transfer = updateTransfer(transferA, transferB, meta);

        expect(mergeTransfers).toBeCalledWith(transferA, transferB);
        expect(patchSegments).toBeCalledWith(
            expect.objectContaining({
                segments: [transfer],
                meta,
            }),
        );
    });
});
