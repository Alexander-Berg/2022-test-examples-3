describe('b-page css', () => {
    let data;

    beforeEach(() => {
        data = stubData();
    });

    describeBlock('b-page__head-css', block => {
        stubBlocks('b-page__head-css-production');

        it('should set production css for production config', () => {
            _.set(data, 'config.name', 'production');

            blocks['b-page__head-css-production'].returns('b-page__head-css-production');

            assert.include(block(data), 'b-page__head-css-production');
        });

        it('should set production css for development config', () => {
            _.set(data, 'config.name', 'development');

            blocks['b-page__head-css-production'].returns('b-page__head-css-production');

            assert.include(block(data), 'b-page__head-css-production');
        });
    });

    describeBlock('b-page__main-css', block => {
        stubBlocks('b-page__main-css-production');

        it('should set production css for production config', () => {
            _.set(data, 'config.name', 'production');

            blocks['b-page__main-css-production'].returns('b-page__main-css-production');

            assert.equal(block(data), 'b-page__main-css-production');
        });

        it('should set production css for development config', () => {
            _.set(data, 'config.name', 'development');

            blocks['b-page__main-css-production'].returns('b-page__main-css-production');

            assert.equal(block(data), 'b-page__main-css-production');
        });
    });
});
