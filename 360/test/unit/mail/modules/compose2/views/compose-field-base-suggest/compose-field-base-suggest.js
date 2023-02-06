describe('Daria.vComposeFieldBaseSuggest', function() {
    beforeEach(function() {
        this.controllerNode = $('<div />');

        this.view = ns.View.create('compose-field-base-suggest');
        this.view.FIELD_NAME = 'to';
        this.view.getControllerNode = function() {
            return this.controllerNode;
        }.bind(this);
        this.view.setFieldData = no.nop;
        this.view.areYabblesDisabled = no.true;

        this.viewFieldTo = ns.View.create('compose-field-to');

        this.sinon.stub(this.view, 'setFieldData');

        this.sinon.stub(ns.page.current, 'page').value('compose2');
    });

    describe('#suggestInit', function() {

        beforeEach(function() {
            this.view.areYabblesDisabled = no.false;
        });

        it('должен создать экземпляр саджеста', function() {
            this.sinon.spy(Daria.Suggest, 'Contacts');
            this.view.suggestInit();

            expect(Daria.Suggest.Contacts).to.be.calledWithExactly(null, {
                'getPositionNode': this.view._suggestGetPositionNode,
                'multiple': false,
                'fieldType': this.view.FIELD_NAME,
                'position': {
                    'my': 'left top',
                    'at': 'left+9px bottom-1px',
                    'collision': 'none'
                },
                'showAbookPopupButton': undefined
            });
        });

        it('должен определить параметр multiple из areYabblesDisabled', function() {
            this.sinon.spy(Daria.Suggest, 'Contacts');
            this.view.areYabblesDisabled = function() {
                return 'areYabblesDisabledResult';
            };

            this.view.suggestInit();

            expect(Daria.Suggest.Contacts).to.be.calledWithExactly(null, {
                'getPositionNode': this.view._suggestGetPositionNode,
                'multiple': 'areYabblesDisabledResult',
                'fieldType': this.view.FIELD_NAME,
                'position': {
                    'my': 'left top',
                    'at': 'left+9px bottom-1px',
                    'collision': 'none'
                },
                'showAbookPopupButton': undefined
            });
        });

        it('должен пробросить флаг showAbookPopupButton (прилетает из миксина Daria.vComposeFieldBaseAbook)', function() {
            this.sinon.spy(Daria.Suggest, 'Contacts');
            this.viewFieldTo.suggestInit();

            expect(Daria.Suggest.Contacts).to.be.calledWithExactly(null, {
                'getPositionNode': this.viewFieldTo._suggestGetPositionNode,
                'multiple': false,
                'fieldType': this.viewFieldTo.FIELD_NAME,
                'position': {
                    'my': 'left top',
                    'at': 'left+9px bottom-1px',
                    'collision': 'none'
                },
                'showAbookPopupButton': true
            });

        });

    });

    describe('#suggestStart', function() {
        beforeEach(function() {
            this.view.suggestInit();
            this.sinon.stub(this.view.suggestInstance, 'on');
            this.sinon.stub(this.view.suggestInstance, 'changeInputField');
        });

        it('должны подписаться на события выбора элемента саджеста и изменения значения', function() {
            this.view.suggestStart();
            expect(this.view.suggestInstance.on).to.be.calledWithExactly('select', this.view._suggestItemSelect);
            expect(this.view.suggestInstance.on).to.be.calledWithExactly('value-set', this.view._suggestValueSet);
            expect(this.view.suggestInstance.changeInputField).to.be.calledWithExactly(this.controllerNode);
        });

        it('если включены яблы, то на события не подписываемся', function() {
            this.sinon.stub(this.view, 'areYabblesDisabled').returns(false);
            this.view.suggestStart();
            expect(this.view.suggestInstance.on.callCount).to.be.equal(0);
            expect(this.view.suggestInstance.changeInputField.callCount).to.be.equal(0);
        });
    });

    describe('#suggestStop', function() {
        beforeEach(function() {
            this.view.suggestInit();
            this.sinon.stub(this.view.suggestInstance, 'off');
            this.sinon.stub(this.view.suggestInstance, 'destroy');
        });

        it('должны отписаться от show и события изменения фокуса', function() {
            this.view.suggestStop();
            expect(this.view.suggestInstance.off).to.be.calledWithExactly('select', this.view._suggestItemSelect);
            expect(this.view.suggestInstance.off).to.be.calledWithExactly('value-set', this.view._suggestValueSet);
            expect(this.view.suggestInstance.destroy).to.have.callCount(1);
        });

        it('если включены яблы, то от событий не отписываемся', function() {
            this.sinon.stub(this.view, 'areYabblesDisabled').returns(false);
            this.view.suggestStop();
            expect(this.view.suggestInstance.off.callCount).to.be.equal(0);
            expect(this.view.suggestInstance.destroy.callCount).to.be.equal(0);
        });
    });

    describe('#_suggestValueSet', function() {
        it('должен инициировать событие вставки данных в поле ввода', function() {
            this.view._suggestValueSet('nb-value-set', $.Event(), {}, 'test');

            expect(this.view.setFieldData).to.be.calledWithExactly('test');
        });
    });

});

