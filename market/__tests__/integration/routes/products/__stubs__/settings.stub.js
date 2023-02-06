const RESPONSE = expect.objectContaining({
    autoShowShopList: expect.any(Boolean),
    applicationName: 'Яндекс.Советник',
    isMbrApplication: expect.any(Boolean),
    constants: expect.any(Object),
    items: [
        {
            title: expect.any(String),
            enabled: expect.any(Boolean),
        },
        {
            title: expect.any(String),
            enabled: expect.any(Boolean),
        },
    ],
    region: expect.any(String),
    needShowGDPR: expect.any(Boolean),
    needShowNotifications: expect.any(Boolean),
    showProductNotifications: expect.any(Boolean),
    showAviaNotifications: expect.any(Boolean),
    showAutoNotifications: expect.any(Boolean),
});

module.exports = RESPONSE;
