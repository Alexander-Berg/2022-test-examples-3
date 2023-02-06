describe('Daria.mSettings', function() {
    /* global mock */

    beforeEach(function() {
        var hAccountInformation = ns.Model.get('account-information');
        setModelByMock(hAccountInformation);

        this.sinon.stub(Daria.Config, 'layout').value('2pane');
        this.sinon.stub(Daria.Config, 'locale').value('ru');

        this.handler = ns.Model.get('settings');
        setModelByMock(this.handler);

        this.sinon.spy(ns, 'forcedRequest');
        this.sinon.spy(this.handler, 'setSettingOn');
        this.sinon.spy(this.handler, 'setSettingOff');
    });

    describe('getSetting', function() {
        it('должен вернуть значение, если оно определено', function() {
            expect(this.handler.getSetting('test_number')).to.be.equal(1);
        });

        it('должен вернуть undefined, если оно не определено', function() {
            expect(this.handler.getSetting('test_undefined')).to.eql(undefined);
        });

        it('должен вернуть пустой объект есле не может распарсить объект в настройке', function() {
            expect(this.handler.getSetting('zzzzz', 'json')).to.be.eql({});
        });
    });

    describe('#withBiggerText', function() {
        it('должен вернуть true, если включен крупный шрифт', function() {
            this.handler.set('.with_bigger_text', true);
            expect(this.handler.withBiggerText()).to.be.equal(true);
        });

        it('должен вернуть false, если выключен крупный шрифт', function() {
            expect(this.handler.withBiggerText()).to.be.equal(false);
        });
    });

    describe('updateSetting', function() {
        beforeEach(function() {
            this.sinon.stub(this.handler, 'setSettings');
        });

        it('должен создавать объект настроек если он не существует', function() {
            this.handler.updateSetting('unexist-name', { foo: 3, bar: 'nyan' });

            expect(this.handler.setSettings).to.be.calledWith({ 'unexist-name': {
                foo: 3,
                bar: 'nyan'
            } });
        });

        it('должен проксировать колбэк в setSettings', function() {
            var callback = function() {
            };
            this.handler.updateSetting('fake-name', 8, callback);

            expect(this.handler.setSettings).to.be.calledWith({ 'fake-name': 8 }, callback);
        });

        it('должен обновлять объект настроек если он пустой', function() {
            this.handler.updateSetting('test-empty-name', { foo: 3, bar: 'nyan' });

            expect(this.handler.setSettings).to.be.calledWith({ 'test-empty-name': {
                foo: 3,
                bar: 'nyan'
            } });
        });

        it('должен обновлять непустой объект настроек', function() {
            this.handler.updateSetting('test-non-empty-name', { foo: 3, bar: 'nyan' });

            expect(this.handler.setSettings).to.be.calledWith({ 'test-non-empty-name': {
                foo: 3,
                bar: 'nyan',
                alreadyBeHere: true
            } });
        });

        it('числовые значения должен просто перезаписывать', function() {
            this.handler.updateSetting('number-name', 7);

            expect(this.handler.setSettings).to.be.calledWith({ 'number-name': 7 });
        });

        it('строковые значения должен просто перезаписывать', function() {
            this.handler.updateSetting('string-name', 'some');

            expect(this.handler.setSettings).to.be.calledWith({ 'string-name': 'some' });
        });

        it('массивы должен просто перезаписывать', function() {
            this.handler.updateSetting('array-name', [ 1, 2, 4 ]);

            expect(this.handler.setSettings).to.be.calledWith({ 'array-name': [ 1, 2, 4 ] });
        });

        it('должен работать с вложенным объектом, если первым аргументом передана разделенная точками строка', function() {
            this.handler.updateSetting('setting-with-inner-obj.innerObj', { foo: 1, baz: 2 });

            expect(this.handler.setSettings).to.be.calledWith({ 'setting-with-inner-obj': {
                innerObj: {
                    foo: 1,
                    baz: 2
                }
            } });
        });
    });

    describe('hasSetting', function() {
        it('hasSetting должен вернуть true, если есть свойство', function() {
            expect(this.handler.hasSetting('test_has_setting')).to.be.ok;
        });

        it('hasSetting должен вернуть false, если нет свойства', function() {
            expect(this.handler.hasSetting('test_no_has_setting')).to.not.be.equal();
        });

        it('hasSetting должен вернуть false, если нет данных', function() {
            this.handler.destroy();
            expect(this.handler.hasSetting('test_no_has_setting')).to.not.be.equal();
        });
    });

    describe('isSet', function() {
        it('isSet должен возвращать true для непустого значения', function() {
            expect(this.handler.isSet('b')).to.be.ok;
        });

        it('isSet должен возвращать false для пустого значения', function() {
            expect(this.handler.isSet('z')).to.not.be.ok;
        });

        it('isSet должен возвращать false когда настройка не существует', function() {
            expect(this.handler.isSet('zzzzz')).to.not.be.ok;
        });
    });

    describe('setSettingOn', function() {
        it('должен сразу вызвать callback, если настройка уже включена', function() {
            var callback = {
                f: function() {
                }
            };
            sinon.spy(callback, 'f');

            this.handler.setSettingOn('c', callback.f);
            expect(this.handler.isSet('c')).to.be.ok;
            expect(callback.f.called).to.be.equal(true);
        });

        it('должен вызвать ns.forcedRequest, если настройка еще не включена', function() {
            this.handler.setSettingOn('d');
            expect(this.handler.isSet('d')).to.be.ok;
            expect(ns.forcedRequest.called).to.be.equal(true);
        });
    });

    describe('setSettingOff', function() {
        it('должен сразу вызвать callback, если настройка уже выключена', function() {
            var callback = {
                f: function() {
                }
            };
            sinon.spy(callback, 'f');

            this.handler.setSettingOff('aaaa', callback.f);
            expect(this.handler.isSet('aaaa')).to.not.be.ok;
            expect(callback.f.called).to.be.equal(true);
        });

        it('должен вызвать ns.forcedRequest, если настройка еще не включена', function() {
            this.handler.setSettingOff('c');
            expect(this.handler.isSet('c')).to.not.be.ok;
            expect(ns.forcedRequest.called).to.be.equal(true);
        });
    });

    describe('toggleSetting', function() {
        it('должен вызвать setSettingOn, если настройка выключена', function() {
            this.handler.toggleSetting('aaaa', function() {
            });
            expect(this.handler.setSettingOn.called).to.be.equal(true);
        });

        it('должен вызвать setSettingOff, если настройка включена', function() {
            this.handler.toggleSetting('a', function() {
            });
            expect(this.handler.setSettingOff.called).to.be.equal(true);
        });
    });

    describe('#isThreaded', function() {
        it('isThreaded=true, если есть настройка folder_thread_view', function() {
            expect(this.handler.isThreaded()).to.be.ok;
        });

        it('isThreaded=true, если нет настройки folder_thread_view', function() {
            var data = getModelMock(this.handler);
            data.folder_thread_view = '';
            this.handler.setData(data);

            expect(this.handler.isThreaded()).to.not.be.ok;
        });
    });

    describe('threadedOn', function() {
        it('должен вызвать setSettingOn, если настройка выключена', function() {
            this.sinon.stub(this.handler, 'isThreaded').returns(false);
            this.handler.threadedOn();
            expect(this.handler.setSettingOn).to.be.calledWith('folder_thread_view');
        });

        it('должен ничего не сделать, если настройка включена', function() {
            this.sinon.stub(this.handler, 'isThreaded').returns(true);
            this.handler.threadedOn();
            expect(this.handler.setSettingOn).to.have.callCount(0);
        });
    });

    describe('threadedOff', function() {
        it('должен вызвать setSettingOff, если настройка включена', function() {
            this.sinon.stub(this.handler, 'isThreaded').returns(true);
            this.handler.threadedOff();
            expect(this.handler.setSettingOff).to.be.calledWith('folder_thread_view');
        });

        it('должен ничего не сделать, если настройка выключена', function() {
            this.sinon.stub(this.handler, 'isThreaded').returns(false);
            this.handler.threadedOff();
            expect(this.handler.setSettingOff).to.have.callCount(0);
        });
    });

    describe('threadedToggle', function() {
        it('должен вызвать setSettingOff, если настройка включена', function() {
            this.sinon.stub(this.handler, 'isThreaded').returns(true);
            this.handler.threadedToggle();
            expect(this.handler.setSettingOff).to.be.calledWith('folder_thread_view');
        });

        it('должен вызвать setSettingOn, если настройка выключена', function() {
            this.sinon.stub(this.handler, 'isThreaded').returns(false);
            this.handler.threadedToggle();
            expect(this.handler.setSettingOn).to.be.calledWith('folder_thread_view');
        });
    });

    it('getValidatedEmails', function() {
        expect(this.handler.getValidatedEmails()).to.eql([ 'doochik@yandex.ru', 'my@ya.ru' ]);
    });

    it('setSettings', function() {
        this.handler.setSettings({
            a: true,
            b: 1
        });

        expect(ns.forcedRequest.calledWith([
            { id: 'do-settings', params: { actual: true, list: 'a,b', params: '{"a":true,"b":1}' } },
            { id: 'settings', params: { actual: true, list: 'a,b' } }
        ])).to.be.equal(true);
    });

    describe('#recordAttemptToRegisterMAILTOProtocolHandler', function() {
        beforeEach(function() {
            this.getRphSetting = this.sinon.stub(this.handler, 'getSetting')
                .withArgs('rph')
                .returns('');
            this.sinon.stub(this.handler, 'setSettings');
        });

        it('Должен возвращать false, если не передан первый аргумент `browser`', function() {
            expect(this.handler.recordAttemptToRegisterMAILTOProtocolHandler()).to.be.equal(false);
        });

        it('Должен возвращать true, если для данного браузера настройка имеет значение отличное от желаемого', function() {
            expect(this.handler.recordAttemptToRegisterMAILTOProtocolHandler('webkit', true)).to.be.equal(true);
        });

        it('Должен возвращать false, если для данного браузера настройка имеет желаемое значение', function() {
            expect(this.handler.recordAttemptToRegisterMAILTOProtocolHandler('webkit', false)).to.be.equal(false);
        });

        it('Должен для нового значения конструировать строку и сохранять её в значение настройки', function() {
            this.handler.recordAttemptToRegisterMAILTOProtocolHandler('webkit', true);

            expect(this.handler.setSettings).to.be.calledWithExactly({ rph: 'mozilla=0&opera=0&webkit=1' });
        });

        it('Должен для нового значения конструировать строку и сохранять её в значение настройки (2)', function() {
            this.getRphSetting.returns('mozilla=1&opera=1&webkit=1');

            this.handler.recordAttemptToRegisterMAILTOProtocolHandler('opera', false);

            expect(this.handler.setSettings).to.be.calledWithExactly({ rph: 'mozilla=1&opera=0&webkit=1' });
        });
    });

    describe('#preprocessData', function() {
        beforeEach(function() {
            this.minimumData = {
                messages_per_page: 30
            };
            this.is3pane = this.sinon.stub(Daria, 'is3pane');
            this.is3pane.returns(true);
        });

        it('настройка messages_per_page должна иметь дефолтное значение 30', function() {
            expect(this.handler.preprocessData({})).to.be.eql(this.minimumData);
        });

        it('в 3pane настройка messages_per_page должна быть равна 30', function() {
            expect(this.handler.preprocessData({ messages_per_page: '3' })).to.be.eql(this.minimumData);
        });

        it('в 2pane настройка messages_per_page должна остаться неизменной, если она есть', function() {
            this.is3pane.returns(false);
            this.minimumData.messages_per_page = '3';
            expect(this.handler.preprocessData({ messages_per_page: '3' })).to.be.eql(this.minimumData);
        });
    });

    describe('#getSign', function() {
        beforeEach(function() {
            this.sinon.stub(this.handler, 'isSet');
            this.sinon.stub(this.handler, 'hasSetting');
        });

        describe('notify_message', function() {
            it('Должен вернуть true, если настройка включена', function() {
                this.handler.isSet.returns(true);

                expect(this.handler.getSign('notify_message')).to.be.equal(true);
            });

            it('Должен вернуть false, если настройка выключена и она есть', function() {
                this.handler.hasSetting.returns(true);
                this.handler.isSet.returns(false);

                expect(this.handler.getSign('notify_message')).to.be.equal(false);
            });

            it('Должен вернуть false, если настройки нет вообще и пользовтель с корпа', function() {
                this.handler.hasSetting.returns(false);
                this.handler.isSet.returns(false);
                this.sinon.stub(Daria, 'IS_CORP').value(true);

                expect(this.handler.getSign('notify_message')).to.be.equal(false);
            });

            it('Должен вернуть true, если настройки нет вообще и пользователь не с корпа', function() {
                this.handler.hasSetting.returns(false);
                this.handler.isSet.returns(false);
                this.sinon.stub(Daria, 'IS_CORP').value(false);

                expect(this.handler.getSign('notify_message')).to.be.equal(true);
            });
        });
    });

    describe('#checkTimelineEnable', function() {
        describe('Пользователи, для которых таймлайн должен быть включен поумолчанию', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'checkTimelineAvailable').returns(true);
            });

            it('Должен вернуть true, если настройка timeline_enable не задана', function() {
                this.sinon.stub(this.handler, 'hasSetting').withArgs('timeline_enable').returns(false);

                expect(this.handler.checkTimelineEnable()).to.be.equal(true);
            });

            it('Должен вернуть true, если настройка timeline_enable включена', function() {
                this.sinon.stub(this.handler, 'hasSetting').withArgs('timeline_enable').returns(true);
                this.sinon.stub(this.handler, 'isSet').withArgs('timeline_enable').returns(true);

                expect(this.handler.checkTimelineEnable()).to.be.equal(true);
            });

            it('Должен вернуть false, если настройка timeline_enable выключена', function() {
                this.sinon.stub(this.handler, 'hasSetting').withArgs('timeline_enable').returns(true);
                this.sinon.stub(this.handler, 'isSet').withArgs('timeline_enable').returns(false);

                expect(this.handler.checkTimelineEnable()).to.be.equal(false);
            });
        });

        describe('Пользователи, для которых таймлайн НЕ должен быть включен поумолчанию', function() {
            beforeEach(function() {
                this.sinon.stub(Daria, 'checkTimelineAvailable').returns(false);
                this.sinon.stub(this.handler, 'isSet').withArgs('timeline_enable').returns(true);
            });

            it('Должен вернуть true', function() {
                expect(this.handler.checkTimelineEnable()).to.be.equal(true);
            });

            it('Должен вернуть false, если не выставлена настройка timeline_enable', function() {
                this.handler.isSet.withArgs('timeline_enable').returns(false);
                expect(this.handler.checkTimelineEnable()).to.be.equal(false);
            });
        });
    });
});
