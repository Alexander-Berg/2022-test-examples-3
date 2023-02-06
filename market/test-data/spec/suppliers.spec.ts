import type {Shop} from 'spec/utils';

import {expectUser} from './users.spec';
import testingSuppliers from '../testing-suppliers';
import productionSuppliers from '../productions-suppliers';

const expectSupplier = (supplier: Shop) => {
    expect(supplier).toBeInstanceOf(Object);

    const {
        campaignId,
        shopId,
        contacts: {owner},
    } = supplier;

    expect(campaignId).toEqual(expect.any(Number));
    expect(campaignId).toBeGreaterThan(0);

    expect(shopId).toEqual(expect.any(Number));
    expect(shopId).toBeGreaterThan(0);

    expectUser(owner);
};

describe('suppliers list', () => {
    it('список поставщиков для тестинга содержит лишь поставщиков со всеми обязательными полями', () => {
        Object.keys(testingSuppliers).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expectSupplier(testingSuppliers[key]);
        });
    });
    it('список поставщиков для продакшена содержит лишь поставщиков со всеми обязательными полями', () => {
        Object.keys(productionSuppliers).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expectSupplier(productionSuppliers[key]);
        });
    });
});
