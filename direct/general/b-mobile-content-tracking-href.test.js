describe('b-mobile-content-tracking-href', function() {
    var DM,
        model;

    describe('Проверка значения полей в DM и поведение блока', function() {
        beforeEach(function() {
            DM = BEM.MODEL.create('dm-mobile-content-banner', {
                modelId: 'bnr1',
                href: 'http://yandex.ru'
            });

            model = BEM.MODEL.create({ name: 'b-mobile-content-tracking-href', id: 'bnr1' }, {
                href: 'http://yandex.ru'
            });

            model.init();
        });

        afterEach(function() {
            DM.destruct();
            model.destruct();
        });

        describe('значние полей в DM', function() {

            it('должно синхронизироваться с ссылкой', function() {
                model.set('href', 'example.com');

                expect(DM.get('href')).to.be.equal('https://example.com/');
            });

            it('должно синхронизироваться с полной ссылкой', function() {
                model.set('href', 'https://example.com');

                expect(DM.get('href')).to.be.equal('https://example.com');
                expect(DM.get('url_protocol')).to.be.equal('https://');
            });

            it('должно быть пустым при пустой ссылке', function() {
                model.set('href', '');

                expect(DM.get('href')).to.be.equal('');
            });
        });

        describe('поведение', function() {

            it('при вводе полной ссылки, должен менять протокол', function() {
                model.set('href', 'https://example.ru');

                expect(model.get('protocol')).to.be.equal('https://');
            });

            it('при вводе ссылки длиннее 1024, должен сообщать о привышении лимита', function() {
                // Array(1026).join('w') - создает строку в 1026 символов
                model.set('href', Array(1026).join('w'));

                expect(model.get('lengthExceeded')).to.be.equal(true);
            });

            it('при вводе ссылки равной 1024, не должен сообщать о привышении лимита', function() {
                model.set('href', Array(1024).join('w'));

                expect(model.get('lengthExceeded')).to.be.equal(false);
            });

            it('при вводе ссылки меньше 1024, не должен сообщать о привышении лимита', function() {
                model.set('href', Array(0).join('w'));

                expect(model.get('lengthExceeded')).to.be.equal(false);
            });

        });
    });
});
