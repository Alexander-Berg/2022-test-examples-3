describe('Daria.mQuickReplyState', function() {

    beforeEach(function() {
        this.model = ns.Model.get('quick-reply-state');
        this.model._onInit();
    });

    describe('#showForm', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, '_tmpEditorInit');
        });

        it('по умолчанию должен установить признак показа формы', function() {
            this.sinon.stub(this.model, 'set');
            this.model.showForm();
            expect(this.model.set).to.be.calledWith('.show', true);
        });

        it('если форма уже показана, то повторная установка не выполняется', function() {
            this.model.set('.formIsShown', true);
            this.sinon.stub(this.model, 'set');
            this.model.showForm();
            expect(this.model.set.callCount).to.be.equal(0);
        });

        it('должен создать фейковый редактор при установке признака показа формы', function() {
            this.model.set('.formIsShown', false);
            this.model.showForm();
            expect(this.model._tmpEditorInit.callCount).to.be.equal(1);
        });
    });

    describe('#toShowForm', function() {
        it('если хаб композа не загружен, то выводить форму нельзя', function() {
            this.sinon.stub(Jane.Services, 'isDone').withArgs(this.model.COMPOSE_HUB).returns(false);
            expect(this.model.toShowForm()).to.not.ok;
        });

        it('если хаб загружен, то выводим форму, если установлен признак', function() {
            this.sinon.stub(Jane.Services, 'isDone').withArgs(this.model.COMPOSE_HUB).returns(true);
            this.model.showForm();
            expect(this.model.toShowForm()).to.be.ok;
        });
    });

    describe('#hideForm', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, '_tmpEditorDestroy');
        });

        it('должен сбросить значения show и showFieldPlaceholder', function() {
            this.sinon.stub(this.model, 'setIfChanged');
            this.model.hideForm();
            expect(this.model.setIfChanged).to.be.calledWithExactly('.show', false, undefined);
            expect(this.model.setIfChanged).to.be.calledWithExactly('.showFieldPlaceholder', true, undefined);
        });

        it('должен удалить фейковый редактор', function() {
            this.model.hideForm();
            expect(this.model._tmpEditorDestroy.callCount).to.be.equal(1);
        });
    });

    describe('#_tmpEditorDestroy', function() {
        it('Если указано id редактора, должен удалить редактор и сбросить значение id', function() {
            this.model.set('.tmpEditorId', 'test');
            $('<div id="test" />').appendTo('body');

            this.sinon.stub(this.model, 'set');

            this.model._tmpEditorDestroy();
            expect(this.model.set).to.be.calledWith('.tmpEditorId', undefined, { silent: true });
            expect($('#test').length).to.be.equal(0);
        });
    });

    describe('#_tmpEditorInit', function() {
        it('должен удалить предыдущий редактор', function() {
            this.sinon.stub(this.model, '_tmpEditorDestroy');
            this.model._tmpEditorInit();
            expect(this.model._tmpEditorDestroy.callCount).to.be.equal(1);
        });

        it('должен создать редактор и записать его id в стейт', function() {
            this.sinon.stub(_, 'uniqueId').returns('test1');
            this.sinon.stub(this.model, '_tmpEditorDestroy');
            this.sinon.stub(this.model, 'set');

            this.model._tmpEditorInit();
            expect(this.model.set).to.be.calledWith('.tmpEditorId', 'test1', { silent: true });
            expect($('#test1').length).to.be.equal(1);
        });
    });
});

