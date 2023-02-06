describe('Daria.mHelpLink', function() {
    beforeEach(function() {
        this.sinon.stub(ns.page, 'current').value({
            page: 'some_page',
            params: {}
        });

        this.mHelpLink = ns.Model.get('help-link');
        setModelByMock(ns.Model.get('folders'));
    });

    describe('#_onInit', function() {
        beforeEach(function() {
            this.sinon.stub(this.mHelpLink, '_getHref').returns('context_link');
        });

        it('должен подписываться на переходы и ставить контекстную ссылку, если продукт INT', function() {
            this.sinon.stub(ns.events, 'on');
            this.sinon.stub(Daria.Config, 'product').value('INT');
            this.sinon.stub(Daria.Config, 'yandex-domain').value('yandex.com');

            this.mHelpLink._onInit();

            expect(this.mHelpLink.get('.href')).to.be.equal('context_link');
            expect(ns.events.on).to.have.callCount(1);
        });

        it('должен подписываться на переходы и ставить контекстную ссылку, если продукт TUR', function() {
            this.sinon.stub(ns.events, 'on');
            this.sinon.stub(Daria.Config, 'product').value('TUR');
            this.sinon.stub(Daria.Config, 'yandex-domain').value('yandex.com.tr');

            this.mHelpLink._onInit();

            expect(this.mHelpLink.get('.href')).to.be.equal('context_link');
            expect(ns.events.on).to.have.callCount(1);
        });

        it('должен подписываться на переходы и ставить контекстную ссылку, если домен ua', function() {
            this.sinon.stub(ns.events, 'on');
            this.sinon.stub(Daria.Config, 'product').value('RUS');
            this.sinon.stub(Daria.Config, 'yandex-domain').value('yandex.ua');

            this.mHelpLink._onInit();

            expect(this.mHelpLink.get('.href')).to.be.equal('context_link');
            expect(ns.events.on).to.have.callCount(1);
        });

        it('должен подписаться на переходы и поставить контекстную ссылку', function() {
            this.sinon.stub(ns.events, 'on');
            this.sinon.stub(Daria.Config, 'product').value('RUS');
            this.sinon.stub(Daria.Config, 'yandex-domain').value('yandex.ru');

            this.mHelpLink._onInit();

            expect(this.mHelpLink.get('.href')).to.be.equal('context_link');
            expect(ns.events.on).to.have.callCount(1);
        });
    });

    describe('#updateHref', function() {
        beforeEach(function() {
            this.sinon.stub(this.mHelpLink, '_getHref').returns('link');
        });

        it('Должен обновить ссылку', function() {
            expect(this.mHelpLink.get('.href')).to.be.equal(Daria.Config['help-url']);

            this.mHelpLink.updateHref();

            expect(this.mHelpLink.get('.href')).to.be.equal('link');
        });
    });

    describe('#_getHref', function() {
        beforeEach(function() {
            this.sinon.stub(this.mHelpLink, '_getHrefSuffix').returns('inbox.html');
        });

        it('Должен вернуть правильную ссылку', function() {
            expect(this.mHelpLink._getHref({})).to.be.equal(Daria.Config['help-url'] + '/inbox.html');
        });
    });

    describe('#_onPageBeforeLoad', function() {
        beforeEach(function() {
            this.sinon.stub(this.mHelpLink, 'updateHref');
        });

        it('не должен вызывать апдейт ссылки, если страница не поменяется', function() {
            this.mHelpLink._onPageBeforeLoad('ns-page-before-load', [
                {
                    page: 'messages',
                    params: {
                        current_folder: '1'
                    }
                },
                {
                    page: 'messages',
                    params: {
                        current_folder: '1'
                    }
                }
            ]);

            expect(this.mHelpLink.updateHref).to.have.callCount(0);
        });

        it('должен вызвать апдейт ссылки, если страница поменяется', function() {
            this.mHelpLink._onPageBeforeLoad('ns-page-before-load', [
                {
                    page: 'messages',
                    params: {
                        current_folder: '1'
                    }
                },
                {
                    page: 'messages',
                    params: {
                        current_folder: '2'
                    }
                }
            ]);

            expect(this.mHelpLink.updateHref).to.have.callCount(1);
        });
    });

    describe('#_getHrefSuffix', function() {
        describe('письмо ->', function() {
            it('должен вернуть пустой суффикс, если открыто письмо', function() {
                expect(this.mHelpLink._getHrefSuffix('неважно какая страница', {
                    ids: '111'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс, если открыт тред', function() {
                expect(this.mHelpLink._getHrefSuffix('неважно какая страница', {
                    thread_id: '111'
                }))
                    .to.be.equal('');
            });
        });

        describe('папки ->', function() {
            it('должен вернуть пустой суффикс для входящих', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '1'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть суффикс для архива', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '3'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap.archive)
                    .and.not.equal(undefined);
            });

            it('должен вернуть пустой суффикс для отправленных', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '8'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для удаленных', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '7'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для спама', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '2'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть суффикс для черновиков', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '4'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap.draft)
                    .and.not.equal(undefined);
            });

            it('должен вернуть суффикс для шаблонов', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '6'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap.template)
                    .and.not.equal(undefined);
            });

            it('должен вернуть пустой суффикс для исходящих', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '9'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть суффикс для пользовательской папки', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    current_folder: '11'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap.folder)
                    .and.not.equal(undefined);
            });
        });

        describe('поиск ->', function() {
            it('должен вернуть пустой суффикс для поиска, если находимся на странице поисковой выдачи', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    search: 'yes'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для поиска, если находимся на странице поиска', function() {
                expect(this.mHelpLink._getHrefSuffix('search', {}))
                    .to.be.equal('');
            });
        });

        describe('фильтры ->', function() {
            it('должен вернуть суффикс для "Важные"', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    important: 'important'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap.important)
                    .and.not.equal(undefined);
            });

            it('должен вернуть суффикс для "Непрочитанные"', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    extra_cond: 'only_new'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap.unread)
                    .and.not.equal(undefined);
            });

            it('должен вернуть суффикс для "С вложениями"', function() {
                expect(this.mHelpLink._getHrefSuffix('messages', {
                    extra_cond: 'only_atta'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap.attachments)
                    .and.not.equal(undefined);
            });
        });

        describe('настройки ->', function() {
            it('должен вернуть пустой суффикс для настроек', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {}))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек отправителя', function() {
                expect(this.mHelpLink._getHrefSuffix('setup-sender', {
                    tab: 'sender'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек сборщиков', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'collectors'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек папок и меток', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'folders'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек фильтров', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'filters'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек создания фильтров', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'filters-create'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек фильтра "перемещать письма в отдельную папку"', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'filters-create-simple'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек фильтра "отмечать письма определённой меткой"', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'filters-create-simple',
                    action: 'label'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек фильтра "удалять ненужные письма при получении"', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'filters-create-simple',
                    action: 'delete'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть пустой суффикс для настроек безопасности', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'security'
                }))
                    .to.be.equal('');
            });

            it('должен вернуть суффикс для настроек журнала', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'journal'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap['setup/journal'])
                    .and.not.equal(undefined);
            });

            it('должен вернуть суффикс для настроек абука', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'abook'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap['setup/abook'])
                    .and.not.equal(undefined);
            });

            it('должен вернуть суффикс для настроек дел', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'todo'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap['setup/todo'])
                    .and.not.equal(undefined);
            });

            it('должен вернуть суффикс для настроек клиента', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'client'
                }))
                    .to.be.equal(this.mHelpLink._hrefSuffixMap['setup/client'])
                    .and.not.equal(undefined);
            });

            it('должен вернуть пустой суффикс для настроек прочих параметров', function() {
                expect(this.mHelpLink._getHrefSuffix('setup', {
                    tab: 'other'
                }))
                    .to.be.equal('');
            });
        });

        it('должен вернуть суффикс для метки', function() {
            expect(this.mHelpLink._getHrefSuffix('messages', {
                current_label: '123'
            }))
                .to.be.equal(this.mHelpLink._hrefSuffixMap.label)
                .and.not.equal(undefined);
        });

        it('должен вернуть суффикс для абука', function() {
            expect(this.mHelpLink._getHrefSuffix('abook', {}))
                .to.be.equal(this.mHelpLink._hrefSuffixMap.abook)
                .and.not.equal(undefined);
        });

        it('должен вернуть пустой суффикс для композа', function() {
            expect(this.mHelpLink._getHrefSuffix('compose2', {}))
                .to.be.equal('');
        });

        it('должен вернуть пустую строку, если не удалось определить суффикс', function() {
            expect(this.mHelpLink._getHrefSuffix('some_page', {})).to.be.equal('');
        });
    });
});
