'use strict';

const extract = require('./../../../middleware/client-event/extract-client-event');

describe('extracting with old format', () => {
    test('should extract info about closing', () => {
        const query = {
            name: 'suggest_script.2210590.sitebar.pricebar.closed.click',
            value: 1,
            transaction_id: 'ijzfdrldtbmwex1tsbx522n7hrzhhgzs'
        };

        const info = extract({ query });

        expect(info).toEqual([
            {
                event: 'pricebar_close',
                clid: '2210590',
                typeView: 'desktop',
                old: true,
                transactionId: 'ijzfdrldtbmwex1tsbx522n7hrzhhgzs'
            }
        ]);
    });

    test('should extract info about closing without transaction id', () => {
        const query = {
            name: 'suggest_script.2210590.sitebar.pricebar.closed.click',
            value: 1
        };

        const info = extract({ query });

        expect(info).toEqual([
            {
                event: 'pricebar_close',
                clid: '2210590',
                typeView: 'desktop',
                old: true
            }
        ]);
    });

    test('should extract info about error', () => {
        const query = {
            name: 'suggest_script.2210590.script.error.size',
            value: 1,
            transaction_id: 'ijzfdrldtbmwex1tsbx522n7hrzhhgzs'
        };

        const info = extract({ query });

        expect(info).toEqual([
            {
                event: 'error',
                eventDetails: 'size',
                clid: '2210590',
                old: true,
                typeView: 'desktop',
                transactionId: 'ijzfdrldtbmwex1tsbx522n7hrzhhgzs'
            }
        ]);
    });

    test('should extract info about wrong product', () => {
        const query = {
            name: 'suggest_script.2210590.script.wrong_product',
            value: 1,
            transaction_id: 'ijzfdrldtbmwex1tsbx522n7hrzhhgzs'
        };

        const info = extract({ query });

        expect(info).toEqual([
            {
                event: 'wrong_product',
                clid: '2210590',
                old: true,
                typeView: 'desktop',
                transactionId: 'ijzfdrldtbmwex1tsbx522n7hrzhhgzs'
            }
        ]);
    });
});

describe('extracting with new format', () => {
    test('should extract info about closing', () => {
        const body = { transaction_id: 'ijzm26lcqhste2hzxv10gx02gihdn7z4', interaction: 'pricebar_close' };
        const settings = {
            clid: '100500',
            clientId: 'test',
            bucketInfo: {
                test: 'original'
            }
        };

        const info = extract({ body, settings, cookies: {} });

        expect(info).toEqual([
            {
                event: 'pricebar_close',
                clid: '100500',
                transactionId: 'ijzm26lcqhste2hzxv10gx02gihdn7z4',
                clientId: 'test',
                isMaster: true,
                optOutStatus: 'without',
                bucketInfo: {
                    test: 'original'
                },
                withButton: false
            }
        ]);
    });
});
