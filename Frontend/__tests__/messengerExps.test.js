const { getMessengerExps } = require('../params/messengerExps');

const EXPERIMENTS_WITH_DATA = {
    contexts: {
        MAIN: {
            REPORT: {
                images_wizard_vcomm_threshold: 0.51,
                news_autoplay_serp_counters: 'sound,seek',
                direct_sitelinks_move: 2,
                direct_meta_clean: 1,
                direct_first_callout_bold: 1,
                imgwiz_market_prices: '1',
                direct_callouts_all: 3,
                'yandex-chats_test_data': 'test-data-value',
                'yandex-chats_test_data_yandex-chats_': 'data-with-prefix',
            },
        },
    },
};

const EXPERIMENTS_WITHOUT_DATA = {
    contexts: {
        MAIN: {
            REPORT: {
                images_wizard_vcomm_threshold: 0.51,
                news_autoplay_serp_counters: 'sound,seek',
                direct_sitelinks_move: 2,
                direct_meta_clean: 1,
                direct_first_callout_bold: 1,
                imgwiz_market_prices: '1',
                direct_callouts_all: 3,
            },
        },
    },
};

const EXPERIMENTS_EMPTY = {
    contexts: {
        MAIN: {},
    },
};

const EXTRACTED_DATA = {
    test_data: 'test-data-value',
    'test_data_yandex-chats_': 'data-with-prefix',
};

describe('#getMessengerExps', () => {
    it('Should extract messenger experiments', () => {
        expect(getMessengerExps(EXPERIMENTS_WITH_DATA)).toEqual(EXTRACTED_DATA);
    });

    it('Should work when there is no messenger exp data', () => {
        expect(getMessengerExps(EXPERIMENTS_WITHOUT_DATA)).toEqual({});
    });

    it('Should work when there is no data at all', () => {
        expect(getMessengerExps(EXPERIMENTS_EMPTY)).toEqual({});
        expect(getMessengerExps()).toEqual({});
    });

    it('Should filtered by serviceId', () => {
        expect(getMessengerExps({
            contexts: {
                MAIN: {
                    REPORT: {
                        'yandex-chats_test_data1': { value: 'test-data-value1' },
                        'yandex-chats_test_data2': 'test-data-value2',
                        'yandex-chats_test_data3': { value: 'test-data-value3', disabled_services: [2] },
                        'yandex-chats_test_data4': { value: 'test-data-value4', disabled_services: [3] },
                        'yandex-chats_test_data5': { value: 'test-data-value5', enabled_services: [2] },
                        'yandex-chats_test_data6': { value: 'test-data-value6', enabled_services: [3] },
                    },
                },
            },
        }, { serviceId: 2 })).toEqual({
            test_data1: 'test-data-value1',
            test_data2: 'test-data-value2',
            test_data4: 'test-data-value4',
            test_data5: 'test-data-value5',
        });
    });
});
