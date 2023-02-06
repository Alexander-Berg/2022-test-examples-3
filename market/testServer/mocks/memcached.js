jest.mock('@yandex-market/b2b-core/shared/resolvers/memcached', () => ({
    memcachedFactory: () => null,
}));
