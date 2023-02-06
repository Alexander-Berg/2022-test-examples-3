describeBlock('serp-adv__displayed', function(block) {
    var context, result;

    beforeEach(function() {
        sinon.stub(blocks, 'serp-adv__add-to-not-show').returns(false);
        context = {
            reportData: {
                ...stubData('direct', 'i18n', 'cgi'),
                app_host: {
                    word_stat: {
                        data: 42
                    }
                }
            }
        };
    });

    afterEach(function() {
        blocks['serp-adv__add-to-not-show'].restore();
    });

    it('should return correct displayed count for optimistic case', function() {
        result = block(context);

        assert.equal(result.content, '42 показа в месяц');
    });

    it('should return undefined if "serp-adv__add" will not be shown', function() {
        blocks['serp-adv__add-to-not-show'].returns(true);

        result = block(context);

        assert.isUndefined(result);
    });

    every(
        [
            [1, '1 показ в месяц'],
            [2, '2 показа в месяц'],
            [5, '5 показов в месяц'],
            [100, '100 показов в месяц'],
            [1000, '1 000 показов в месяц'],
            [9999, '9 999 показов в месяц']
        ],
        'should return number with split thousands for count less than 10000',
        function(count) {
            context.reportData.app_host.word_stat.data = count[0];

            result = block(context);

            assert.equal(result.content, count[1]);
        }
    );

    every(
        [
            [10000, '10 тыс. показов в месяц'],
            [14000, '14 тыс. показов в месяц'],
            [14500, '14 тыс. показов в месяц'],
            [14999, '14 тыс. показов в месяц'],
            [25999999, '25 млн показов в месяц'],
            [61999999999, '61 млрд показов в месяц']
        ],
        'should return short number rounded down for count >= 10000',
        function(count) {
            context.reportData.app_host.word_stat.data = count[0];

            result = block(context);

            assert.equal(result.content, count[1]);
        }
    );
});
