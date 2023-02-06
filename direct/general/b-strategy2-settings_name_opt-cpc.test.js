describe('b-strategy2-settings_name_opt-cpc', function() {

    var vm;

    beforeEach(function() {
        vm = BEM.MODEL.create('b-strategy2-settings_name_opt-cpc', {
            "currency": "RUB",
            "mediaType": "performance",
            "disabledMetrika": true,
            "target": "filter",
            "maxClickBidEnabled": false,
            "weekBidEnabled": false,
            "cpcPerFilter": 3
        });
    });

    afterEach(function() {
        vm.destruct();
    });

    describe('Поле options', function() {
        var options;
        [
            {
                title: 'кампании',
                options: {
                    target: 'camp',
                    cpcPerCamp: 4
                },
                fields: [
                    { target: 'camp' },
                    { avg_bid: 4 },
                    { originName: 'autobudget_avg_cpc_per_camp' }
                ]
            },
            {
                title: 'фильтра',
                options: {
                    target: 'filter',
                    cpcPerFilter: 5
                },
                fields: [
                    { target: 'filter' },
                    { filter_avg_bid: 5 },
                    { originName: 'autobudget_avg_cpc_per_filter' }
                ]
            },
            {
                title: 'maxClickBid: enabled',
                options: {
                    target: 'filter',
                    cpcPerFilter: 5,
                    maxClickBidEnabled: true,
                    maxClickBid: 500
                },
                fields: [
                    { bid: 500 }
                ]
            },
            {
                title: 'maxClickBid: disabled',
                options: {
                    target: 'filter',
                    cpcPerFilter: 5,
                    maxClickBidEnabled: false
                },
                fields: [
                    { bid: null }
                ]
            },
            {
                title: 'weekBidEnabled: true',
                options: {
                    target: 'filter',
                    cpcPerFilter: 5,
                    weekBidEnabled: true,
                    weekBid: 500
                },
                fields: [
                    { sum: 500 }
                ]
            },
            {
                title: 'weekBidEnabled: false',
                options: {
                    target: 'filter',
                    cpcPerFilter: 5,
                    weekBidEnabled: false
                },
                fields: [
                    { sum: null }
                ]
            }
        ].forEach(function(test) {
            describe('Для ' + test.title + ' возвращает', function() {
                beforeEach(function() {
                    vm.update(test.options);
                    options = vm.get('options');
                });

                test.fields.forEach(function(field) {
                    it(JSON.stringify(field), function() {
                        var key = Object.keys(field)[0];
                        expect(options[key]).to.equal(field[key]);
                    })
                });
            });
        });
    });
});
