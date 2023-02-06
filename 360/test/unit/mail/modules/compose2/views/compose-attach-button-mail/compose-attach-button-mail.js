describe('Daria.vComposeAttachButtonMail', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-attach-button-mail');
    });

    var MIXIN_NAME = 'compose-attach-open-disk-mixin';

    it('Параметры вида должны быть такими же, как и у vCompose2', function() {
        expect(ns.View.info('compose-attach-button-mail').params).to.be.equal(ns.View.info('compose2').params);
    });

    it('Подмешивает миксин ' + MIXIN_NAME, function() {
        var proto = Object.getPrototypeOf(this.view);
        // проверяем, что все методы миксина присутствуют в прототипе вида
        var isMixed = _.all(ns.View.info(MIXIN_NAME).methods, function(method, methodName) {
            return methodName in proto;
        });

        expect(isMixed).to.be.ok;
    });

    describe('#onShow', function() {
        it('Должен вызвать инициализацию миксина ' + MIXIN_NAME, function() {
            this.sinon.stub(this.view, 'openDiskInit');

            this.view.onShow();

            expect(this.view.openDiskInit).to.have.callCount(1);
        });
    });

    describe('#onHide', function() {
        it('Должен вызвать деинициализацию миксина ' + MIXIN_NAME, function() {
            this.sinon.stub(this.view, 'openDiskDestroy');

            this.view.onHide();

            expect(this.view.openDiskDestroy).to.have.callCount(1);
        });
    });
});

