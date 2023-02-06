'use strict';

const { initSettings } = require('./../../../../middleware/settings/settings');

describe('init settings middleware', () => {
    test('shouldn\'t change default settings', async (done) => {
        const firstReq = {
            transactionId: 'transaction id',
            ajaxSessionId: 'ajax session id',
            affId: 1400,
            clid: 2210590,
            cookies: {},
            query: {},
            body: {}
        };

        const firstRes = {};

        const next = () => {
            firstReq.settings.ad.count.show = 1;
            firstReq.settings.ad.count.check = 2;
            firstReq.settings.ad.count.click = 3;
            firstReq.settings.ad.partner.clids.push(4);
            firstReq.settings.ad.partner.aff_ids.push(5);

            const secondReq = {
                transactionId: 'transaction id',
                ajaxSessionId: 'ajax session id',
                affId: 1500,
                clid: 2220590,
                cookies: {},
                query: {},
                body: {}
            };

            const secondRes = {};

            initSettings(secondReq, secondRes, () => {
                try {
                    expect(firstReq.settings.ad.count.show).toBe(1);
                    expect(secondReq.settings.ad.count.show).toBe(0);

                    expect(firstReq.settings.ad.count.check).toBe(2);
                    expect(secondReq.settings.ad.count.check).toBe(0);

                    expect(firstReq.settings.ad.count.click).toBe(3);
                    expect(secondReq.settings.ad.count.click).toBe(0);

                    expect(firstReq.settings.ad.partner.clids).toEqual([4]);
                    expect(secondReq.settings.ad.partner.clids).toEqual([]);

                    expect(firstReq.settings.ad.partner.aff_ids).toEqual([5]);
                    expect(secondReq.settings.ad.partner.aff_ids).toEqual([]);
                } catch (err) {
                    done(err);
                    return;
                }

                done();
            });
        };

        initSettings(firstReq, firstRes, next);
    });
});
