xdescribe('layout-compose-field-to-extras', function() {
    beforeEach(function() {
        this.mQuickReplyState = ns.Model.get('quick-reply-state');
        this.sinon.stub(this.mQuickReplyState, 'toShowForm');

        this.mComposeState = ns.Model.get('compose-state');
        this.sinon.stub(this.mComposeState, 'isFieldVisible');
    });

    describe('Большой композ', function() {
        beforeEach(function() {
            this.mQuickReplyState.toShowForm.returns(false);
        });

        it('должно показать кнопку "Копия", если показываем в большой композе и поле скрыто', function() {
            this.mComposeState.isFieldVisible.withArgs('cc').returns(false);
            var layout = ns.layout.page('layout-compose-field-to-extras');

            expect(layout).to.only.have.keys('compose-field-to-extras-box');
            expect(layout[ 'compose-field-to-extras-box' ].views).to.have.keys('compose-field-to-extras-cc');
        });

        it('должно скрыть кнопку "Копия", если показываем большой композ и поле показано', function() {
            this.mComposeState.isFieldVisible.withArgs('cc').returns(true);
            var layout = ns.layout.page('layout-compose-field-to-extras');

            expect(layout).to.only.have.keys('compose-field-to-extras-box');
            expect(layout[ 'compose-field-to-extras-box' ].views).not.have.keys('compose-field-to-extras-cc');
        });

        it('должно показать кнопку "Скрытая Копия", если показываем в большой композе и поле скрыто', function() {
            this.mComposeState.isFieldVisible.withArgs('bcc').returns(false);
            var layout = ns.layout.page('layout-compose-field-to-extras');

            expect(layout).to.only.have.keys('compose-field-to-extras-box');
            expect(layout[ 'compose-field-to-extras-box' ].views).to.have.keys('compose-field-to-extras-bcc');
        });

        it('должно скрыть кнопку "Скрытая Копия", если показываем большой композ и поле показано', function() {
            this.mComposeState.isFieldVisible.withArgs('bcc').returns(true);
            var layout = ns.layout.page('layout-compose-field-to-extras');

            expect(layout).to.only.have.keys('compose-field-to-extras-box');
            expect(layout[ 'compose-field-to-extras-box' ].views).not.have.keys('compose-field-to-extras-bcc');
        });
    });

    describe('QuickReply', function() {
        beforeEach(function() {
            this.mQuickReplyStateGet = this.sinon.stub(this.mQuickReplyState, 'get');
            this.mQuickReplyState.toShowForm.returns(true);
        });

        describe('не показываем плейсхолдер полей', function() {
            beforeEach(function() {
                this.mQuickReplyStateGet.withArgs('.showFieldPlaceholder').returns(false);
            });

            it('должно показать кнопку "Копия", если показываем в большой композе и поле скрыто', function() {
                this.mComposeState.isFieldVisible.withArgs('cc').returns(false);
                var layout = ns.layout.page('layout-compose-field-to-extras');

                expect(layout).to.only.have.keys('compose-field-to-extras-box');
                expect(layout[ 'compose-field-to-extras-box' ].views).to.have.keys('compose-field-to-extras-cc');
            });

            it('должно скрыть кнопку "Копия", если показываем большой композ и поле показано', function() {
                this.mComposeState.isFieldVisible.withArgs('cc').returns(true);
                var layout = ns.layout.page('layout-compose-field-to-extras');

                expect(layout).to.only.have.keys('compose-field-to-extras-box');
                expect(layout[ 'compose-field-to-extras-box' ].views).not.have.keys('compose-field-to-extras-cc');
            });

            it('должно показать кнопку "Скрытая Копия", если показываем в большой композе и поле скрыто', function() {
                this.mComposeState.isFieldVisible.withArgs('bcc').returns(false);
                var layout = ns.layout.page('layout-compose-field-to-extras');

                expect(layout).to.only.have.keys('compose-field-to-extras-box');
                expect(layout[ 'compose-field-to-extras-box' ].views).to.have.keys('compose-field-to-extras-bcc');
            });

            it('должно скрыть кнопку "Скрытая Копия", если показываем большой композ и поле показано', function() {
                this.mComposeState.isFieldVisible.withArgs('bcc').returns(true);
                var layout = ns.layout.page('layout-compose-field-to-extras');

                expect(layout).to.only.have.keys('compose-field-to-extras-box');
                expect(layout[ 'compose-field-to-extras-box' ].views).not.have.keys('compose-field-to-extras-bcc');
            });
        });

        describe('показываем плейсхолдер полей', function() {
            beforeEach(function() {
                this.mQuickReplyStateGet.withArgs('.showFieldPlaceholder').returns(true);
            });

            it('должно показать кнопку "Копия", если показываем в большой композе и поле скрыто', function() {
                var layout = ns.layout.page('layout-compose-field-to-extras');

                expect(layout).to.only.have.keys('compose-field-to-extras-box');
                expect(layout[ 'compose-field-to-extras-box' ].views).to.only.have.keys(
                    'compose-field-to-extras-go-to-compose',
                    'compose-field-to-extras-qr-close'
                );
            });
        });
    });
});

