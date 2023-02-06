describe('dm-turbo-landing', function() {
    var dataModel;

    function createModel(data) {
        dataModel = BEM.MODEL.create('dm-turbo-landing', data || {});
    }



    describe('provideData', function(){
        describe('Если id лендинга не равен 0, то должен вернуть следующие поля:', function(){
            var provideData;

            before(function() {
                createModel({
                    id: '1',
                    name: 'Name',
                    href: 'yandex.ru/turbo'
                });

                provideData = dataModel.provideData();
            });

            after(function() {
                dataModel.destruct && dataModel.destruct();
            });

            ['id', 'name', 'href'].forEach(function(field){
                it(field, function() {
                    expect(provideData).to.have.property(field);
                });
            });
        });
        describe('Если id лендинга равен 0, то должен вернуть:', function(){
            var provideData;

            before(function() {
                createModel({
                    id: '0',
                    name: '',
                    href: ''
                });

                provideData = dataModel.provideData();
            });

            after(function() {
                dataModel.destruct && dataModel.destruct();
            });

            it('undefined', function() {
                expect(provideData).to.be.eq(undefined);
            });
        });
    });

});
