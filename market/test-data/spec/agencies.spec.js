const {expectUser} = require('./users.spec');
const testingAgencies = require('../testing-agencies');

const expectAgency = shop => {
    expect(shop).toBeInstanceOf(Object);

    const {
        campaignId,
        shopId,
        contacts: {agency, subclient},
    } = shop;

    expect(campaignId).toEqual(expect.any(Number));
    expect(campaignId).toBeGreaterThan(0);

    expect(shopId).toEqual(expect.any(Number));
    expect(shopId).toBeGreaterThan(0);

    expectUser(agency);

    if (subclient) {
        expectUser(subclient);
    }
};

describe('agencies list', () => {
    it('список агентств для тестинга содержит лишь агентства со всеми обязательными полями', () => {
        Object.keys(testingAgencies).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expectAgency(testingAgencies[key]);
        });
    });
});
