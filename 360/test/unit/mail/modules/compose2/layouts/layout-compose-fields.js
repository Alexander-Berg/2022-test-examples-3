xdescribe('layout-compose-fields', function() {

    beforeEach(function() {
        this.mQuickReplyState = ns.Model.get('quick-reply-state');
    });

    it('должен вернуть плейсхолдер по умолчанию', function() {
        this.sinon.stub(this.mQuickReplyState, 'toShowForm').returns(true);
        var layout = ns.layout.page('layout-compose-fields');

        expect(layout).to.only.have.keys('compose-fields-box');
        expect(layout[ 'compose-fields-box' ].views).to.have.keys(
            'compose-field-placeholder'
        );
    });

    it('должен вернуть набор полей получателей, если плейсхолдер выключен', function() {
        this.sinon.stub(this.mQuickReplyState, 'get').withArgs('.showFieldPlaceholder').returns(false);
        this.sinon.stub(this.mQuickReplyState, 'toShowForm').returns(true);
        var layout = ns.layout.page('layout-compose-fields');

        expect(layout).to.only.have.keys('compose-fields-box');
        expect(layout[ 'compose-fields-box' ].views).to.have.keys(
            'compose-field-to',
            'compose-field-to-notices-wrapper',
            'compose-popular-contacts-wrapper',
            'compose-field-cc',
            'compose-field-cc-notices-wrapper',
            'compose-field-bcc',
            'compose-field-bcc-notices-wrapper',
            'compose-field-subject'
        );
    });

    it('должен вернуть набор полей получателей, если плейсхолдер включен, но QR выключен', function() {
        this.sinon.stub(this.mQuickReplyState, 'get').withArgs('.showFieldPlaceholder').returns(true);
        this.sinon.stub(this.mQuickReplyState, 'toShowForm').returns(false);
        var layout = ns.layout.page('layout-compose-fields');

        expect(layout).to.only.have.keys('compose-fields-box');
        expect(layout[ 'compose-fields-box' ].views).to.have.keys(
            'compose-field-to',
            'compose-field-to-notices-wrapper',
            'compose-popular-contacts-wrapper',
            'compose-field-cc',
            'compose-field-cc-notices-wrapper',
            'compose-field-bcc',
            'compose-field-bcc-notices-wrapper',
            'compose-field-subject'
        );
    });
});

