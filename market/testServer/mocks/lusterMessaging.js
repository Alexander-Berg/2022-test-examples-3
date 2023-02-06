jest.mock('@yandex-market/luster-messaging', () => ({
    api: {
        onRequest: jest.fn(),
    },
}));
