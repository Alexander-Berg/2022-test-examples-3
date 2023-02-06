describe('Daria.vComposeMessageEditor', function() {
    beforeEach(function() {
        this.mQuickReplyState = ns.Model.get('quick-reply-state').setData({});

        this.view = ns.View.create('compose-message-editor');

        var models = this.sinon.stub(this.view, 'getModel');
        models.withArgs('quick-reply-state').returns(this.mQuickReplyState);

        this.sinon.stub(this.view, 'getEditor').returns({
            'mode': 'source',
            'execCommand': function() {}
        });
    });

    afterEach(function() {
        this.view.destroy();
    });

    describe('#onFocus', function() {
        beforeEach(function() {
            this.sinon.stub(this.mQuickReplyState, 'getTmpContentAndRemove');
            this.sinon.stub(this.view, 'toggleFocus');
            this.sinon.stub(this.view, 'restoreBookmark');
        });

        it('должен подставить содержимое временного редактора, если есть данные', function() {
            this.mQuickReplyState.getTmpContentAndRemove.returns('test');
            this.view.onFocus();
            expect(_.defer).to.be.calledWith(this.view._applyTmpContent, 'test');
        });
    });

    describe('#_applyTmpContent', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Html2Text, 'html2text');
            this.sinon.stub(this.view, 'syncViewToData');
        });

        it('должен преобразовать содержимое в plain/text если редактор в режиме source', function() {
            this.view._applyTmpContent('test');
            expect(Daria.Html2Text.html2text).to.be.calledWith('test');
        });

        it('должен вставить введенный текст в редактор с переносом ранее заполненного текста на новую строку', function() {
            this.view.getEditor().mode = 'wysiwyg';
            this.sinon.stub(this.view.getEditor(), 'execCommand');

            this.view._applyTmpContent('test');
            expect(this.view.getEditor().execCommand).to.be.calledWith('pasteContent', { 'content': 'test', 'breakAfter': true });
        });

        it('должен синхронизировать данные редактора и модели', function() {
            this.view._applyTmpContent('test');
            expect(this.view.syncViewToData.callCount).to.be.equal(1);
        });
    });
});

