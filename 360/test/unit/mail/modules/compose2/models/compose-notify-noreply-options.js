describe('mComposeNotifyNoreplyOptions', function() {
    beforeEach(function() {
        this.model = ns.Model.get('compose-notify-noreply-options');
        this.model._initFromModels([
            ns.Model.get('settings')
        ]);
    });

    describe('#setCurrentTimeDelta', function() {
        beforeEach(function() {
            this.item = {
                time: { hours: 1 },
                value: Daria.timify({ hours: 1 }, 'seconds'),
                text: '1 час'
            };
            this.defaultItem = {
                time: { days: 5 },
                value: Daria.timify({ days: 5 }, 'seconds'),
                text: '5 дней',
                'default': true
            };
            this.findTimeDeltaItem = this.sinon.stub(this.model, 'findTimeDeltaItem').returns(this.item);
            this.getDefaultTimeDeltaItem = this.sinon.stub(this.model, 'getDefaultTimeDeltaItem').returns(this.defaultItem);
            this.sinon.spy(this.model, 'set');
        });

        it('должен установить новое значение интервала ожидания напоминания о неответе', function() {
            this.model.setCurrentTimeDelta(this.item);

            expect(this.model.get('.currentTimeDelta')).to.be.equal(this.item);
        });

        it('должен установить дефолтное значение, если передаваемый интервал не находится в доступных значениях', function() {
            this.findTimeDeltaItem.returns(undefined);

            this.model.setCurrentTimeDelta();

            expect(this.model.get('.currentTimeDelta')).to.be.equal(this.defaultItem);
        });

        it('должен установить флаг "напомнить о неответе"', function() {
            this.model.setCurrentTimeDelta();

            expect(this.model.get('.enabledNotify')).to.be.equal(true);
        });

    });

    describe('#findTimeDeltaItem', function() {

        beforeEach(function() {
            this.items = [
                {
                    time: {hours: 1},
                    value: Daria.timify({hours: 1}, 'seconds'),
                    text: '1 час'
                },

                {
                    time: {days: 5},
                    value: Daria.timify({days: 5}, 'seconds'),
                    text: '5 дней',
                    'default': true
                }
            ];
            this.sinon.stub(this.model, 'get').withArgs('.timeDeltaItems').returns(this.items);
        });


        it('должен найти интервал по указанному строковому value', function() {
            var item = this.model.findTimeDeltaItem({value: '3600'});

            expect(item).to.be.equal(this.items[0]);
        });

        it('должен анйти интервал по указанному числовому value', function() {
            var item = this.model.findTimeDeltaItem({value: Daria.timify({days: 5}, 'seconds')});

            expect(item).to.be.equal(this.items[1]);
        });

        it('должен найти интервал по произвольному ключу', function() {
            var item = this.model.findTimeDeltaItem({default: true});

            expect(item).to.be.equal(this.items[1]);
        });

        it('должен вернуть undefined, если интервал не найден', function() {
            var item = this.model.findTimeDeltaItem({test: 'test'});

            expect(item).to.be.equal(undefined);
        });
    });

    describe('#getSelectedTimeDeltaItem', function() {

        beforeEach(function() {
            this.sinon.stub(this.model, 'findTimeDeltaItem');
        });

        it('должен вернуть выбранный интервал напоминания', function() {
            this.model.getSelectedTimeDeltaItem();

            expect(this.model.findTimeDeltaItem).to.be.calledWithExactly({selected: true});
        });

    });

    describe('#getDefaultTimeDeltaItem', function() {

        beforeEach(function() {
            this.sinon.stub(this.model, 'findTimeDeltaItem');
        });

        it('должен вернуть выбранный интервал напоминания', function() {
            this.model.getDefaultTimeDeltaItem();

            expect(this.model.findTimeDeltaItem).to.be.calledWithExactly({default: true});
        });

    });

    describe('#isSetInSettings', function() {

        beforeEach(function() {
            this.mSettingsIsSet = this.sinon.stub(this.model.mSettings, 'isSet');
        });

        it('должен вернуть true, если настройками установлено "всегда напоминать о неответе"', function() {
            this.mSettingsIsSet.withArgs('no_reply_notify').returns(true);

            expect(this.model.isSetInSettings()).to.be.equal(true);
        });

        it('должен вернуть false, если раньше не было высталено "всегда напоминать о неответе"', function() {
            this.mSettingsIsSet.withArgs('no_reply_notify').returns(false);

            expect(this.model.isSetInSettings()).to.be.equal(false);
        });

    });

    describe('#setAlwaysNotifyOption', function() {

        beforeEach(function() {
            this.item = {
                time: {hours: 1},
                value: Daria.timify({hours: 1}, 'seconds'),
                text: '1 час'
            };
            this.sinon.stub(this.model.mSettings, 'setSettings');
            this.sinon.stub(this.model, 'getCurrentTimeDelta').returns(this.item);
        });

        it('должен установить состояние "всегда напоминать о неответе" при передаче флага true  ', function() {
            this.model.setAlwaysNotifyOption(true);

            expect(this.model.get('.enabledNotify')).to.be.equal(true);
            expect(this.model.get('.alwaysNotify')).to.be.equal(true);
            expect(this.model.mSettings.setSettings).to.be.calledWithExactly({
                no_reply_notify: true,
                no_reply_notify_time: this.item.value
            });
        });

        it('должен сбросить состояние "всегда напоминать о неответе" при передаче флага false', function() {
            this.model.setAlwaysNotifyOption(false);

            expect(this.model.get('.alwaysNotify')).to.be.equal(false);
            expect(this.model.mSettings.setSettings).to.be.calledWithExactly({
                no_reply_notify: false
            });
        });

    });

    describe('#wasFirstVisitPopupShown', function() {

        beforeEach(function() {
            this.noreplyPopupShowIsSet = this.sinon.stub(this.model.mSettings, 'isSet').withArgs('noreply_popup_shown');
        });

        it('должен вернуть true, если попап первого посещения уже показывался', function() {
            this.noreplyPopupShowIsSet.returns(true);

            expect(this.model.wasFirstVisitPopupShown()).to.be.equal(true);
        });

        it('должен вернуть false, если попап первого посещения ещё не показывался', function() {
            this.noreplyPopupShowIsSet.returns(false);

            expect(this.model.wasFirstVisitPopupShown()).to.be.equal(false);
        });

    });

    describe('#setFirstVisitPopupWasShown', function() {

        beforeEach(function() {
            this.sinon.stub(this.model.mSettings, 'setSettingOn');
        });

        it('должен запомнить, что попап первого посещения был показан', function() {
            this.model.setFirstVisitPopupWasShown();

            expect(this.model.mSettings.setSettingOn).to.be.calledWithExactly('noreply_popup_shown');
        });

    });

});
