describeBlock('adapter-adresa', function(block) {
    var data,
        snp;

    stubBlocks('adapter-adresa__single', 'adapter-adresa__multiple');

    beforeEach(function() {
        data = { log: { node: () => {} } };
        snp = { items: [{ numitems: 1 }] };
    });

    it('for numitems == 1 should call adapter-adresa__single once', function() {
        block(data, snp);

        assert.ok(blocks['adapter-adresa__single'].calledOnce);
    });

    it('for numitems == 1 should not call adapter-adresa__multiple', function() {
        block(data, snp);

        assert.notOk(blocks['adapter-adresa__multiple'].called);
    });

    it('for numitems > 1 should call adapter-adresa__multiple once', function() {
        snp.items[0].numitems = 2;

        block(data, snp);

        assert.ok(blocks['adapter-adresa__multiple'].calledOnce);
    });

    it('for numitems > 1 should not call adapter-adresa__single', function() {
        snp.items[0].numitems = 2;

        block(data, snp);

        assert.notOk(blocks['adapter-adresa__single'].called);
    });
});

describeBlock('adapter-adresa__single', function(block) {
    var kubr = ['ru', 'ua', 'by', 'kz'],
        data,
        snp;

    stubBlocks('adapter-adresa__url');

    beforeEach(function() {
        data = { expFlags: {}, reportData: { highlight: _.identity } };
        snp = {
            item: {
                metro: 'Марьино',
                phone: '+7 (915) 162-68-00',
                time: 'ежедневно, 0:00–2:00; ежедневно, 12:00–0:00'
            }
        };
    });

    it('should have metro after address if metro is present', function() {
        delete snp.item.time;

        var result = block(data, snp);

        assert.isDefined(result[0].items[0]);
    });

    it('should have no metro after address if no metro is present', function() {
        delete snp.item.time;
        delete snp.item.metro;

        var result = block(data, snp);

        assert.isUndefined(result[0].items[0]);
    });

    describe('when no time is present in Turkey', function() {
        beforeEach(function() {
            delete snp.item.time;
            delete snp.item.metro;
            data.tld = 'com.tr';
        });

        it('should have two rows', function() {
            var result = block(data, snp);

            assert.lengthOf(result, 2);
        });

        it('should have phone in first row', function() {
            var result = block(data, snp);

            assert.strictEqual(result[0].type, 'phone');
        });
    });

    describe('when time is present', function() {
        it('should have two rows', function() {
            var result = block(data, snp);

            assert.lengthOf(result, 2);
        });

        every(kubr, 'should not have type', function(tld) {
            data.tld = tld;

            var result = block(data, snp);

            assert.isNull(result[0].type);
        });

        it('should have type "clock" for "com.tr"', function() {
            data.tld = 'com.tr';

            var result = block(data, snp);

            assert.strictEqual(result[0].type, 'clock');
        });
    });
});
