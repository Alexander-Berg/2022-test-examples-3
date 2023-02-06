import { Event } from '@yandex-int/messenger.utils';
import { TransportStatus } from '@yandex-int/messenger.websocket';

jest.mock('../transport', () => ({
    transport: {
        onStatusChanged: new Event(),
        isReady: true,
    },
}));

/* eslint-disable import/first */
import SubscriptionController from '../SubscriptionController';
import { transport } from '../transport';

describe('SubscriptionController', () => {
    it('subscribe/cancel', () => {
        jest.useFakeTimers();
        const controller = new SubscriptionController({ timeout: 1000 });
        const spySubscribe = jest.fn();
        const spyCancel = jest.fn();

        controller.onSubscribe.addListener(spySubscribe);
        controller.onCancel.addListener(spyCancel);

        controller.subscribe();

        expect(spySubscribe).toBeCalledTimes(1);

        jest.runTimersToTime(1000);

        expect(spySubscribe).toBeCalledTimes(2);

        controller.cancel();

        expect(spyCancel).toBeCalledTimes(1);

        jest.runTimersToTime(1000);

        expect(spySubscribe).toBeCalledTimes(2);
    });

    it('check offline', () => {
        jest.useFakeTimers();
        const controller = new SubscriptionController({ timeout: 1000 });
        const spySubscribe = jest.fn();

        controller.onSubscribe.addListener(spySubscribe);

        controller.subscribe();

        transport.onStatusChanged.dispatch(TransportStatus.CLOSED);

        jest.runTimersToTime(1000);

        expect(spySubscribe).toBeCalledTimes(1);

        transport.onStatusChanged.dispatch(TransportStatus.OPEN);

        expect(spySubscribe).toBeCalledTimes(2);

        jest.runTimersToTime(1000);

        expect(spySubscribe).toBeCalledTimes(3);
    });
});

jest.unmock('../transport');
