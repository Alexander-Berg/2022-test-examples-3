import type {Shop} from 'spec/utils';

import {expectUser} from './users.spec';
import deliveryTestingPartners from '../delivery-testings-partners';
import deliveryProductionPartners from '../delivery-production-partners';

const expectPartner = (partner: Shop) => {
    expect(partner).toBeInstanceOf(Object);

    const {
        campaignId,
        shopId,
        contacts: {owner},
    } = partner;

    expect(campaignId).toEqual(expect.any(Number));
    expect(campaignId).toBeGreaterThan(0);

    expect(shopId).toEqual(expect.any(Number));
    expect(shopId).toBeGreaterThan(0);

    expectUser(owner);
};

describe('delivery partners list', () => {
    it('список партнёров доставки для тестинга содержит партнёров со всеми обязательными полями', () => {
        Object.keys(deliveryTestingPartners).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expectPartner(deliveryTestingPartners[key]);
        });
    });
    it('список партнёров доставки для продакшена содержит партнёров со всеми обязательными полями', () => {
        Object.keys(deliveryProductionPartners).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expectPartner(deliveryProductionPartners[key]);
        });
    });
});
