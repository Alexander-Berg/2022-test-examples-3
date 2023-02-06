describe('Daria.mStateMessageThreadList', function() {
  beforeEach(function() {
      this.model = ns.Model.get('state-message-thread-list');

      this.mMessage1 = ns.Model.get('message', {ids: '111'});
      this.sinon.stub(this.mMessage1, 'isNew').returns(false);
      this.mMessage2 = ns.Model.get('message', {ids: '222'});
      this.sinon.stub(this.mMessage2, 'isNew').returns(true);
      this.mMessage3 = ns.Model.get('message', {ids: '333'});
      this.sinon.stub(this.mMessage3, 'isNew').returns(false);

      var that = this;

      this.view1 = {
          getModel: function() {
              return that.mMessage1;
          }
      };
      this.view2 = {
          getModel: function() {
              return that.mMessage2;
          }
      };
      this.view3 = {
          getModel: function() {
              return that.mMessage3;
          }
      };
      this.hiddenMessages = [this.view1, this.view2, this.view3];
  });

  describe('#setHiddenMessages', function() {
      beforeEach(function() {
          this.sinon.stub(this.model, 'set');
      });

      it('Должен сохранить список скрытых сообщений', function() {
          this.model.setHiddenMessages(this.hiddenMessages);

          expect(this.model.set).to.be.calledWithExactly('.hiddenMessages', this.hiddenMessages);
      });

      it('Должен сохранить количество скрытых сообщений', function() {
          this.model.setHiddenMessages(this.hiddenMessages);

          expect(this.model.set).to.be.calledWithExactly('.countHiddenMessages', 3);
      });
  });

});

