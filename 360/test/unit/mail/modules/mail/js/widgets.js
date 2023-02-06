/* eslint max-len: off */
describe('Daria.Widgets', function() {
    after(function() {
        Daria.Config.locale = 'ru';
    });

    describe('.shouldShowTaksaWidgets', function() {
        beforeEach(function() {
            this.mSettings = ns.Model.get('settings');
            this.mMessage = ns.Model.get('message', { ids: 'widget-ids' });
        });

        describe('Если нет в выдаче message поля widgets', function() {
            beforeEach(function() {
                this.mMessage.setData({});
            });
            it('не показываем сразу', function() {
                expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(false);
            });
        });
        describe('Если выдача в message про виджет есть', function() {
            describe('Любой тип виджета - основные условия для всех априори', function() {
                beforeEach(function() {
                    this.mMessage = ns.Model.get('message', { ids: 'widget-ids' }).setData({
                        widgets: [
                            {
                                info: {
                                    type: 'imglink',
                                    showType: 'list'
                                }
                            }
                        ]
                    });
                });
                it('Если основные условия выполнены - виджет показан #1', function() {
                    this.sinon.stub(Daria.Config, 'locale').value('ru');
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });

                it('Если основные условия выполнены - виджет показан #2', function() {
                    this.sinon.stub(Daria.Config, 'locale').value('ru');
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: true,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });

                it('Если одно из основных условий не выполняется - виджет не покажется #1', function() {
                    this.sinon.stub(Daria.Config, 'locale').value('en');
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(false);
                });

                it('Если одно из основных условий не выполняется - виджет не покажется #2', function() {
                    this.sinon.stub(Daria.Config, 'locale').value('ru');
                    this.sinon.stub(Daria, 'is3pane').returns(true);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(false);
                });

                it('Если одно из основных условий не выполняется - виджет не покажется #3', function() {
                    this.sinon.stub(Daria.Config, 'locale').value('ru');
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(true);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(false);
                });

                it('Если одно из основных условий не выполняется - виджет не покажется #4', function() {
                    this.sinon.stub(Daria.Config, 'locale').value('ru');
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(true);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: false
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(false);
                });
            });

            describe('Виджет типа tracker - обязательные дополнительные условия для этого виджета', function() {
                beforeEach(function() {
                    this.mMessage = ns.Model.get('message', { ids: 'widget-ids' }).setData({
                        widgets: [
                            {
                                info: {
                                    type: 'tracker',
                                    showType: 'list'
                                }
                            }
                        ]
                    });
                });
                it('Если основные условия выполнены - виджет показан #1', function() {
                    Daria.Config.locale = 'ru';
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });
                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });

                it('Если основные условия выполнены - виджет показан #2', function() {
                    Daria.Config.locale = 'ru';
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: true,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });

                it('Если основные условия выполнены - виджет показан #3', function() {
                    Daria.Config.locale = 'en';
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });

                it('Если основные условия выполнены - виджет показан #4', function() {
                    Daria.Config.locale = 'en';
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(true);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });
            });

            describe('Виджет типа onelink - обязательные дополнительные условия для этого виджета', function() {
                beforeEach(function() {
                    this.mMessage = ns.Model.get('message', { ids: 'widget-ids' }).setData({
                        widgets: [
                            {
                                info: {
                                    type: 'onelink',
                                    showType: 'list'
                                }
                            }
                        ]
                    });
                });
                it('Если основные условия выполнены - виджет показан #1', function() {
                    Daria.Config.locale = 'ru';
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });
                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });

                it('Если основные условия выполнены - виджет показан #2', function() {
                    Daria.Config.locale = 'ru';
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: true,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });

                it('Если основные условия выполнены - виджет показан #3', function() {
                    Daria.Config.locale = 'en';
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(true);
                });

                it('Если одно из основных условий не выполнено - виджет не покажется #1', function() {
                    Daria.Config.locale = 'en';
                    this.sinon.stub(Daria, 'is3pane').returns(false);
                    this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(true);
                    this.mSettings.setData({
                        show_widgets_decor: false,
                        show_widgets_buttons: true
                    });

                    expect(Daria.Widgets.shouldShowTaksaWidgets(this.mMessage)).to.equal(false);
                });
            });
        });
    });

    describe('._commonChecks', function() {
        beforeEach(function() {
            this.mFolders = ns.Model.get('folders');
            this.sinon.stub(this.mFolders, 'spamOrTrash');

            this.mSettings = ns.Model.get('settings');

            this.mMessage = ns.Model.get('message', { ids: 'widget-ids' });
        });

        it('должен вернуть true, если spamOrTrash=false', function() {
            this.mFolders.spamOrTrash.returns(false);

            expect(Daria.Widgets._commonChecks(this.mMessage)).to.be.equal(true);
        });

        it('должен вернуть false, если spamOrTrash=true', function() {
            this.mFolders.spamOrTrash.returns(true);

            expect(Daria.Widgets._commonChecks(this.mMessage)).to.be.equal(false);
        });
    });

    describe('.shouldShowAttachments', function() {
        beforeEach(function() {
            // делаем условия для показа
            this.mFolders = ns.Model.get('folders');
            setModelByMock(this.mFolders);

            this.mMessage = ns.Model.get('message', { ids: '1' }).setData({
                fid: this.mFolders.getFidBySymbol('inbox'),
                flags: {
                    attachment: true
                },
                mid: '1',
                tid: 't1',
                type: [ 4 ]
            });
            this.sinon.spy(this.mMessage, 'getSOLabels');

            this.mSettings = ns.Model.get('settings').setData({
                disable_inboxattachs: false
            });
            this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);

            this.sinon.stub(Daria, 'is2pane').returns(true);
        });

        it('должен вернуть true, если все условия выполнены', function() {
            Daria.is2pane.returns(true);
            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(true);
        });

        it('должен вернуть false, если все условия выполнены, но компактный режим', function() {
            Daria.CompactMode.isCompactMessagesList.returns(true);
            Daria.is2pane.returns(true);
            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(false);
        });

        it('должен вернуть false, если все условия выполнены, компактный режим, но выборка "все аттачи"', function() {
            Daria.CompactMode.isCompactMessagesList.returns(true);
            Daria.is2pane.returns(true);
            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, { extra_cond: 'only_atta' })).to.be.equal(true);
        });

        it('должен вернуть false для 3pane', function() {
            Daria.is2pane.returns(false);
            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(false);
        });

        it('должен вернуть false если 2pane и нет аттачей', function() {
            this.sinon.stub(this.mMessage, 'hasAttachment').returns(false);
            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(false);
        });

        it('должен вернуть false если 2pane, есть аттачи, но выключен виджет', function() {
            this.mSettings.set('.disable_inboxattachs', true);
            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(false);
        });

        describe('Не показываем аттачи в определенных папках ->', function() {
            [ 'draft', 'sent', 'spam', 'template', 'trash' ].forEach(function(symbol) {
                it('должен вернуть false, если все условия выполнены, но письмо в "' + symbol + '"', function() {
                    this.mMessage.set('.fid', this.mFolders.getFidBySymbol(symbol));
                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(false);
                });
            });

            [ 'spam', 'template', 'trash' ].forEach(function(symbol) {
                it('должен вернуть false, если все условия выполнены, но письмо в "' + symbol + '" даже для выборки только аттачи', function() {
                    this.mMessage.set('.fid', this.mFolders.getFidBySymbol(symbol));
                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, { extra_cond: 'only_atta' })).to.be.equal(false);
                });
            });

            [ 'draft', 'sent' ].forEach(function(symbol) {
                it('должен вернуть true, если все условия выполнены, письмо в "' + symbol + '" и выборка только аттачи', function() {
                    this.mMessage.set('.fid', this.mFolders.getFidBySymbol(symbol));
                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, { extra_cond: 'only_atta' })).to.be.equal(true);
                });
            });

            [ 'draft', 'sent' ].forEach(function(symbol) {
                it('должен вернуть true, если все условия выполнены, письмо в "' + symbol + '" и письмо из треда', function() {
                    this.mMessage.set('.fid', this.mFolders.getFidBySymbol(symbol));
                    this.sinon.stub(this.mMessage, 'isIntoThread').returns(true);
                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(true);
                });
            });

            it('Показываем аттачи в Шаблонах, если включен дизайн с полоской аттачей ->', function() {
                this.sinon.stub(Daria, 'isAttachmentsLineDesign').returns(true);
                this.mMessage.set('.fid', this.mFolders.getFidBySymbol('template'));
                expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(true);
            });
        });

        describe('Поиск с вложениями ->', function() {
            beforeEach(function() {
                this.params4SearchWithAttaches = { search: 'search', attaches: 'yes' };
            });

            describe('обычный режим ->', function() {
                beforeEach(function() {
                    Daria.CompactMode.isCompactMessagesList.returns(false);
                });

                it('должен вернуть true, если 2pane', function() {
                    Daria.is2pane.returns(true);

                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, this.params4SearchWithAttaches)).to.be.equal(true);
                });

                it('должен вернуть true, если 2pane, письмо из отправленных', function() {
                    Daria.is2pane.returns(true);
                    this.mMessage.set('.fid', this.mFolders.getFidBySymbol('sent'));

                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, this.params4SearchWithAttaches)).to.be.equal(true);
                });

                it('должен вернуть false, если 3pane', function() {
                    Daria.is2pane.returns(false);

                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, this.params4SearchWithAttaches)).to.be.equal(false);
                });
            });

            describe('компактный режим ->', function() {
                beforeEach(function() {
                    Daria.CompactMode.isCompactMessagesList.returns(true);
                });

                it('должен вернуть true, если 2pane', function() {
                    Daria.is2pane.returns(true);

                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, this.params4SearchWithAttaches)).to.be.equal(true);
                });

                it('должен вернуть true, если 2pane, письмо из отправленных', function() {
                    Daria.is2pane.returns(true);
                    this.mMessage.set('.fid', this.mFolders.getFidBySymbol('sent'));

                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, this.params4SearchWithAttaches)).to.be.equal(true);
                });

                it('должен вернуть false, если 3pane', function() {
                    Daria.is2pane.returns(false);

                    expect(Daria.Widgets.shouldShowAttachments(this.mMessage, this.params4SearchWithAttaches)).to.be.equal(false);
                });
            });
        });

        it('возвращает false, если все условия выполнены, но письмо от календаря', function() {
            this.mMessage.set('.dlid', 'yacal');
            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(false);
        });

        it('возвращает true, если все условия выполнены, письмо от календаря, но выборка "только аттачи"', function() {
            this.mMessage.set('.dlid', 'yacal');
            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, { extra_cond: 'only_atta' })).to.be.equal(true);
        });

        it('возвращает true, если мы в корпе', function() {
            this.mMessage.set('.type', []);
            this.sinon.stub(Daria, 'IS_CORP').value(true);

            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(true);
            expect(this.mMessage.getSOLabels).to.have.callCount(0);
        });

        // Не показываем виджет аттачей для рассылок - type=13 (news).
        // https://st.yandex-team.ru/DARIA-54397#1514476014000
        it('возвращает false, если все условия выполнены, но есть тип 13', function() {
            this.mMessage.set('.type', [ 13 ]);
            this.sinon.stub(Daria, 'IS_CORP').value(false);

            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(false);
            expect(this.mMessage.getSOLabels).to.have.callCount(1);
        });

        it('возвращает true, если все условия выполнены и нет типа 13', function() {
            this.mMessage.set('.type', [ 2 ]);
            this.sinon.stub(Daria, 'IS_CORP').value(false);

            expect(Daria.Widgets.shouldShowAttachments(this.mMessage, {})).to.be.equal(true);
            expect(this.mMessage.getSOLabels).to.have.callCount(1);
        });
    });
});
