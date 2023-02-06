import TransferQueryStatus from '../../../../interfaces/transfer/TransferQueryStatus';

import setTransferQueryingPayloadCreator from '../setTransferQueryingPayloadCreator';

describe('setTransferQueryingPayloadCreator', () => {
    it('Для пустого объекта вернет пустой объект', () => {
        expect(setTransferQueryingPayloadCreator({})).toEqual({});
    });

    it(`Преобразует значения статусов опроса цен из объединения пересадочных запросов 
    в объект, который можно будет отправить в редьюсер`, () => {
        expect(
            setTransferQueryingPayloadCreator({
                all: TransferQueryStatus.done,
                train: TransferQueryStatus.error,
                plane: TransferQueryStatus.querying,
            }),
        ).toEqual({
            transferAll: false,
            transferTrain: false,
            transferPlane: true,
        });
    });
});
