describeBlock('i-global__ajax-wizard-data', function(block) {
    var data = stubData(),
        result,

        // Колдунщик, данные которого нужно будет использовать при аякс запросах
        neededWzrd = {
            type: 'z-wzrd-needed',
            data: 'blablbablaba'
        },

        // Застабанные данные для неаяксового колдунщика
        notNeededWzrd = {
            type: 'z-wzrd-not-needed',
            data: 'content for needed wzrd'
        };

    it('should return undefined without data.ajaxData', function() {
        assert.isUndefined(block(data));
    });

    it('should return undefined without data.searchdata', function() {
        data.ajaxData = {};
        assert.isUndefined(block(data));
    });

    it('should have z-wzrd-needed in result obj with data.ajaxData', function() {
        // структура в том виде, что приходит из репорта
        data.wizplaces = {
            '0': [
                neededWzrd,
                notNeededWzrd
            ]
        };
        data.ajaxData[neededWzrd.type] = 1;

        result = block(data, neededWzrd.type);

        assert.isTrue(result === neededWzrd, neededWzrd.type, 'Objects isn`t equal');
    });
});
