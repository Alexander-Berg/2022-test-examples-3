describe('Daria.vContactActions', function() {
    beforeEach(function() {
        this.mMessageBody = ns.Model.get('message-body', { ids: '123' });
        this.mMessageBody.setData({
            info: {
                field: [ {
                    email: 'test@ya.ru',
                    ref: '0b05c551e28a5d2a077961d8b6de20ee'
                } ]
            }
        });

        this.mAccountInformation = ns.Model.get('account-information');
        this.mEmailInfo = ns.Model.get('email-info-v2');

        this.sinon.stub(ns.Model, 'get')
            .returns({})
            .withArgs('message-body', { ids: '123' }).returns(this.mMessageBody);

        this.view = ns.View.create('contact-actions', {
            email: 'test@ya.ru',
            ids: '123'
        });

        this.sinon.stub(this.view, 'getModel')
            .withArgs('account-information').returns(this.mAccountInformation)
            .withArgs('email-info-v2').returns(this.mEmailInfo);
    });

    describe('#_getMenuItems', function() {
        beforeEach(function() {
            this.sinon.stub(this.mAccountInformation, 'isMyEmail');
            this.sinon.stub(this.mEmailInfo, 'getData').returns({});
            this.sinon.stub(this.view, '_getAbookContact').returns({});
            this.sinon.stub(ns.router, 'generateUrl').returns('');
            this.sinon.stub(Daria.Clipboard, 'canUse').value(true);

            this.sinon.stub(window, 'i18n')
                .withArgs('%Message_Написать_письмо').returns('Написать письмо')
                .withArgs('%Message_Перейти_на_Стафф').returns('Перейти на Стафф')
                .withArgs('%Скопировать_адрес').returns('Скопировать адрес')
                .withArgs('%Message_to_contacts').returns('В адресную книгу')
                .withArgs('%Message_to_blacklist').returns('В черный список');
        });

        describe('Email -> ', function() {
            it('В меню должен выводиться email на который кликнул пользователь', function() {
                expect(this.view._getMenuItems()).to.include({
                    content: this.view.params.email,
                    class: [ 'mail-ContactMenu-Item_selectonly' ],
                    attrs: {
                        'data-noclose': '1'
                    }
                });
            });
        });

        describe('Написать письмо -> ', function() {
            it('Меню должно содержать пункт "Написать письмо", если пользователь не нажал на самого себя', function() {
                expect(this.view._getMenuItems()).to.include({
                    content: 'Написать письмо',
                    attrs: {
                        'data-item-id': 'compose',
                        href: ''
                    }
                });
            });

            it('Меню не должно содержать пункт "Написать письмо", если пользователь нажал на самого себя', function() {
                this.mAccountInformation.isMyEmail.returns(true);

                expect(this.view._getMenuItems()).to.not.include({
                    content: 'Написать письмо',
                    attrs: {
                        'data-item-id': 'compose',
                        href: ''
                    }
                });
            });
        });

        describe('Перейти на Стафф -> ', function() {
            it('На Корпе меню должно содержать пункт "Перейти на Стафф", если пользователь кликнул на email из yandex-team и удалось загрузить данные', function() {
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.sinon.stub(Daria, 'Config').value({ 'staff-host': 'staff.ru' })

                this.mEmailInfo.getData.returns({
                    type: 'staff',
                    staff: {
                        login: 'test'
                    }
                });

                expect(this.view._getMenuItems()).to.include({
                    content: 'Перейти на Стафф',
                    attrs: {
                        href: 'staff.ru/test',
                        target: '_blank',
                        rel: 'noopener noreferrer'
                    }
                });
            });

            it('На БП меню не должно содержать пункт "Перейти на Стафф"', function() {
                this.sinon.stub(Daria, 'IS_CORP').value(false);

                expect(this.view._getMenuItems()).to.not.include({
                    content: 'Перейти на Стафф',
                    attrs: {
                        href: 'staff.yandex-team.ru/test',
                        target: '_blank',
                        rel: 'noopener noreferrer'
                    }
                });
            });

            it('Меню не должно содержать пункт "Перейти на Стафф", если пользователь кликнул на email не из yandex-team', function() {
                this.sinon.stub(Daria, 'IS_CORP').value(true);

                this.mEmailInfo.getData.returns({
                    type: 'social'
                });

                expect(this.view._getMenuItems()).to.not.include({
                    content: 'Перейти на Стафф',
                    attrs: {
                        href: 'staff.yandex-team.ru/test',
                        target: '_blank',
                        rel: 'noopener noreferrer'
                    }
                });
            });

            it('Меню не должно содержать пункт "Перейти на Стафф", если не удалось загрузить данные', function() {
                this.sinon.stub(Daria, 'IS_CORP').value(true);

                this.mEmailInfo.getData.returns({
                    type: 'staff',
                    staff: {
                        _error: true
                    }
                });

                expect(this.view._getMenuItems()).to.not.include({
                    content: 'Перейти на Стафф',
                    attrs: {
                        href: 'staff.yandex-team.ru/test',
                        target: '_blank',
                        rel: 'noopener noreferrer'
                    }
                });
            });
        });

        describe('Скопировать адрес -> ', function() {
            it('Меню должно содержать пункт "Скопировать адрес"', function() {
                expect(this.view._getMenuItems()).to.include({
                    content: 'Скопировать адрес',
                    attrs: {
                        'data-item-id': 'copy'
                    },
                    class: [ 'js-clipboard' ]
                });
            });

            it('Пункт "Скопировать адрес" должен быть задизейблин, если нет флеш-плеера v9', function() {
                Daria.Clipboard.canUse = false;

                expect(this.view._getMenuItems()).to.include({
                    content: 'Скопировать адрес',
                    attrs: {
                        'data-item-id': 'copy'
                    },
                    class: [ 'mail-ContactMenu-Item_disabled' ]
                });
            });
        });

        describe('В адресную книгу -> ', function() {
            it('Меню должно содержать пункт "В адресную книгу", если пользователь не нажал на самого себя и этого контакта еще нет', function() {
                var menuContent = _.map(this.view._getMenuItems(), 'content');
                expect(menuContent).to.include('В адресную книгу');
            });

            it('Меню не должно содержать пункт "В адресную книгу", если пользователь нажал на самого себя', function() {
                this.mAccountInformation.isMyEmail.returns(true);

                var menuContent = _.map(this.view._getMenuItems(), 'content');
                expect(menuContent).to.not.include('В адресную книгу');
            });

            it('Меню не должно содержать пункт "В адресную книгу", если контакт уже есть', function() {
                this.mAccountInformation.isMyEmail.returns(true);
                this.view._getAbookContact.returns({ cid: '100500' });

                var menuContent = _.map(this.view._getMenuItems(), 'content');
                expect(menuContent).to.not.include('В адресную книгу');
            });
        });

        describe('В черный список -> ', function() {
            it('Меню должно содержать пункт "В черный список", если пользователь не нажал на самого себя', function() {
                var menuContent = _.map(this.view._getMenuItems(), 'content');
                expect(menuContent).to.include('В черный список');
            });

            it('Меню не должно содержать пункт "В черный список", если пользователь нажал на самого себя', function() {
                this.mAccountInformation.isMyEmail.returns(true);

                var menuContent = _.map(this.view._getMenuItems(), 'content');
                expect(menuContent).to.not.include('В черный список');
            });
        });
    });
});
