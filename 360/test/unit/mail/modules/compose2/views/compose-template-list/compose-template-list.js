describe('Daria.vComposeTemplateList', function() {
  beforeEach(function() {
      this.mid = '123';
      this.view = ns.View.create('compose-template-list');
      this.mComposeMessage = ns.Model.get('compose-message');
      this.mComposeFsm = ns.Model.get('compose-fsm');
      this.mMessage = ns.Model.get('message', {ids: this.mid});
      this.mFolders = ns.Model.get('folders');

      var getModelStub = this.sinon.stub(this.view, 'getModel');
      getModelStub.withArgs('compose-message').returns(this.mComposeMessage);
      getModelStub.withArgs('message').returns(this.mMessage);
      getModelStub.withArgs('compose-fsm').returns(this.mComposeFsm);
  });

  describe('Инициализация', function() {
      it('Если есть директория шаблонов, то в параметрах вида есть current_folder и есть allow_empty', function() {
          this.sinon.stub(this.mFolders, 'getFidBySymbol').withArgs('template').returns('11');

          var view = ns.View.create('compose-template-list');
          expect(view.params).to.be.eql({ current_folder: '11', allow_empty: true });
      });
  });

  describe('#onTemplateClick', function() {
      beforeEach(function() {
          this.sinon.stub(this.mComposeMessage, 'isTemplate');
          this.sinon.stub(this.mComposeMessage, 'isDraft');
          this.sinon.stub(this.mComposeMessage, 'isReplyAny');
          this.sinon.stub(this.mComposeMessage, 'isForward');
          this.event = {
              currentTarget: '<div data-mid="' + this.mid + '"></div>'
          };

          this.sinon.stub(ns.router, 'generateUrl').withArgs('compose2').returns('compose?ids=' + this.mid);
          this.sinon.stub(ns.page, 'go');
          this.thenStub = this.sinon.stub();
          this.sinon.stub(ns, 'request').withArgs([ 'message', 'message-body' ]).returns({then: this.thenStub});
      });

      it('Должен перейти в композ с новым шаблоном, если в композе шаблон', function() {
          this.mComposeMessage.isDraft.returns(false);
          this.mComposeMessage.isReplyAny.returns(false);
          this.mComposeMessage.isForward.returns(false);
          this.view.onTemplateClick(this.event);
          expect(ns.page.go).to.be.calledWithExactly('compose?ids=' + this.mid);
      });

      it('Должен запросить сообщение и тело для шаблона, если в композе не шаблон', function() {
          this.mComposeMessage.isDraft.returns(true);
          this.sinon.stub(this.view.onMessageBodyRequestSuccess, 'bind').returns(this.view.onMessageBodyRequestSuccess);
          this.view.onTemplateClick(this.event);
          expect(this.thenStub).to.be.calledWithExactly(this.view.onMessageBodyRequestSuccess);
      });
  });

  describe('#onMessageBodyRequestSuccess', function() {
      beforeEach(function() {
          this.mMessage = ns.Model.get('message', {ids: this.mid});
          this.mMessageBody = ns.Model.get('message-body', {ids: this.mid});
          this.sinon.stub(this.mComposeMessage, 'getAllRecepients');
          this.sinon.stub(this.mComposeMessage, 'addTextFromMessageBody');
          this.sinon.stub(this.mComposeMessage, 'setRecepientsFromMessageBody');
          this.sinon.stub(this.mComposeMessage, 'set').withArgs('.subj');
          this.sinon.stub(this.mComposeMessage, 'get').withArgs('.subj');
          this.sinon.stub(this.mMessage, 'get').withArgs('.subject');
      });

      it('Должен добавить шаблон к телу письма', function() {
          this.mComposeMessage.getAllRecepients.returns([]);
          this.view.onMessageBodyRequestSuccess(this.mComposeMessage, [ this.mMessage, this.mMessageBody ]);
          expect(this.mComposeMessage.addTextFromMessageBody).to.be.calledWithExactly(this.mMessageBody);
      });

      it('Должен подставить адресатов из шаблона, если в композе нет заполнены адресаты', function() {
          this.mComposeMessage.getAllRecepients.returns([]);
          this.view.onMessageBodyRequestSuccess(this.mComposeMessage, [ this.mMessage, this.mMessageBody ]);
          expect(this.mComposeMessage.setRecepientsFromMessageBody).to.be.calledWithExactly(this.mMessageBody);
      });

      it('Не должен подставлять адресатов из шаблона, если в композе заполнены', function() {
          this.mComposeMessage.getAllRecepients.returns(['aa@mail']);
          this.view.onMessageBodyRequestSuccess(this.mComposeMessage, [ this.mMessage, this.mMessageBody ]);
          expect(this.mComposeMessage.setRecepientsFromMessageBody).to.have.callCount(0);
      });

      it('Должен заполнить тему из шаблона, если в композе не заполнена тема', function() {
          this.mComposeMessage.getAllRecepients.returns(['aa@mail']);
          this.mComposeMessage.get.withArgs('.subj').returns('');
          this.mMessage.get.withArgs('.subject').returns('template subject');
          this.view.onMessageBodyRequestSuccess(this.mComposeMessage, [ this.mMessage, this.mMessageBody ]);
          expect(this.mComposeMessage.set).to.be.calledWith('.subj', 'template subject');
      });

      it('Не должен заполнять тему из шаблона, если в композе уже заполнена тема', function() {
          this.mComposeMessage.getAllRecepients.returns(['aa@mail']);
          this.mComposeMessage.get.withArgs('.subj').returns('compose subject');
          this.view.onMessageBodyRequestSuccess(this.mComposeMessage, [ this.mMessage, this.mMessageBody ]);
          expect(this.mComposeMessage.set).to.have.callCount(0);
      });

  });

  describe('#onCreateTemplateClick', function() {
      beforeEach(function() {
          this.sinon.stub(ns.router, 'generateUrl')
              .withArgs('compose2', { save_symbol: 'template' })
              .returns('compose?save_symbol=template');
          this.sinon.stub(ns.page, 'go').returns(Promise.resolve());
      });

      it('Должен перейти на пустую страницу композа с save_symbol=template', function() {
          this.sinon.stub(this.mComposeMessage, 'getData').returns({});
          this.sinon.stub(this.mComposeMessage, 'isDraft').returns(true);

          this.view.onCreateTemplateClick();

          expect(ns.page.go).to.be.calledWith('compose?save_symbol=template');
      });
  });

  describe('#onSaveTemplateClick', function() {
      beforeEach(function() {
          this.sinon.stub(this.mComposeFsm, 'setState');
          this.sinon.stub(this.mComposeMessage, 'set').withArgs('.save_symbol');
      });

      it('Должен выставить сообщению композа save_symbol=template и перевести состояние в save', function() {
          this.view.onSaveTemplateClick();
          expect(this.mComposeMessage.set).to.be.calledWith('.save_symbol', 'template');
          expect(this.mComposeFsm.setState).to.be.calledWith('save');
      });
  });
});
