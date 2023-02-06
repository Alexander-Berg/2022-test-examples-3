describe('Daria.vToolbarButton', function() {
    beforeEach(function() {
        this.view = ns.View.create('toolbar-button');
        this.sinon.stubGetModel(this.view, 'messages-checked');
    });

    describe('#defaultAction', function() {
        beforeEach(function() {
            this.sinon.stub(ns, 'request');
            this.sinon.stub(ns.action, 'run');
        });

        [ 'forward', 'sendon', 'reply', 'reply-all' ].forEach(function(action) {
            it('должен вызвать action для кнопки "' + action + '"', function() {
                var view = ns.View.create('toolbar-button', { id: action });
                return view.update().then(function() {
                    view.defaultAction();
                    expect(ns.action.run).to.be.calledWith(action, {
                        buttonView: view,
                        mMessagesChecked: view.getCheckedModel(),
                        toolbar: 1
                    });
                });
            });
        });
    });

    describe('#canBeShownSelectOrDeselect', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'getCheckedModel').returns(this.mMessagesChecked);

            this.sinon.stub(this.mMessagesChecked, 'getCount');
            this.sinon.stub(this.mMessagesChecked, 'shouldSelectAll');

            this.getId = this.sinon.stub(this.view, 'getId').returns('reply');
        });

        describe('Кнопка "Выбрать все" -> ', function() {
            beforeEach(function() {
                this.getId.returns('select-all');
            });

            it('Должен разрешить показ, если подходит id кнопки и можно выбрать все письма', function() {
                this.mMessagesChecked.shouldSelectAll.returns(true);

                expect(this.view.canBeShownSelectOrDeselect()).to.be.equal(true);
            });

            it('Должен запретить показ, если подходит id кнопки и нельзя выбрать все письма', function() {
                this.mMessagesChecked.shouldSelectAll.returns(false);

                expect(this.view.canBeShownSelectOrDeselect()).to.be.equal(false);
            });
        });

        describe('Кнопка "Снять выделение" -> ', function() {
            beforeEach(function() {
                this.getId.returns('deselect');
            });

            it('Должен разрешить показ, если подходит id кнопки и можно снять выделение', function() {
                this.mMessagesChecked.shouldSelectAll.returns(false);

                expect(this.view.canBeShownSelectOrDeselect()).to.be.equal(true);
            });

            it('Должен разрешить показ, если подходит id кнопки и количество выделенных писем' +
                'превышает заданный минимум', function() {
                this.mMessagesChecked.shouldSelectAll.returns(true);
                this.mMessagesChecked.getCount.returns(5);

                expect(this.view.canBeShownSelectOrDeselect()).to.be.equal(true);
            });

            it('Должен запретить показ, если подходит id кнопки, но не все письма выделены и количество' +
                'выделенных не превышает минимум', function() {
                this.mMessagesChecked.getCount.returns(3);
                this.mMessagesChecked.shouldSelectAll.returns(true);

                expect(this.view.canBeShownSelectOrDeselect()).to.be.equal(false);
            });
        });

        it('Должен запретить показ, если не подходит id кнопки', function() {
            expect(this.view.canBeShownSelectOrDeselect()).to.be.equal(false);
        });
    });
});
