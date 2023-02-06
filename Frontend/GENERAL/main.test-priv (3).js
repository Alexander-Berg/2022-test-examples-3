describeBlock('main__more', function(block) {
    let data;

    stubBlocks([
        'main__content',
        'main__push-favicons',
        'main__ajax-response-content-html'
    ]);

    beforeEach(function() {
        data = stubData();
        data.navi = { cur_page: 2, last_page: 10 };
        data.log = { baobabTree: { tree: { id: 'BAOBAB_ID' } } };
        blocks['main__content'].returns({ content: 'MAIN_CONTENT' });
    });

    it('should call main__push-favicons', function() {
        block(data);

        assert.calledOnce(blocks['main__push-favicons']);
    });
});
