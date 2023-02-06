import { ErrorType } from '@mssngr/calls';
import { OrdererQueue } from '../OrdererQueue';

describe('OrdererQueue', () => {
    const call = {
        guid: 'call-guid',
        chatId: 'call-chatId',
        deviceId: 'call-deviceId',
        deviceInfo: {},
        videoEnabled: true,
        audioEnabled: true,
    };

    let handlerMessage;
    let sequence: number[];

    beforeEach(() => {
        sequence = [];
        // @ts-ignore Fanout
        const handleCallingMessage = jest.fn((message: Fanout.CallingMessage) => {
            if (typeof message.SeqNo !== 'undefined') {
                sequence.push(message.SeqNo);
            }
        });

        handlerMessage = new OrdererQueue((_: ErrorType) => {}, handleCallingMessage);
    });

    const sendMessage = (SeqNo: number) => {
        handlerMessage.add({
            ...call,
            SeqNo,
        });
    };

    it('Return true for correct SeqNo', () => {
        sendMessage(1);
        sendMessage(2);
        sendMessage(3);
        expect(sequence).toEqual([1, 2, 3]);
    });

    it('Return true for not correct SeqNo', () => {
        sendMessage(1);
        sendMessage(3);
        sendMessage(2);
        sendMessage(4);
        expect(sequence).toEqual([1, 2, 3, 4]);
    });

    it('Return true for not correct first SeqNo', () => {
        sendMessage(2);
        sendMessage(3);
        sendMessage(1);
        sendMessage(4);
        expect(sequence).toEqual([1, 2, 3, 4]);
    });

    it('Return true for not correct big SeqNo', () => {
        sendMessage(1);
        sendMessage(4);
        sendMessage(3);
        sendMessage(2);
        sendMessage(5);
        sendMessage(6);

        expect(sequence).toEqual([1, 2, 3, 4, 5, 6]);
    });

    it('Return true for correct SeqNo with 0', () => {
        sendMessage(1);
        sendMessage(2);
        sendMessage(0);
        sendMessage(3);

        expect(sequence).toEqual([1, 2, 0, 3]);
    });

    it('Return true for not correct2 SeqNo with 0', () => {
        sendMessage(1);
        sendMessage(3);
        sendMessage(0);
        sendMessage(2);

        expect(sequence).toEqual([1, 0, 2, 3]);
    });

    it('Return true for skipped and not correct SeqNo', () => {
        jest.useFakeTimers();
        sendMessage(2);
        sendMessage(4);
        sendMessage(1);
        sendMessage(5);

        jest.runTimersToTime(2000);

        expect(sequence).toEqual([1, 2, 4, 5]);
        jest.useRealTimers();
    });

    it('Return true for skipped SeqNo more 2', () => {
        jest.useFakeTimers();
        sendMessage(1);
        sendMessage(2);
        sendMessage(6);

        jest.runTimersToTime(4000);

        expect(sequence).toEqual([1, 2]);
        jest.useRealTimers();
    });

    it('Return true for retry SeqNo', () => {
        sendMessage(1);
        sendMessage(2);
        sendMessage(3);
        sendMessage(3);
        sendMessage(4);

        expect(sequence).toEqual([1, 2, 3, 4]);
    });

    it('Return true for double not correct SeqNo', () => {
        sendMessage(1);
        sendMessage(3);
        sendMessage(4);
        sendMessage(2);
        sendMessage(5);
        sendMessage(8);
        sendMessage(6);
        sendMessage(7);
        sendMessage(9);

        expect(sequence).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9]);
    });
});
