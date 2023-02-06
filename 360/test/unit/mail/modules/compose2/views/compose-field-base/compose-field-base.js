describe('Daria.vComposeFieldBase', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-field-base');
        this.mComposeMessage = this.view.getModel('compose-message');
    });

    describe('#onInput', function() {
        beforeEach(function() {
            var $input = $("<input value='testValue'/>");
            this.e = {
                currentTarget: $input[0],
                target: $input[0]
            };

            this.sinon.stub(this.view, 'setFieldData');
        });

        it('должен вызвать метод записи значения в модель с передачей данных поля', function() {
            this.view.onInput(this.e);
            expect(this.view.setFieldData).calledWithExactly('testValue');
        });
    });

    describe('#setFieldData', function() {
        beforeEach(function() {
            this.sinon.stub(this.mComposeMessage, 'setIfChanged');
        });

        it('должен вызвать запись данных в модель с данными об отсутствии необходимости перерисовки', function() {
            this.view.FIELD_NAME = 'coolFieldName';
            this.view.setFieldData('test');

            expect(this.mComposeMessage.setIfChanged)
                .calledWithExactly('.coolFieldName', 'test', { data: { needUpdate: false } });
        });
    });
});
