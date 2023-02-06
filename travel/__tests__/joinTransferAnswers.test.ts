import IApiTransferResponce from '../../../interfaces/api/IApiTransferResponse';
import TransferQueryStatus from '../../../interfaces/transfer/TransferQueryStatus';
import {FilterTransportType} from '../../transportType';

import joinTransferAnswers from '../joinTransferAnswers';
import mergeTransfers from '../mergeTransfers';

jest.mock('../mergeTransfers', () => jest.fn(transferA => transferA));

describe('joinTransferAnswers', () => {
    it('В случае отсутствия ответов API от пересадок, вернет объект с "пустыми" свойствами', () => {
        expect(joinTransferAnswers([{someAnswer: {}}])).toEqual({
            transfers: [],
            status: {},
            transportTypes: [],
        });
    });

    it('В случае присутствия одного ответа, вернет его содержимое', () => {
        const transfers = [{id: '1'}];
        const transferAnswer = {
            transfers,
            status: TransferQueryStatus.done,
            transportType: FilterTransportType.all,
        } as IApiTransferResponce;

        expect(joinTransferAnswers([transferAnswer, {someAnswer: {}}])).toEqual(
            {
                transfers,
                status: {
                    [transferAnswer.transportType]: transferAnswer.status,
                },
                transportTypes: [transferAnswer.transportType],
            },
        );
    });

    it('Объединение двух ответов с различными id пересадок', () => {
        const transfers1 = [{id: '1'}];
        const transfers2 = [{id: '2'}];
        const transferAnswer1 = {
            transfers: transfers1,
            status: TransferQueryStatus.done,
            transportType: FilterTransportType.train,
        } as IApiTransferResponce;
        const transferAnswer2 = {
            transfers: transfers2,
            status: TransferQueryStatus.querying,
            transportType: FilterTransportType.plane,
        } as IApiTransferResponce;

        expect(joinTransferAnswers([transferAnswer1, transferAnswer2])).toEqual(
            {
                transfers: [...transfers1, ...transfers2],
                status: {
                    [transferAnswer1.transportType]: transferAnswer1.status,
                    [transferAnswer2.transportType]: transferAnswer2.status,
                },
                transportTypes: [
                    transferAnswer1.transportType,
                    transferAnswer2.transportType,
                ],
            },
        );
    });

    it('Объединение двух ответов, включающих идентичные id пересадок', () => {
        const transfers1 = [
            {id: '1'},
            {
                id: '2',
                segments: [{tariffs: {}}],
            },
        ];
        const transfers2 = [
            {
                id: '2',
                segments: [{}],
            },
            {id: '3'},
        ];
        const transferAnswer1 = {
            transfers: transfers1,
            status: TransferQueryStatus.done,
            transportType: FilterTransportType.all,
        } as IApiTransferResponce;
        const transferAnswer2 = {
            transfers: transfers2,
            status: TransferQueryStatus.querying,
            transportType: FilterTransportType.plane,
        } as IApiTransferResponce;

        expect(joinTransferAnswers([transferAnswer1, transferAnswer2])).toEqual(
            {
                transfers: [transfers1[0], transfers1[1], transfers2[1]],
                status: {
                    [transferAnswer1.transportType]: transferAnswer1.status,
                    [transferAnswer2.transportType]: transferAnswer2.status,
                },
                transportTypes: [
                    transferAnswer1.transportType,
                    transferAnswer2.transportType,
                ],
            },
        );
        expect(mergeTransfers).toBeCalledWith(transfers1[1], transfers2[0]);
    });
});
