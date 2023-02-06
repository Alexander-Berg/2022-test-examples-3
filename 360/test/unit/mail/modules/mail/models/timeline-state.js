describe('Daria.mTimelineState', function() {
    beforeEach(function() {
        this.sinon.stub(Daria.Transport, 'getInstance');
        this.model = ns.Model.get('timeline-state').setData({
            'startDate': Jane.Date.LocalDate.parseISOTime('2016-07-18T00:00:00'),
            'currentDate': {
                get: function() {
                    return Jane.Date.LocalDate.parseISOTime('2016-07-18T00:00:00');
                }
            }
        });

        this.stubGetCurrentDate = this.sinon.stub(this.model, '_getCurrentDate').returns(Jane.Date.LocalDate.parseISOTime('2016-07-18T00:00:00'));
    });

    afterEach(function() {
        this.model.destroy();
    });

    describe('Блокировка изменения startDate', function() {
        it('При изменении startDate устанавливается блок на повторное изменение', function() {
            var date0 = Jane.Date.LocalDate.parseISOTime('2016-07-18T00:00:00');
            var date1 = Jane.Date.LocalDate.parseISOTime('2016-07-18T23:00:00');
            this.model.set('.startDate', date0);
            this.model.set('.startDate', date1);

            expect(this.model.get('.startDate')).to.be.equal(date0);
        });

        it('Блок изменения startDate снимается при изменении startOffset', function() {
            var date0 = Jane.Date.LocalDate.parseISOTime('2016-07-18T00:00:00');
            var date1 = Jane.Date.LocalDate.parseISOTime('2016-07-18T23:00:00');
            this.model.set('.startDate', date0);
            this.model.set('.startOffset', {
                get: function() { return 0; }
            });
            this.model.set('.startDate', date1);

            expect(this.model.get('.startDate')).to.be.equal(date1);
        });
    });

    describe('Изменение текущей даты', function() {
        it('Изменение текущей даты currentDate более чем на 5 минут должен создать событие daria:mTimelineState:update', function() {
            var stub = this.sinon.stub(this.model, 'atrigger');
            this.model.set('.currentDate', {
                get: function() {
                    return Jane.Date.LocalDate.parseISOTime('2016-07-18T00:06:00');
                }
            });

            expect(stub).to.be.calledWithExactly('daria:mTimelineState:update');
        });

        it('Изменение текущей даты currentDate менее чем на 5 минут не должен создать событие daria:mTimelineState:update', function() {
            var stub = this.sinon.stub(this.model, 'atrigger');
            this.model.set('.currentDate', {
                get: function() {
                    return Jane.Date.LocalDate.parseISOTime('2016-07-18T00:05:00');
                }
            });

            expect(stub).to.have.callCount(0);
        });

        it('Если при изменении currentDate она отличается от startDate на 4 часа, сдвигаем startDate до текущего значения', function() {
            this.sinon.stub(this.model, 'atrigger');
            this.stubGetCurrentDate.returns(Jane.Date.LocalDate.parseISOTime('2016-07-18T23:00:00'));
            this.model.set('.currentDate', {
                get: function() {
                    return Jane.Date.LocalDate.parseISOTime('2016-07-18T23:00:00');
                }
            });

            expect(this.model.get('.startDate').toISOString()).to.be.equal('2016-07-18T21:00:00');
        });
    });

    describe('#checkShow', function() {
        beforeEach(function() {
            this.stubCheckUnavailable = this.sinon.stub(this.model, 'checkUnavailable').returns(false);
            this.stubCheckTimelineAvailable = this.sinon.stub(Daria, 'checkTimelineAvailable').returns(false);
            this.stubIsSetTimelineEnable = this.sinon.stub(ns.Model.get('settings'), 'isSet').withArgs('timeline_enable').returns(true);
            this.stubHasSettingTimelineEnable = this.sinon.stub(ns.Model.get('settings'), 'hasSetting').withArgs('timeline_enable').returns(true);
            this.stubHasSid = this.sinon.stub(Daria, 'hasSid').withArgs(this.model.CALENDAR_SID).returns(true);
        });

        it('Условие выполняется для обычного пользователя при установленной настройке и наличии сида', function() {
            expect(this.model.checkShow()).to.be.equal(true);
        });

        it('Если сработало условие безусловного скрытия checkUnavailable, должно вернуть false', function() {
            this.stubCheckUnavailable.returns(true);
            expect(this.model.checkShow()).to.be.equal(false);
        });

        it('Должно вернуть false, если обычный пользователь и нет настройки timeline_enable', function() {
            this.stubIsSetTimelineEnable.returns(false);
            expect(this.model.checkShow()).to.be.equal(false);
        });

        it('Для тимного юзера должно вернуть true при наличии настройки', function() {
            this.stubCheckTimelineAvailable.returns(true);
            expect(this.model.checkShow()).to.be.equal(true);
        });

        it('Для тимного пользователя должно вернуть true, если настройки никогда небыло', function() {
            this.stubCheckTimelineAvailable.returns(true);
            this.stubIsSetTimelineEnable.returns(false);
            this.stubHasSettingTimelineEnable.returns(false);
            expect(this.model.checkShow()).to.be.equal(true);
        });

        it('Для тимного юзера должно вернуть false, если настройка отключена', function() {
            this.stubCheckTimelineAvailable.returns(true);
            this.stubIsSetTimelineEnable.returns(false);
            expect(this.model.checkShow()).to.be.equal(false);
        });
    });

    describe('#checkShowCollapsed', function() {
        beforeEach(function() {
            this.stubCheckShow = this.sinon.stub(this.model, 'checkShow').returns(true);
            this.stubIsSet = this.sinon.stub(ns.Model.get('settings'), 'isSet').withArgs('timeline-is-open').returns(false);
            this.stubHasSetting = this.sinon.stub(ns.Model.get('settings'), 'hasSetting').withArgs('timeline-is-open').returns(true);
        });

        it('Должно вернуть true, если выполняется услвие показа полоски или промо и настройка выключена', function() {
            expect(this.model.checkShowCollapsed()).to.be.equal(true);
        });

        it('Должно вернуть false, если не показывается полоска и промо', function() {
            this.stubCheckShow.returns(false);
            expect(this.model.checkShowCollapsed()).to.be.equal(false);
        });

        it('Должно вернуть false, если настройки никогда не было', function() {
            this.stubIsSet.returns(false);
            this.stubHasSetting.returns(false);
            expect(this.model.checkShowCollapsed()).to.be.equal(false);
        });

        it('Для не ПДД и воркспейса полоску выводим развернутой, если настройки никогда небыло', function() {
            this.sinon.stub(Daria.Config, 'pddDomain').value(false);
            this.sinon.stub(Daria.Config, 'workspace').value(false);
            this.stubIsSet.returns(false);
            this.stubHasSetting.returns(true);
            expect(this.model.checkShowCollapsed()).to.be.equal(true);
        });
    });

    describe('#subscribeCalendar', function() {
        beforeEach(function() {
            this.stubCheckTimelineAvailable = this.sinon.stub(Daria, 'checkTimelineAvailable').returns(false);
            this.stubIsSetTimelineEnable = this.sinon.stub(ns.Model.get('settings'), 'isSet').withArgs('timeline_enable').returns(true);
            this.stubHasSid = this.sinon.stub(Daria, 'hasSid').withArgs(this.model.CALENDAR_SID).returns(false);

            this.stubSetSettingOff = this.sinon.stub(ns.Model.get('settings'), 'setSettingOff').callsFake(function(name, callback) {
                if (callback) {
                    callback();
                }
            }).withArgs('timeline_enable');

            this.stubRequest = this.sinon.stub(ns, 'forcedRequest').withArgs('do-admsubscribe-calendar').returns(Vow.resolve([{
                getData: function() {
                    return { status: 'ok' };
                }
            }]));
        });

        it('Для не тима при наличии настройки и отсутствии сида должно выполнить подписку на календарь и выставить сид', function(done) {
            var spy = this.sinon.spy(Daria, 'addSid');

            this.model.subscribeCalendar().always(function() {
                expect(spy).to.be.calledWith(this.model.CALENDAR_SID);
                done();
            }, this);
        });

        it('Для тима запрос не отправляется', function(done) {
            this.stubCheckTimelineAvailable.returns(true);

            this.model.subscribeCalendar().always(function() {
                expect(ns.forcedRequest).to.have.callCount(0);
                done();
            });
        });

        it('Если нет настройки запрос не отправляется', function(done) {
            this.stubIsSetTimelineEnable.returns(false);

            this.model.subscribeCalendar().always(function() {
                expect(ns.forcedRequest).to.have.callCount(0);
                done();
            });
        });

        it('Если есть сид запрос не отправляется', function(done) {
            this.stubHasSid.returns(true);

            this.model.subscribeCalendar().always(function() {
                expect(ns.forcedRequest).to.have.callCount(0);
                done();
            });
        });

        it('Если ошибка подписки, сид не выставляется', function(done) {
            this.stubRequest.returns(Vow.resolve([{
                getData: function() {
                    return { status: 'error' };
                }
            }]));

            var spy = this.sinon.spy(Daria, 'addSid');
            this.model.subscribeCalendar().always(function() {
                expect(spy).to.have.callCount(0);
                done();
            });
        });

        it('Если ошибка подписки, настройка снимается', function(done) {
            this.stubRequest.returns(Vow.resolve([{
                getData: function() {
                    return { status: 'error' };
                }
            }]));

            this.model.subscribeCalendar().always(function() {
                expect(this.stubSetSettingOff).to.be.calledWith('timeline_enable');
                done();
            }, this);
        });
    });
});

