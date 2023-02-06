import filter from './filter';

const commonData: any = {
    FeatureToggles: {
        TestBucket: [],
        Handlers: [],
        RawHandlers: [],
    },
    Service: 'mail-mobile-apps',
    ConfigVersion: '16439',
    LogstatUID: '',
    CryptedExpBoxes: 'DjnxBf9FW7qNdPRiTxTrlxaqYXgqM44ZmMv3w-ihy8_lnS_BjXOmh-VwO933Lx2OSOA7P2SNkG3AZ9LjhYfNNg,,',
};

describe('filter', () => {
    it('kinda test', () => {
        const data = {
            ...commonData,
            Experiments: [
                {
                    TestBucket: [
                        { Testid: '12345', Bucket: -10 },
                        { Testid: '56789', Bucket: 42 },
                    ],
                    RawHandlers: [
                        '{"HANDLER":"TEST","CONTEXT":"BLAH"}',
                        '{"testid":"12345"}',
                        '{}',
                    ],
                },
            ],
        };

        expect(filter(data)).toMatchSnapshot();
    });

    it('kinda test 2', () => {
        const data = {
            ...commonData,
            Experiments: [
                {
                    TestBucket: [
                        { Testid: '283995', Bucket: 83 },
                        { Testid: '455527', Bucket: 51 },
                        { Testid: '434731', Bucket: 51 },
                        { Testid: '470189', Bucket: 87 },
                        { Testid: '391077', Bucket: 33 },
                    ],
                    RawHandlers: [
                        '{\"CONTEXT\":{\"DISK\":{\"testid\":[\"283995\"]}},\"HANDLER\":\"DISK\"}',
                        '{\"CONTEXT\":{\"MAIL\":{}},\"HANDLER\":\"MAIL\"}',
                        '{\"CONTEXT\":{\"TELEMOST\":{}},\"TESTID\":[\"283995\"],\"HANDLER\":\"TELEMOST\"}',
                        '{\"CONTEXT\":{\"MOBMAIL\":{\"flags\":{\"opt_in.folder_list\":false},\"logs\":{\"test_ids\":\"455527\"},\"source\":\"experiment\"}},\"HANDLER\":\"MOBMAIL\"}',
                        '{\"CONTEXT\":{\"MOBMAIL\":{\"flags\":{\"compose.notify_action\":true},\"logs\":{\"test_ids\":\"434731\"},\"source\":\"experiment\"}},\"HANDLER\":\"MOBMAIL\"}',
                        '{\"CONTEXT\":{\"MOBMAIL\":{\"flags\":{\"command_service.single_thread\":true},\"logs\":{\"test_ids\":\"470189\"},\"source\":\"experiment\"}},\"HANDLER\":\"MOBMAIL\"}',
                        '{\"CONTEXT\":{\"MOBMAIL\":{\"flags\":{\"backup\":true},\"logs\":{\"test_ids\":\"391077\"},\"source\":\"experiment\"}},\"HANDLER\":\"MOBMAIL\"}',
                    ],
                },
            ],
        };

        expect(filter(data)).toMatchSnapshot();
    });

    it('empty experiments', () => {
        const data: any = {
            Experiments: [],
        };

        expect(filter(data)).toMatchSnapshot();
    });

    it('empty experiments, not empty feature toggles', () => {
        const data: any = {
            FeatureToggles: {
                TestBucket: [
                    { Testid: '12345', Bucket: 83 },
                    { Testid: '455527', Bucket: 51 },
                ],
                RawHandlers: [
                    '{\"CONTEXT\":{\"MAIL\":{\"testid\":[\"12345\"]}},\"HANDLER\":\"MAIL\"}',
                ],
            },
            Experiments: [],
        };

        expect(filter(data)).toMatchSnapshot();
    });

    it('not empty experiments, not empty feature toggles', () => {
        const data: any = {
            FeatureToggles: {
                TestBucket: [
                    { Testid: '12345', Bucket: 83 },
                    { Testid: '455527', Bucket: 51 },
                ],
                RawHandlers: [
                    '{\"CONTEXT\":{\"MAIL\":{\"testid\":[\"12345\"]}},\"HANDLER\":\"MAIL\"}',
                ],
            },
            Experiments: [
                {
                    TestBucket: [
                        { Testid: '283995', Bucket: 83 },
                        { Testid: '455527', Bucket: 51 },
                    ],
                    RawHandlers: [
                        '{\"CONTEXT\":{\"DISK\":{\"testid\":[\"283995\"]}},\"HANDLER\":\"DISK\"}',
                        '{\"CONTEXT\":{\"MAIL\":{}},\"HANDLER\":\"MAIL\"}',
                        '{\"CONTEXT\":{\"TELEMOST\":{}},\"TESTID\":[\"283995\"],\"HANDLER\":\"TELEMOST\"}',
                        '{\"CONTEXT\":{\"MOBMAIL\":{\"flags\":{\"opt_in.folder_list\":false},\"logs\":{\"test_ids\":\"455527\"},\"source\":\"experiment\"}},\"HANDLER\":\"MOBMAIL\"}',
                    ],
                },
            ],
        };

        expect(filter(data)).toMatchSnapshot();
    });
});
