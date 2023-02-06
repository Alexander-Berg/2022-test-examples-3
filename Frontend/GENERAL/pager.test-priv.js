describeBlock('pager', function(block) {
    let glob;

    let data;
    let pagerItems;

    function hasMix(block, checkObj) {
        return block.mix && [].concat(block.mix).some(function(item) {
            return _.isEqual(item, checkObj);
        });
    }

    beforeEach(function() {
        glob = stubGlobal('RequestCtx');
        RequestCtx.GlobalContext.query = { text: 'a', xmlEscaped: 'a', uriEscaped: 'a' };
        RequestCtx.GlobalContext.expFlags = stubData('experiments');

        data = stubData('cgi', 'counters', 'experiments');

        data.navi = {
            cur_page: '0',
            last_page: '99'
        };
    });

    afterEach(() => {
        glob.restore();
    });

    it('should have empty content for one page result', function() {
        data.navi.cur_page = '0';
        data.navi.last_page = '0';
        assert.equal(block(data).content, '');
    });

    it('should have "aria-labelledby" attribute', function() {
        assert.equal(block(data).attrs['aria-labelledby'], 'pager__header_id');
    });

    it('should have header with "id" attribute', function() {
        const header = block(data).content[0];
        assert.equal(header.attrs.id, 'pager__header_id');
    });

    describe('on first page', function() {
        beforeEach(function() {
            data.navi.cur_page = '0';
            data.navi.last_page = '10';
            pagerItems = block(data).content[1];
        });

        it('should have _current_yes mod for first item', function() {
            const firstItem = pagerItems.content[0];
            assert.equal(firstItem.elemMods.current, 'yes');
        });

        it('should have appropriate value of "aria-label" attribute for first item', function() {
            const firstItem = pagerItems.content[0];
            assert.equal(firstItem.attrs['aria-label'], 'Текущая страница 1');
        });

        it('should have _kind_page mod for first item', function() {
            const firstItem = pagerItems.content[0];
            assert.equal(firstItem.elemMods.kind, 'page');
        });

        it('should display "1" in first item', function() {
            const firstItem = pagerItems.content[0];
            assert.equal(firstItem.content, '1');
        });

        it('should have _kind_next mod for last item', function() {
            const lastItem = _.last(pagerItems.content);

            assert(hasMix(lastItem, {
                block: 'pager', elem: 'item', elemMods: { kind: 'next' }
            }));
        });

        it('should have appropriate value of "aria-label" attribute for last item', function() {
            const lastItem = _.last(pagerItems.content);
            assert.equal(lastItem.attrs['aria-label'], 'Следующая страница');
        });
    });

    describe('on fourth page', function() {
        beforeEach(function() {
            data.navi.cur_page = '3';
            data.navi.last_page = '10';
            pagerItems = block(data).content[1];
        });

        it('should have _current_yes mod for fourth item', function() {
            const fourthItem = pagerItems.content[3];
            assert.equal(fourthItem.elemMods.current, 'yes');
        });

        it('should have appropriate value of "aria-label" attribute for fourth item', function() {
            const fourthItem = pagerItems.content[3];
            assert.equal(fourthItem.attrs['aria-label'], 'Текущая страница 4');
        });

        it('should have _kind_begin mod for first item', function() {
            const firstItem = pagerItems.content[0];
            assert(hasMix(firstItem, {
                block: 'pager', elem: 'item', elemMods: { kind: 'begin' }
            }));
        });

        it('should have appropriate value of "aria-label" attribute for begin item', function() {
            const beginItem = pagerItems.content[0];
            assert.equal(beginItem.attrs['aria-label'], 'Первая страница');
        });

        it('should have _kind_next mod for last item', function() {
            const lastItem = _.last(pagerItems.content);

            assert(hasMix(lastItem, {
                block: 'pager', elem: 'item', elemMods: { kind: 'next' }
            }));
        });
    });

    describe('on last page', function() {
        beforeEach(function() {
            data.navi.cur_page = '9';
            data.navi.last_page = '9';
            pagerItems = block(data).content[1];
        });

        it('should have _kind_begin mod for first item', function() {
            const firstItem = pagerItems.content[0];

            assert(hasMix(firstItem, {
                block: 'pager', elem: 'item', elemMods: { kind: 'begin' }
            }));
        });

        it('should have _current_yes mod for last item', function() {
            const lastItem = _.last(pagerItems.content);
            assert.equal(lastItem.elemMods.current, 'yes');
        });

        it('should have _kind_page mod for last item', function() {
            const lastItem = _.last(pagerItems.content);
            assert.equal(lastItem.elemMods.kind, 'page');
        });

        it('should have no _kind_next mod for last item', function() {
            const lastItem = _.last(pagerItems.content);
            assert.notEqual(lastItem.elemMods.kind, 'next');
        });

        it('should have appropriate value of "aria-label" attribute for last item', function() {
            const lastItem = pagerItems.content[3];
            assert.equal(lastItem.attrs['aria-label'], 'Текущая страница 10');
        });
    });
});
