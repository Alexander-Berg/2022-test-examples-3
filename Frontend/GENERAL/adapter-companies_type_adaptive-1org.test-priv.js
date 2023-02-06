describeBlock('adapter-companies__adaptive-tabs-main', block => {
    let context;
    let state;

    stubBlocks([
        'adapter-companies__adaptive-tabs-main-text',
        'adapter-companies__figures',
        'adapter-companies__adaptive-tabs-main-grid',
        'adapter-companies_type_hotel__card-similar'
    ]);

    beforeEach(() => {
        context = { expFlags: {}, reportData: { reqdata: { passport: { logged_in: false } } } };
        state = {
            isTravelSubtype: true,
            reviews: true
        };
    });

    it('should contain similar hotels', () => {
        block(context, state);

        assert.calledOnce(blocks['adapter-companies_type_hotel__card-similar']);
    });
});

describeBlock('adapter-companies__adaptive-tabs-main-text', block => {
    let context;
    let state;

    beforeEach(() => {
        context = { expFlags: {}, reportData: { reqdata: { passport: { logged_in: false } } } };
        state = {
            isTravelSubtype: true
        };
    });

    it('should return tab «About hotel»', () => {
        const result = block(context, state);

        assert.equal(result.key, 'Про отель');
    });
});
