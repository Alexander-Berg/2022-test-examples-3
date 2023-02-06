jest.mock('@yandex-market/mandrel/bcm/tvm/TvmSettings', () => ({
    setup: jest.fn(),
    getSettings: jest.fn(() => ({
        tvmEnabled: false,
        backends: new Set(),
        services: new Set(),
    })),
}));
