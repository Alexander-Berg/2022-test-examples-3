describe('Daria.vMessagesItemUserpic', function() {
    beforeEach(function() {
        this.view = ns.View.create('messages-item-userpic', {ids: '12334'});
    });

    describe('#onUserpicClick', function() {
        beforeEach(function() {
            this.clickEvent = {
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub(),
                target: {}
            };
            this.sinon.stub(this.view, 'logClickMessageFromSearch');
            this.sinon.stub(ns.page, 'go');
        });

        it('должен предотвратить действие по умолчанию', function() {
            this.view.onUserpicClick(this.clickEvent);

            expect(this.clickEvent.preventDefault).to.have.callCount(1);
        });

        it('должен прервать всплытие события', function() {
            this.view.onUserpicClick(this.clickEvent);

            expect(this.clickEvent.stopPropagation).to.have.callCount(1);
        });

        it('должен вызвать логгер поисковых кликов', function() {
            this.view.onUserpicClick(this.clickEvent);

            expect(this.view.logClickMessageFromSearch).to.have.callCount(1);
        });

        it('логгер поисковых кликов должен быть вызван с параметром avatar', function() {
            this.view.onUserpicClick(this.clickEvent);

            expect(this.view.logClickMessageFromSearch).to.be.calledWith('avatar');
        });

        it('должен вызвать переход, если у блока, на котором произошёл клик, есть href', function() {
            this.clickEvent.target.href = '#';

            this.view.onUserpicClick(this.clickEvent);

            expect(ns.page.go).to.have.callCount(1);
        });

        it('должен вызвать переход на url, равный href блока, по которому кликнули', function() {
            this.clickEvent.target.href = '#';

            this.view.onUserpicClick(this.clickEvent);

            expect(ns.page.go).to.be.calledWith('#');
        });

        it('не должен вызывать переход, если у блока, на котором произошёл клик, нет href', function() {
            this.clickEvent.target.href = undefined;

            this.view.onUserpicClick(this.clickEvent);

            expect(ns.page.go).to.have.callCount(0);
        });
    });
});
