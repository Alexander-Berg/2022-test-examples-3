const proxyquire = require('proxyquire');

describe.skip('Localize, tanker', () => {
    const localizeStubs = {
        './keysets.json': {
            '@global': true,
            projectId: {
                keysetId: {
                    ru: {
                        a: [
                            {
                                version: 100,
                                default: 100,
                            },
                            {
                                version: 50,
                                default: 50,
                                ab: {
                                    ab_test: {
                                        first_variant: 'first',
                                        second_variant: 'second',
                                    },
                                },
                            },
                            {
                                version: 0,
                                default: 0,
                            },
                        ],
                    },
                },
            },
        },
    };

    test('should return localized value (1)', () => {
        const localize = proxyquire('./../../../../localize/tanker/index', localizeStubs);
        const expected = localizeStubs['./keysets.json'].projectId.keysetId.ru.a[2].default;
        const actual = localize('ru', { projectId: 'projectId', keysetId: 'keysetId', key: 'a' }, 30);

        expect(actual).toEqual(expected);
    });

    test('should return localized value (2)', () => {
        const localize = proxyquire('./../../../../localize/tanker/index', localizeStubs);
        const expected = localizeStubs['./keysets.json'].projectId.keysetId.ru.a[0].default;
        const actual = localize('ru', { projectId: 'projectId', keysetId: 'keysetId', key: 'a' }, 120);

        expect(actual).toEqual(expected);
    });

    test('should return localized value (3)', () => {
        const localize = proxyquire('./../../../../localize/tanker/index', localizeStubs);
        const expected = localizeStubs['./keysets.json'].projectId.keysetId.ru.a[1].ab.ab_test.first_variant;
        const actual = localize('ru', { projectId: 'projectId', keysetId: 'keysetId', key: 'a' }, 60, {
            ab_test: 'first_variant',
        });

        expect(actual).toEqual(expected);
    });

    test('should return localized value (3)', () => {
        const localize = proxyquire('./../../../../localize/tanker/index', localizeStubs);
        const expected = localizeStubs['./keysets.json'].projectId.keysetId.ru.a[1].default;
        const actual = localize('ru', { projectId: 'projectId', keysetId: 'keysetId', key: 'a' }, 60, {
            ab_test: 'third_variant',
        });

        expect(actual).toEqual(expected);
    });

    test('should return localized value (4)', () => {
        const localize = proxyquire('./../../../../localize/tanker/index', localizeStubs);
        const expected = localizeStubs['./keysets.json'].projectId.keysetId.ru.a[0].default;
        const actual = localize('ru', { projectId: 'projectId', keysetId: 'keysetId', key: 'a' }, 120, {
            ab_test: 'third_variant',
        });

        expect(actual).toEqual(expected);
    });
});
