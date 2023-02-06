import { MessengerPushSubscriber } from '../MessengerPushSubscriber';
import { RegistryTransportTypes } from '../../../private/RegistryApi';
import { IRegistryApi } from '../../../private/RegistryApi/types';
import { SubscriptionProvider } from '../types';

const createMockRegistryApi: (vapid: Uint8Array) => RegistryTransportTypes.IRegistryApi = (vapid) => ({
    getVapid: jest.fn(() => Promise.resolve(vapid)),
    revokePushToken: jest.fn(),
    setPushToken: jest.fn(),
    requestUser: jest.fn(() => Promise.resolve({} as any)),
});

const createMockSubscription: () => PushSubscription = () => {
    return {
        endpoint: '',
        expirationTime: 0,
        options: {
            applicationServerKey: null,
            userVisibleOnly: false,
        },
        getKey: jest.fn(() => null),
        toJSON: jest.fn(),
        unsubscribe: jest.fn(),
    };
};

describe('MessengerPushSubscriber', () => {
    let mockVapid: Uint8Array;
    let mockSubscriptionProvider: SubscriptionProvider;
    let mockRegistryApi: IRegistryApi;
    let mockSubscription: PushSubscription;

    beforeEach(() => {
        mockVapid = Uint8Array.from([42]);
        mockSubscription = createMockSubscription();
        mockRegistryApi = createMockRegistryApi(mockVapid);
        mockSubscriptionProvider = jest.fn(() => Promise.resolve(mockSubscription));
    });

    it('should call registryApi#getVapid if vapid not provided', async () => {
        mockSubscriptionProvider = jest.fn((getVapid) => getVapid().then(() => Promise.resolve(mockSubscription)));
        const pushManager = new MessengerPushSubscriber(mockRegistryApi, mockSubscriptionProvider);

        await pushManager.subscribe({ uuid: '', deviceId: '' });

        expect(mockRegistryApi.getVapid).toBeCalledTimes(1);
        expect(mockSubscriptionProvider).toBeCalledTimes(1);
        expect(mockRegistryApi.requestUser).toBeCalledTimes(1);
    });

    it('should not call registryApi#getVapid if vapid provided', async () => {
        const pushManager = new MessengerPushSubscriber(mockRegistryApi, mockSubscriptionProvider);

        await pushManager.subscribe({ uuid: '', deviceId: '' });

        expect(mockRegistryApi.getVapid).toBeCalledTimes(0);
        expect(mockSubscriptionProvider).toBeCalledTimes(1);
        expect(mockRegistryApi.requestUser).toBeCalledTimes(1);
    });

    it('should create subscription if factory returns null', async () => {
        const pushManager = new MessengerPushSubscriber(
            mockRegistryApi,
            mockSubscriptionProvider,
        );

        await pushManager.subscribe({ uuid: '', deviceId: '' });

        expect(mockSubscriptionProvider).toBeCalledTimes(1);
        expect(mockRegistryApi.requestUser).toBeCalledTimes(1);
    });

    it('should call registryApi#setPushToken with valid default params', async () => {
        const pushManager = new MessengerPushSubscriber(
            mockRegistryApi,
            mockSubscriptionProvider,
        );
        const uuid = '123';

        await pushManager.subscribe({ uuid, deviceId: '' });

        expect(mockRegistryApi.setPushToken).toBeCalledTimes(1);
        expect(mockRegistryApi.setPushToken).toBeCalledWith({
            UUID: uuid,
            params: {
                token: JSON.stringify(mockSubscription),
                token_type: 'web_sup',
                package_name: 'yandex_web_push',
                device_id: expect.stringMatching(/^........-....-4...-....-............$/),
            },
        });
        expect(mockRegistryApi.requestUser).toBeCalledTimes(1);
    });

    it('should call registryApi#setPushToken with valid passed params', async () => {
        const pushManager = new MessengerPushSubscriber(
            mockRegistryApi,
            mockSubscriptionProvider,
        );
        const uuid = '123';
        const deviceId = 'device_id';
        const tokenType = 'web_sup';
        const workspaceId = 12;

        await pushManager.subscribe({ uuid, deviceId, tokenType, workspaceId });

        expect(mockRegistryApi.setPushToken).toBeCalledTimes(1);
        expect(mockRegistryApi.setPushToken).toBeCalledWith({
            UUID: uuid,
            params: {
                token: JSON.stringify(mockSubscription),
                token_type: tokenType,
                device_id: deviceId,
                package_name: `yandex_${workspaceId}`,
            },
        });
        expect(mockRegistryApi.requestUser).toBeCalledTimes(1);
    });
});
