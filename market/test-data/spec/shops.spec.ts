import type {Shop} from 'spec/utils';

import {expectUser} from './users.spec';
import productionShops from '../production-shops';
import testingShops from '../testing-shops';
import freeTestingShops from '../free-testing-shops';

const expectShop = (shop: Shop) => {
    expect(shop).toBeInstanceOf(Object);

    const {
        campaignId,
        shopId,
        contacts: {owner, developer, operator},
    } = shop;

    expect(campaignId).toEqual(expect.any(Number));
    expect(campaignId).toBeGreaterThan(0);

    expect(shopId).toEqual(expect.any(Number));
    expect(shopId).toBeGreaterThan(0);

    expectUser(owner);

    if (developer) {
        expectUser(developer);
    }
    if (operator) {
        expectUser(operator);
    }
};

describe('shops list', () => {
    it('список магазинов для тестинга содержит лишь магазины со всеми обязательными полями', () => {
        Object.keys(testingShops).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expectShop(testingShops[key]);
        });
    });

    it('список free-магазинов для тестинга содержит лишь магазины со всеми обязательными полями', () => {
        Object.keys(freeTestingShops).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expectShop(freeTestingShops[key]);
        });
    });

    it('список магазинов для престейбла содержит лишь магазины со всеми обязательными полями', () => {
        Object.keys(productionShops).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expectShop(productionShops[key]);
        });
    });
});
