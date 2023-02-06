jest.mock('@yandex-market/mandrel/lib/User/region', () => ({
    init: jest.fn(() => Promise.resolve({})),
}));
