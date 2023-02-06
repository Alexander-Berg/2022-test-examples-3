xdescribe('layout-compose-field-notices', function() {
    beforeEach(function() {
        this.mComposeState = ns.Model.get('compose-state');
        this.mComposeMessage = ns.Model.get('compose-message');

        this.sinon.stub(this.mComposeState, 'isReplyAllNoticeVisible').returns(false);
        this.sinon.stub(this.mComposeState, 'isNdaNoticeVisible').returns(false);
        this.sinon.stub(this.mComposeMessage, 'getFieldError').returns(undefined);
    });

    it('Не должен вернуть никаких видов', function() {
        var layout = ns.layout.page('layout-compose-field-to-notices');

        expect(layout[ 'compose-field-to-notices-box' ].views).to.be.eql({});
    });

    it('Должен вернуть вид переключение на "Ответить всем", если ответ одному', function() {
        this.mComposeState.isReplyAllNoticeVisible.returns(true);

        var layout = ns.layout.page('layout-compose-field-to-notices');

        expect(layout[ 'compose-field-to-notices-box' ].views).to.only.have.keys('compose-field-to-notice-replyall');
    });

    it('Должен вернуть вид с ошибкой', function() {
        this.mComposeMessage.getFieldError.returns('aaa');

        var layout = ns.layout.page('layout-compose-field-to-notices');

        expect(layout[ 'compose-field-to-notices-box' ].views).to.only.have.keys('compose-field-to-error');
    });

    it('Должен вернуть вид с сообщением о наличии контактов из внешней сети', function() {
        this.mComposeState.isNdaNoticeVisible.returns(true);

        var layout = ns.layout.page('layout-compose-field-to-notices');

        expect(layout[ 'compose-field-to-notices-box' ].views).to.only.have.keys('compose-field-to-notice-nda');
    });

    it('Должен вернуть все нотисы для поля to', function() {
        this.mComposeState.isReplyAllNoticeVisible.returns(true);
        this.mComposeMessage.getFieldError.returns('aaa');
        this.mComposeState.isNdaNoticeVisible.returns(true);

        var layout = ns.layout.page('layout-compose-field-to-notices');

        expect(layout[ 'compose-field-to-notices-box' ].views).to.only.have.keys(
            'compose-field-to-notice-nda',
            'compose-field-to-error',
            'compose-field-to-notice-replyall'
        );
    });
});

