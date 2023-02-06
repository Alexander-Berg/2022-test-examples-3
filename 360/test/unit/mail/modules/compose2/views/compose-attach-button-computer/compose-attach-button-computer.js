describe('Daria.vComposeAttachButtonComputer', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-attach-button-computer');
        this.sinon.stub(ns.page.current, 'page').value('compose2');
    });

    it('должен содержать input без множественного выбора, если отключена настройка input-multiple', function() {
        this.sinon.stub(Daria.Config, 'input-multiple').value(false);

        return this.view.update().then(function() {
            var multiple = this.view.$node.find('.js-controller').attr('multiple');
            expect(multiple).to.be.equal(undefined);
        }, this);
    });

    it('должен содержать input с множественным выбором, если включена настройка input-multiple', function() {
        this.sinon.stub(Daria.Config, 'input-multiple').value(true);

        return this.view.update().then(function() {
            var multiple = this.view.$node.find('.js-controller').attr('multiple');
            expect(multiple).to.be.equal('multiple');
        }, this);
    });

    describe('#update', function() {
        beforeEach(function() {
            return this.view.update();
        });

        describe('#_onNsViewHtmlinit', function() {
            it('должен сохранить оригинал input', function() {
                var node = this.view.$node.find('.js-controller');
                expect(node[0].isEqualNode(this.view._$controllerNode[0])).to.be.ok;
            });
        });

        describe('#_getControllerNode', function() {
            it('должен вернуть ноду input', function() {
                var node = this.view.$node.find('.js-controller');
                expect(node[0].isEqualNode(this.view._getControllerNode()[0])).to.be.ok;
            });
        });

        describe('#_onChangeController', function() {
            beforeEach(function() {
                this.sinon.stub(this.view, 'addQuickReplyAttach');
                this.sinon.stub(this.view, '_openComposePage');
            });

            it('должен добавить аттачь в модель, если не в QR', function() {
                this.sinon.stub(this.view, 'inQuickReply').returns(false);
                var stub = this.sinon.stub(this.view.getModel('compose-attachments'), 'addAttach');
                var event = {
                    'target': {
                        'files': []
                    }
                };

                this.view._onChangeController(event);

                expect(stub).to.be.calledWith({
                    'input': event.target,
                    'files': event.target.files
                });

                expect(this.view.addQuickReplyAttach).to.have.callCount(0);
            });

            it('должен инициировать процесс добавления аттачей для QR, если в QR', function() {
                this.sinon.stub(this.view, 'inQuickReply').returns(true);
                var stub = this.sinon.stub(this.view.getModel('compose-attachments'), 'addAttach');
                var event = {
                    'target': {
                        'files': []
                    }
                };

                this.view._onChangeController(event);

                expect(this.view.addQuickReplyAttach).to.be.calledWith({
                    'input': event.target,
                    'files': event.target.files
                });

                expect(stub).to.have.callCount(0);
            });
        });

        describe('#_onNsModelAttachmentsAdded', function() {
            it('должен заменить ноду input оригинальным клоном', function() {
                var node = this.view.$node.find('.js-controller');
                this.view._onNsModelAttachmentsAdded();
                var newNode = this.view.$node.find('.js-controller');

                expect(node[0].isEqualNode(newNode[0])).to.be.ok;
                expect(node[0] !== newNode[0]).to.be.ok;
            });
        });
    });
});

