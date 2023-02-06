describe('b-turbo-landings-selector', function() {
    var sandbox,
        block;

    function createBlock(opitons) {
        opitons || (opitons = {});

        block = u.getInitedBlock({
            block: 'b-turbo-landings-selector',
            value: opitons.value
        });
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
    });

    afterEach(function() {
        sandbox.restore();
        block.destruct && block.destruct();
    });

    describe('Содержимое блока:', function(){

        describe('после шаблонизации', function(){

            describe('если лендинг НЕ выбран', function(){
                it('метка должна содержать стандартный текст', function() {
                    sandbox.stub(window, 'iget2')
                        .withArgs('b-turbo-landings-selector', 'empty-label').returns('test-empty-label');

                    createBlock({ value: { } });

                    expect(block.elem('label').text()).to.be.eq('test-empty-label');
                });

                it('кнопка должна содержать текст \'Выбрать\'', function() {
                    sandbox.stub(window, 'iget2')
                        .withArgs('b-turbo-landings-selector', 'select-button-add').returns('выбрать');

                    createBlock({ value: { } });

                    expect(block.elem('button-select').text()).to.be.eq('выбрать');
                });
            });

            describe('если лендинг выбран', function(){
                it('метка должна содержать имя лендинга', function() {
                    createBlock({ value: { id: '123', name: 'test-turbolanding' } });

                    expect(block.elem('label').text()).to.be.eq('test-turbolanding');
                });

                it('кнопка должна содержать текст \'Изменить\'', function() {
                    sandbox.stub(window, 'iget2')
                        .withArgs('b-turbo-landings-selector', 'select-button-change').returns('изменить');

                    createBlock({ value: { id: '123', name: 'test-turbolanding' } });

                    expect(block.elem('button-select').text()).to.be.eq('изменить');
                });

            });
        });

        describe('после изменения', function(){

            describe('если лендинг НЕ выбран', function(){
                it('метка должна содержать стандартный текст', function() {
                    sandbox.stub(window, 'iget2')
                        .withArgs('b-turbo-landings-selector', 'empty-label').returns('test-empty-label');

                    createBlock({ value: { id: '123', name: 'test-turbolanding' } });
                    block.setValue({});

                    expect(block.elem('label').text()).to.be.eq('test-empty-label');
                });

                it('кнопка должна содержать текст \'Выбрать\'', function() {
                    sandbox.stub(window, 'iget2')
                        .withArgs('b-turbo-landings-selector', 'select-button-add').returns('выбрать');

                    createBlock({ value: { id: '123' } });
                    block.setValue({});

                    expect(block.elem('button-select').text()).to.be.eq('выбрать');
                });
            });

            describe('если лендинг выбран', function(){
                it('метка должна содержать имя лендинга', function() {
                    createBlock({ value: { } });

                    block.setValue({ id: '123', name: 'test-turbolanding' });

                    expect(block.elem('label').text()).to.be.eq('test-turbolanding');
                });

                it('кнопка должна содержать текст \'Изменить\'', function() {
                    sandbox.stub(window, 'iget2')
                        .withArgs('b-turbo-landings-selector', 'select-button-change').returns('изменить');

                    createBlock({ value: { } });

                    block.setValue({ id: '123' });

                    expect(block.elem('button-select').text()).to.be.eq('изменить');
                });

            });
        });

    });

    describe('Поведение блока', function(){

        describe('Выбор лендинга в конструкторе', function() {

             it('Устанавливает значение блока', function() {
                 createBlock();

                 block._onSelectLanding({}, { id: '123', name: 'test-turbolanding-1' });

                 expect(block.getValue()).to.deep.equal({ id: '123', name: 'test-turbolanding-1' });
             });

             it('Генерирует событие \'change\'', function() {
                 createBlock();

                 expect(block).to.triggerEvent('change',
                     { id: '123', name: 'test-turbolanding-2' },
                     function() {
                         block._onSelectLanding({}, { id: '123', name: 'test-turbolanding-2' });
                     });
             });
        });

        describe('setValue', function() {

            it('Устанавливает значение блока', function() {
                createBlock();

                block.setValue({ id: '123', name: 'test-turbolanding-3' });

                expect(block.getValue()).to.deep.equal({ id: '123', name: 'test-turbolanding-3' });
            });

            it('Не генерирует событие \'change\'', function() {
                createBlock();

                expect(block).to.not.triggerEvent('change',
                    function() {
                        block.setValue({ id: '123', name: 'test-turbolanding-4' });
                    });
            });

            it('Устанавливает пустой объект, если нет значения', function() {
                createBlock();

                block.setValue(null);

                expect(block.getValue()).to.deep.equal({ });
            });
        });

        describe('Кнопка \'удалить\'', function() {

            it('Удаляет значение блока', function() {
                createBlock({ value: { id: '123', name: 'test-turbolanding' } });

                block.elem('button-delete').click();
                sandbox.clock.tick(1);

                expect(block.getValue()).to.deep.equal({ });
            });

            it('Генерирует событие \'change\'', function() {
                createBlock();

                expect(block).to.triggerEvent('change', { },
                    function() {
                        block.elem('button-delete').click();
                        sandbox.clock.tick(1);
                    });
            });
        })

    });

});

