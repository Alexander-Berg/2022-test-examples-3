describe('Daria.vComposeNotifyNoreplyButton', function() {
    beforeEach(function() {
        var createNbCheckbox = function() {
            return {
                check: this.sinon.stub(),
                uncheck: this.sinon.stub(),
                isChecked: this.sinon.stub()
            };
        };

        var createNbPopup = function() {
            return {
                isOpen: this.sinon.stub(),
                open: this.sinon.stub(),
                close: this.sinon.stub(),
                onposition: this.sinon.stub()
            };
        };

        this.view = ns.View.create('compose-notify-noreply-button');
        this.view.nbs = {
            notifyCheckbox: createNbCheckbox(),
            popup: createNbPopup(),
            firstVisitPopup: createNbPopup(),
            toggleCheckbox: createNbCheckbox(),
            alwaysCheckbox: createNbCheckbox(),
            daysSelect: {
                setState: this.sinon.stub(),
                getState: this.sinon.stub()
            }
        };
        this.sinon.stub(this.view, 'setCheckboxState');

        // Стаб запроса данных из модели
        this.mComposeNotifyNoreplyOptions = this.view.getModel('compose-notify-noreply-options');
        this.isEnabledNotify = this.sinon.stub(this.mComposeNotifyNoreplyOptions, 'get')
            .withArgs('.enabledNotify');
        this.isAlwaysNotify = this.mComposeNotifyNoreplyOptions.get
            .withArgs('.alwaysNotify');
    });

    describe('#isEnabledNotify', function() {
        it('должен вернуть true, если включено напоминание о неответе', function() {
            this.isEnabledNotify.returns(true);

            expect(this.view.isEnabledNotify()).to.be.equal(true);
        });

        it('должен вернуть false, если выключено напоминание о неответе', function() {
            this.isEnabledNotify.returns(false);

            expect(this.view.isEnabledNotify()).to.be.equal(false);
        });
    });

    describe('#showFirstVisitPopup', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.sinon.stub(this.mComposeNotifyNoreplyOptions, 'wasFirstVisitPopupShown').returns(false);
            this.sinon.stub(this.mComposeNotifyNoreplyOptions, 'setFirstVisitPopupWasShown');
        });

        it('должен показать информационный попап при первом посещении #compose', function() {
            this.view._showFirstVisitPopup();

            expect(this.view.nbs.firstVisitPopup.open)
                .to.have.callCount(1);
        });

        it('должен отключить возможность попапа при последующих посещениях #compose', function() {
            this.view._showFirstVisitPopup();

            expect(this.mComposeNotifyNoreplyOptions.setFirstVisitPopupWasShown)
                .to.have.callCount(1);
        });

        it('не должен показывать информационный попап при 2-м и последующих посещениях #compose', function() {
            this.mComposeNotifyNoreplyOptions.wasFirstVisitPopupShown.returns(true);

            this.view._showFirstVisitPopup();

            expect(this.view.nbs.firstVisitPopup.open)
                .to.have.callCount(0);
        });

        it('должен переместить попап при изменении размеров (если попап был показан)', function() {
            this.view.nbs.firstVisitPopup.isOpen.returns(true);

            this.view._showFirstVisitPopup();

            expect(this.view.nbs.firstVisitPopup.onposition)
                .to.have.callCount(1);
        });
    });

    describe('#hasCurrentTimeDelta', function() {
        beforeEach(function() {
            this.item = {
                time: { hours: 1 },
                value: Daria.timify({ hours: 5 }, 'seconds'),
                text: '1 час'
            };

            this.sinon.stub(this.mComposeNotifyNoreplyOptions, 'getCurrentTimeDelta').returns(this.item);
        });

        it('должен вернуть true, если в модели и селекте выбран один и тот же элемент', function() {
            var item = _.clone(this.item);
            item.value = String(item.value);
            this.view.nbs.daysSelect.getState.returns(item);

            expect(this.view.hasCurrentTimeDelta()).to.be.equal(true);
        });

        it('должен вернуть false, если в модели и селекте выбраны разные элементы', function() {
            this.view.nbs.daysSelect.getState.returns({
                time: { days: 5 },
                value: String(Daria.timify({ days: 5 }, 'seconds')),
                text: '5 дней'
            });

            expect(this.view.hasCurrentTimeDelta()).to.be.equal(false);
        });
    });

    describe('#onChangedCurrentTimeDelta', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.sinon.stub(this.view, 'hasCurrentTimeDelta').returns(true);
        });

        it('не должен менять состояние select, если оно соответствует данным модели', function() {
            this.view.onChangedCurrentTimeDelta();

            expect(this.view.nbs.daysSelect.setState).to.have.callCount(0);
        });

        it('должен поменять состояние select согласно данным из модели, если его состояние отлично', function() {
            var newData = {};
            this.sinon.stub(this.mComposeNotifyNoreplyOptions, 'getCurrentTimeDelta').returns(newData);
            this.view.hasCurrentTimeDelta.returns(false);
            this.view.onChangedCurrentTimeDelta();

            expect(this.view.nbs.daysSelect.setState).to.be.calledWithExactly(newData);
        });
    });

    describe('#onChangedAlwaysNotify', function() {
        it('должен изменить состояние чекбокса "всегда напоминать о неответе" согласно данным из модели', function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.isAlwaysNotify.returns(true);
            this.view.onChangedAlwaysNotify();

            expect(this.view.setCheckboxState).to.be.calledWithExactly('alwaysCheckbox', true);
        });
    });

    describe('#onChangedEnabledNotify', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'isVisible').returns(true);
            this.sinon.stub(this.mComposeNotifyNoreplyOptions, 'getCurrentTimeDeltaLocalizedText');
            this.sinon.stub(this.view, 'replaceTextInWaitForReply');
            this.sinon.stub(this.view, 'addNotifyToMessage');
            this.sinon.stub(this.view, 'resetTextInWaitForReply');
            this.sinon.stub(this.view, 'removeNotifyFromMessage');
        });

        it('должен изменить состояние кнопки согласно данным из модели', function() {
            this.isEnabledNotify.returns(true);
            this.view.onChangedEnabledNotify();

            expect(this.view.setCheckboxState).to.be.calledWithExactly('notifyCheckbox', true);
        });

        it('должен изменить состояние чекбокса "напомнить о неответе" согласно данным из модели', function() {
            this.isEnabledNotify.returns(false);
            this.view.onChangedEnabledNotify();

            expect(this.view.setCheckboxState).to.be.calledWithExactly('toggleCheckbox', false);
        });

        describe('если напоминание о неответе включено ->', function() {
            beforeEach(function() {
                this.isEnabledNotify.returns(true);
                this.view.onChangedEnabledNotify();
            });

            it('должен измениться текст кнопки', function() {
                expect(this.mComposeNotifyNoreplyOptions.getCurrentTimeDeltaLocalizedText).to.have.callCount(1);
                expect(this.view.replaceTextInWaitForReply).to.have.callCount(1);
            });

            it('должна записаться информация о включении напоминания о неответе в данные о письме', function() {
                expect(this.view.addNotifyToMessage).to.have.callCount(1);
            });
        });

        describe('если напоминание о неответе выключено ->', function() {
            beforeEach(function() {
                this.isEnabledNotify.returns(false);
                this.view.onChangedEnabledNotify();
            });

            it('должен измениться текст кнопки', function() {
                expect(this.view.resetTextInWaitForReply).to.have.callCount(1);
            });

            it('должна удалиться информация о включении напоминания о неответе в данные о письме', function() {
                expect(this.view.removeNotifyFromMessage).to.have.callCount(1);
            });
        });
    });

    describe('#onNotifyCheckboxClick', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'showBubble');
        });

        it('должен показать попап настроек напоминания о неответе, если включить их возможно', function() {
            this.view.onNotifyCheckboxClick();

            expect(this.view.showBubble).to.have.callCount(1);
        });
    });

    describe('#onNotifyCheckboxChecked', function() {
        it('не должен дать выбраться кнопке, если не включено напомиание о неответе, но разрешено сохранять письма в папке "Оправленные"', function() {
            this.isEnabledNotify.returns(false);
            this.view.onNotifyCheckboxChecked();

            expect(this.view.nbs.notifyCheckbox.uncheck).to.have.callCount(1);
        });
    });

    describe('#onNotifyCheckboxUnchecked', function() {
        it('должен выбрать кнопку, если включено напоминание о неответе и резрешено сохранять письма в папке "Отправленные"', function() {
            this.isEnabledNotify.returns(true);
            this.view.onNotifyCheckboxUnchecked();

            expect(this.view.nbs.notifyCheckbox.check).to.have.callCount(1);
        });
    });

    describe('#onDaysSelectChanged', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'hasCurrentTimeDelta').returns(false);
            this.sinon.stub(this.mComposeNotifyNoreplyOptions, 'setCurrentTimeDelta');
        });

        it('не должен изменять интервал напоминания, если в модели уже установлены данные из select', function() {
            this.view.hasCurrentTimeDelta.returns(true);
            this.view.onDaysSelectChanged();

            expect(this.mComposeNotifyNoreplyOptions.setCurrentTimeDelta).to.have.callCount(0);
        });

        it('должен задать интервал напоминания согласно данным из селекта, если в модели другое значение', function() {
            var item = {};
            this.view.nbs.daysSelect.getState.returns(item);
            this.view.onDaysSelectChanged();

            expect(this.mComposeNotifyNoreplyOptions.setCurrentTimeDelta).to.be.calledWithExactly(item);
        });

        it('должен закрыть попап настроек', function() {
            this.view.onDaysSelectChanged();

            expect(this.view.nbs.popup.close).to.have.callCount(1);
        });
    });

    describe('#onClosePopupClick', function() {
        it('должен закрыть попап настроек', function() {
            this.view.onClosePopupClick();

            expect(this.view.nbs.popup.close).to.have.callCount(1);
        });
    });

    describe('Взаимодействие с внешними компонентами', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'composeUpdate');
            return this.view.update();
        });

        it('Должен запустить обновление композа composeUpdate при изменении translationEnabled в модели compose-state', function() {
            this.view.getModel('compose-state').set('.translationEnabled', true);
            expect(this.view.composeUpdate).to.have.callCount(1);
        });

        it('Метод isMinimize должен вернуть true, если включен translationEnabled в модели compose-state', function() {
            this.view.getModel('compose-state').set('.translationEnabled', true);
            expect(this.view.isMinimize()).to.be.ok;
        });

        it('Метод isMinimize должен вернуть false, если выключен translationEnabled в модели compose-state', function() {
            this.view.getModel('compose-state').set('.translationEnabled', false);
            expect(this.view.isMinimize()).not.to.be.ok;
        });
    });
});
