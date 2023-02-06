describe('Daria.resize', function() {
    describe('#adjustToCompactMode', function() {
        beforeEach(function() {
            this.mSettings = ns.Model.get('settings');

            this.sinon.stub(Daria.resize, 'expandToViewport');
            this.sinon.stub(Daria.resize, 'resetExpandToViewport');
            this.sinon.stub(Daria.resize, 'onAppWidthChange');
        });

        afterEach(function() {
            ns.events.off('daria:vApp:changeAppWidth');
        });

        it('должен вызвать #expandToViewport, если включен компактный режим', function() {
            this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(true);
            Daria.resize.adjustToCompactMode();

            expect(Daria.resize.expandToViewport).to.have.callCount(1);
            expect(Daria.resize.resetExpandToViewport).to.have.callCount(0);
        });

        it('должен вызвать #resetExpandToViewport, если выключен компактный режим', function() {
            this.sinon.stub(Daria.CompactMode, 'isCompactMessagesList').returns(false);
            Daria.resize.adjustToCompactMode();

            expect(Daria.resize.expandToViewport).to.have.callCount(0);
            expect(Daria.resize.resetExpandToViewport).to.have.callCount(1);
        });

        it('должен вызвать #onAppWidthChange', function() {
            this.sinon.stub(ns.Model.get('settings'), 'on');

            this.sinon.stub(Daria.resize, 'bindResizeEvent');
            this.sinon.stub(Daria.resize, 'setMaximumSizes');
            this.sinon.stub(Daria.resize, '_turnOnResize');

            this.sinon.stub(Daria.resize, '_saveColumnsProportionFor3pv');
            this.sinon.spy(Daria.resize, 'adjustToCompactMode');

            this.sinon.stub(Daria, 'is3ph').returns(false);
            this.sinon.stub(Daria, 'is3pv').returns(true);

            Daria.resize.init();

            expect(Daria.resize.adjustToCompactMode).to.have.callCount(1);
            expect(Daria.resize.onAppWidthChange).to.have.callCount(1);
        });
    });

    describe('#init', function() {
        beforeEach(function() {
            this.sinon.stub(ns.Model.get('settings'), 'on');

            this.sinon.stub(Daria.resize, 'bindResizeEvent');
            this.sinon.stub(Daria.resize, 'setMaximumSizes');
            this.sinon.stub(Daria.resize, '_turnOnResize');
            this.sinon.stub(Daria.resize, 'adjustToCompactMode');

            this.sinon.stub(Daria.resize, '_saveColumnsProportionFor3pv');

            this.sinon.stub(Daria, 'is3ph').returns(false);
            this.sinon.stub(Daria, 'is3pv').returns(true);
        });

        it('должен вызвать #_saveColumnsProportionFor3pv, если включен вертикальный трипейн', function() {
            Daria.resize.init();

            expect(Daria.resize._saveColumnsProportionFor3pv).to.have.callCount(1);
        });

        it('не должен вызвать #_saveColumnsProportionFor3pv, если не включен вертикальный трипейн', function() {
            Daria.is3pv.returns(false);
            Daria.resize.init();

            expect(Daria.resize._saveColumnsProportionFor3pv).to.have.callCount(0);
        });

        it('должен вызвать #_saveColumnsProportionFor3pv, если включен вертикальный трипейн', function() {
            Daria.is3pv.returns(true);
            Daria.resize.init();

            expect(Daria.resize._saveColumnsProportionFor3pv).to.have.callCount(1);
        });
    });

    describe('#_onResizeStop', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.resize, 'saveSizeOnSettings');
            this.sinon.stub(Daria.resize, '_clearGlobalCursorStyle');

            this.sinon.stub(Daria.resize, '_saveRightColumnWidthSettingFor3pv');

            this.sinon.stub(Daria, 'is3ph').returns(false);
            this.sinon.stub(Daria, 'is3pv').returns(true);
        });

        it('должен вызвать #_saveRightColumnWidthSettingFor3pv, если включен вертикальный трипейн', function() {
            Daria.is3pv.returns(true);
            Daria.resize._onResizeStop();

            expect(Daria.resize._saveRightColumnWidthSettingFor3pv).to.have.callCount(1);
        });

        it('не должен вызвать #_saveRightColumnWidthSettingFor3pv, если не включен вертикальный трипейн', function() {
            Daria.is3pv.returns(false);
            Daria.resize._onResizeStop();

            expect(Daria.resize._saveRightColumnWidthSettingFor3pv).to.have.callCount(0);
        });
    });

    describe('daria:vApp:changeAppWidth', function() {
        beforeEach(function() {
            this.sinon.stub(ns.Model.get('settings'), 'on');

            this.sinon.stub(Daria.resize, 'bindResizeEvent');
            this.sinon.stub(Daria.resize, 'setMaximumSizes');
            this.sinon.stub(Daria.resize, '_turnOnResize');

            this.sinon.stub(Daria.resize, '_saveRightColumnWidthSettingFor3pv');

            this.sinon.spy(Daria.resize, 'adjustToCompactMode');

            this.sinon.stub(Daria, 'is3ph').returns(false);
            this.sinon.stub(Daria, 'is3pv').returns(true);
        });

        afterEach(function() {
            ns.events.off('daria:vApp:changeAppWidth');
        });

        it('должен вызвать #onAppWidthChange на каждое изменение размеров лейаута', function() {
            this.sinon.stub(Daria.resize, 'onAppWidthChange');

            Daria.resize.init();
            expect(Daria.resize.onAppWidthChange).to.have.callCount(1);

            ns.events.trigger('daria:vApp:changeAppWidth');
            expect(Daria.resize.onAppWidthChange).to.have.callCount(2);
        });

        it('по умолчанию не должен вызвать #_saveRightColumnWidthSettingFor3pv', function() {
            Daria.resize.init();
            ns.events.trigger('daria:vApp:changeAppWidth');
            expect(Daria.resize._saveRightColumnWidthSettingFor3pv).to.have.callCount(0);
        });

        it('должен вызвать #_saveRightColumnWidthSettingFor3pv если это явно указано в опциях события', function() {
            Daria.resize.init();
            ns.events.trigger('daria:vApp:changeAppWidth', { saveRightColumnWidthSettings: true });
            expect(Daria.resize._saveRightColumnWidthSettingFor3pv).to.have.callCount(1);
        });
    });

    describe('#onAppWidthChange', function() {
        beforeEach(function() {
            this.sinon.stub(ns.events, 'trigger');
            this.sinon.stub(ns.Model.get('settings'), 'on');

            // TODO тесты для других значений этой настройки бы
            this.sinon.stub(ns.Model.get('settings'), 'getSetting').callsFake(function() {
                return 204;
            });

            this.restore = {};

            if (Daria.resize.hasOwnProperty('_3pvColumnsProportion')) {
                this.restore._3pvColumnsProportion = Daria.resize._3pvColumnsProportion;
            } else {
                // Чтобы sinon мог застаббить это свойство.
                Daria.resize._3pvColumnsProportion = null;
            }

            this.sinon.stub(Daria.resize, 'bindResizeEvent');
            this.sinon.stub(Daria.resize, 'setMaximumSizes');
            this.sinon.stub(Daria.resize, '_turnOnResize');
            this.sinon.stub(Daria.resize, 'adjustToCompactMode');
            this.sinon.stub(Daria.resize, '_saveColumnsProportionFor3pv');
            this.sinon.stub(Daria.resize, '_saveRightColumnWidthSettingFor3pv');

            this.sinon.stub(Daria, 'is3ph').returns(false);
            this.sinon.stub(Daria, 'is3pv').returns(true);

            Daria.resize.init();
        });

        afterEach(function() {
            if (this.restore._3pvColumnsProportion) {
                Daria.resize._3pvColumnsProportion = this.restore._3pvColumnsProportion;
            }
        });

        var sizes = {
            layout: { min: 1024 },
            left: { min: 60, max: 400, maxAutoGrow: 1024 - 250 - 570 },
            first: { min: 250 },
            second: { min: 570 }
        };

        var runTest = function(testTitle, options) {
            describe(testTitle, function() {
                beforeEach(function() {
                    var leftResizableMaxWidth;
                    var firstResizableMaxWidth;

                    this.sinon.stub(Daria.resize.$layout, 'width').callsFake(function() {
                        return options.layoutWidth;
                    });
                    this.sinon.stub(Daria.resize.$layoutLeft, 'width').callsFake(function() {
                        return options.leftWidth;
                    });
                    this.sinon.stub(Daria.resize.$layoutLeft, 'resizable').callsFake(function() {
                        if (arguments.length === 2) {
                            return leftResizableMaxWidth;
                        } else if (arguments.length === 3) {
                            leftResizableMaxWidth = arguments[2];
                        }
                    });
                    this.sinon.stub(Daria.resize.$firstPane, 'width').callsFake(function() {
                        return options.firstPaneWidth;
                    });
                    this.sinon.stub(Daria.resize.$firstPane, 'resizable').callsFake(function() {
                        if (arguments.length === 2) {
                            return firstResizableMaxWidth;
                        } else if (arguments.length === 3) {
                            firstResizableMaxWidth = arguments[2];
                        }
                    });
                    this.sinon.stub(Daria.resize.$secondPane, 'width').callsFake(function() {
                        return options.secondPaneWidth;
                    });
                    this.sinon.stub(Daria.resize, '_3pvColumnsProportion').value(options.proportions);

                    Daria.resize.onAppWidthChange(null, { isLeftResized: options.isLeftResized });
                });

                it('[left width]', function() {
                    expect(Daria.resize.$layoutLeft.width.callCount).to.be.eql(3);
                    expect(Daria.resize.$layoutLeft.width.getCall(2).args[0]).to.be.eql(options.expect.left);
                });
                it('[left resizable max width]', function() {
                    expect(Daria.resize.$layoutLeft.resizable('option', 'maxWidth'), options.expect.leftMax);
                });
                it('[first width]', function() {
                    expect(Daria.resize.$firstPane.width.callCount).to.be.eql(1);
                    expect(Daria.resize.$firstPane.width.getCall(0).args[0]).to.be.eql(options.expect.first);
                });
                it('[first resizable max width]', function() {
                    expect(Daria.resize.$firstPane.resizable('option', 'maxWidth'), options.expect.leftMax);
                });
                it('[second width check]', function() {
                    expect(options.expect.left + options.expect.first + options.expect.second)
                        .to.be.eql(options.layoutWidth);
                });
            });
        };

        var tests = {
            'Узкий лейаут (меньше минимума в 1024px)': {
                layoutWidth: 1000,
                leftWidth: 400,
                proportions: 3 / 8,
                expect: {
                    left: 1000 - sizes.first.min - sizes.second.min,    // 180
                    leftMax: 1000 - sizes.first.min - sizes.second.min, // 180
                    first: sizes.first.min,
                    firstMax: sizes.first.min, // 250
                    second: sizes.second.min
                }
            },

            'При увеличении ширины лейаута вначале должна расти левая колонка до MAX_WIDTH_LAYOUT_LEFT_AUTO_GROW (кейс 1)': {
                layoutWidth: 1000,
                leftWidth: 80,
                firstPaneWidth: sizes.first.min,
                secondPaneWidth: sizes.second.min,
                proportions: 3 / 8,
                expect: {
                    left: 1000 - sizes.first.min - sizes.second.min,    // 180
                    leftMax: 1000 - sizes.first.min - sizes.second.min, // 180
                    first: sizes.first.min,
                    firstMax: sizes.first.min, // 250
                    second: sizes.second.min
                }
            },

            'После достижения левой колонкой MAX_WIDTH_LAYOUT_LEFT_AUTO_GROW растём до пропорции - правой колонкой': {
                layoutWidth: sizes.layout.min + 10,
                leftWidth: sizes.left.maxAutoGrow,
                firstPaneWidth: sizes.first.min,
                secondPaneWidth: sizes.second.min,
                proportions: 1 / 2,
                expect: {
                    left: sizes.left.maxAutoGrow, // 204
                    leftMax: sizes.left.maxAutoGrow + 10, // 214
                    first: sizes.first.min + 10, // 260
                    firstMax: sizes.first.min + 10, // 260
                    second: sizes.second.min
                }
            },

            'После достижения левой колонкой MAX_WIDTH_LAYOUT_LEFT_AUTO_GROW растём до пропорции - областью письма': {
                layoutWidth: sizes.layout.min + 10,
                leftWidth: sizes.left.maxAutoGrow,
                firstPaneWidth: sizes.first.min,
                secondPaneWidth: sizes.second.min,
                proportions: 1 / 5,
                expect: {
                    left: sizes.left.maxAutoGrow, // 204
                    leftMax: sizes.left.maxAutoGrow + 10, // 214
                    first: sizes.first.min, // 250
                    firstMax: sizes.first.min, // 250
                    second: sizes.second.min + 10 // 580
                }
            },

            'После достижения левой колонкой MAX_WIDTH_LAYOUT_LEFT_AUTO_GROW и достижения пропорции - правая колонка и область письма растут пропорционально': {
                layoutWidth: 1204, // sizes.left.maxAutoGrow + 400 + 600,
                leftWidth: 204, // sizes.left.maxAutoGrow,
                firstPaneWidth: 380,
                secondPaneWidth: 570,
                proportions: 2 / 5,
                expect: {
                    left: 204, // sizes.left.maxAutoGrow
                    leftMax: 1204 - sizes.first.min - sizes.second.min, // 384
                    first: 400,
                    firstMax: 1204 - 204 - sizes.second.min, // 430
                    second: 600
                }
            },

            'Переключение из широкого окна в узкое ... ': {
                layoutWidth: 970,
                leftWidth: 310,
                firstPaneWidth: 400,
                secondPaneWidth: 600,
                proportions: 2 / 5,
                expect: {
                    left: 150,
                    leftMax: 150,
                    first: sizes.first.min,
                    firstMax: sizes.first.min,
                    second: sizes.second.min
                }
            },

            '... переключение назад в широкое окно': {
                layoutWidth: 1204,
                leftWidth: 80,
                firstPaneWidth: sizes.first.min,
                secondPaneWidth: sizes.second.min,
                proportions: 2 / 5,
                expect: {
                    left: 204,
                    leftMax: 1204 - sizes.first.min - sizes.second.min, // 384
                    first: 400,
                    firstMax: 1204 - 204 - sizes.second.min, // 430
                    second: 600
                }
            },

            'Левая колонка может уменьшаться за счёт правой колонки и области письма': {
                layoutWidth: 1204,
                leftWidth: 194, // Эмуляция уменьшения левой колонки 204 => 194
                firstPaneWidth: 400,
                secondPaneWidth: 600,
                proportions: 2 / 5,
                isLeftResized: true,
                expect: {
                    left: 194,
                    leftMax: 1204 - sizes.first.min - sizes.second.min,
                    first: 404,
                    firstMax: 1204 - 194 - sizes.second.min,
                    second: 606
                }
            },

            'Переключение в широкое окно - левая колонка растёт до стоп размера - 204px': {
                layoutWidth: 1024,
                leftWidth: 80,
                firstPaneWidth: 250,
                secondPaneWidth: 694,
                proportions: 1 / 5,
                expect: {
                    left: 204,
                    leftMax: 204,
                    first: 250,
                    firstMax: 250,
                    second: 570
                }
            }

        };

        for (var testTitle in tests) {
            runTest(testTitle, tests[testTitle]);
        }
    });

    describe('#onAppHeightChange', function() {
        beforeEach(function() {
            this.sinon.stub(ns.events, 'trigger');
            this.sinon.stub(ns.Model.get('settings'), 'on');

            this.restore = {};

            if (Daria.resize.hasOwnProperty('_3phColumnsProportion')) {
                this.restore._3phColumnsProportion = Daria.resize._3phColumnsProportion;
            } else {
                // Чтобы sinon мог застаббить это свойство.
                Daria.resize._3phColumnsProportion = null;
            }

            this.sinon.stub(Daria.resize, 'bindResizeEvent');
            this.sinon.stub(Daria.resize, 'setMaximumSizes');
            this.sinon.stub(Daria.resize, '_turnOnResize');
            this.sinon.stub(Daria.resize, 'adjustToCompactMode');
            this.sinon.stub(Daria.resize, '_saveColumnsProportionFor3ph');
            this.sinon.stub(Daria.resize, '_saveRightColumnHeightSettingFor3ph');
            this.sinon.stub(Daria.resize, '_updateResizeHandleFor3ph');

            this.sinon.stub(Daria, 'is3ph').returns(true);
            this.sinon.stub(Daria, 'is3pv').returns(false);

            Daria.resize.init();
        });

        afterEach(function() {
            if (this.restore._3phColumnsProportion) {
                Daria.resize._3phColumnsProportion = this.restore._3phColumnsProportion;
            } else {
                delete Daria.resize._3phColumnsProportion;
            }
        });

        var sizes = {
            first: { min: 88 }
        };

        var runTest = function(testTitle, options) {
            describe(testTitle, function() {
                beforeEach(function() {
                    this.sinon.stub(Daria.resize.$layoutPanes, 'height').callsFake(function() {
                        return options.change.layoutPanesHeight;
                    });
                    this.sinon.stub(Daria.resize.$firstPane, 'height').callsFake(function() {
                        return options.before.firstPaneHeight;
                    });
                    this.sinon.stub(Daria.resize, '_3phColumnsProportion').value(options.before.proportions);

                    Daria.resize.onAppHeightChange(
                        null, { saveRightColumnHeightSetting: options.expect.saveFirstPaneHeightSetting }
                    );
                });

                it('[first pane height]', function() {
                    expect(Daria.resize.$firstPane.height.callCount).to.be.eql(1);
                    expect(Daria.resize.$firstPane.height.getCall(0).args[0]).to.be.eql(options.expect.firstPaneHeight);
                });
                it('[first pane height setting saved]', function() {
                    expect(Daria.resize._saveRightColumnHeightSettingFor3ph.callCount)
                        .to.be.eql(options.expect.saveFirstPaneHeightSetting ? 1 : 0);
                });
                it('[resizer position updated]', function() {
                    expect(Daria.resize._updateResizeHandleFor3ph.callCount).to.be.eql(1);
                });
            });
        };

        var tests = {
            'Пропорциональный рост высоты при увеличении высоты лейаута': {
                before: {
                    layoutPanesHeight: 500,
                    firstPaneHeight: 200,
                    proportions: 2 / 5
                },
                change: {
                    layoutPanesHeight: 1000
                },
                expect: {
                    firstPaneHeight: 400,
                    saveFirstPaneHeightSetting: true,
                    resizePositionUpdated: true
                }
            },

            'Пропорциональное уменьшение высоты при увеличении высоты лейаута': {
                before: {
                    layoutPanesHeight: 1200,
                    firstPaneHeight: 480,
                    proportions: 2 / 5
                },
                change: {
                    layoutPanesHeight: 500
                },
                expect: {
                    firstPaneHeight: 200,
                    saveFirstPaneHeightSetting: false,
                    resizePositionUpdated: true
                }
            },

            'Высота списка писем не уменьшается меньше минимального': {
                before: {
                    layoutPanesHeight: 1000,
                    firstPaneHeight: 400,
                    proportions: 2 / 5
                },
                change: {
                    layoutPanesHeight: 100
                },
                expect: {
                    // По пропорции тут должно быть 20, но правильно тут иметь минимальную высоту.
                    firstPaneHeight: sizes.first.min,
                    saveFirstPaneHeightSetting: false,
                    resizePositionUpdated: true
                }
            }

        };

        for (var testTitle in tests) {
            runTest(testTitle, tests[testTitle]);
        }
    });

    describe('#_setSettingsWithDelay', function() {
        beforeEach(function() {
            this.stubDebounce();
            this.clock = this.sinon.useFakeTimers();

            this.mSettings = ns.Model.get('settings');
            this.sinon.stub(this.mSettings, 'on');
            this.sinon.stub(this.mSettings, 'setSettings');

            this.sinon.stub(Daria.resize, 'bindResizeEvent');
            this.sinon.stub(Daria.resize, 'setMaximumSizes');
            this.sinon.stub(Daria.resize, '_turnOnResize');

            this.sinon.stub(Daria.resize, '_saveColumnsProportionFor3pv');

            this.sinon.spy(Daria.resize, 'adjustToCompactMode');

            this.sinon.stub(Daria, 'is3ph').returns(false);
            this.sinon.stub(Daria, 'is3pv').returns(true);

            Daria.resize.init();
        });

        afterEach(function() {
            this.clock.restore();
        });

        describe('одиночный вызов сохранения настроек =>', function() {
            beforeEach(function() {
                Daria.resize._setSettingsWithDelay({ 'size-first-pane-3pv': 100 });
                this.clock.tick(1100);
            });

            it('выполняется сохранение настроек', function() {
                expect(this.mSettings.setSettings).to.have.callCount(1);
            });

            it('сохраняются переданные значения настроек', function() {
                expect(this.mSettings.setSettings).calledWith({ 'size-first-pane-3pv': 100 });
            });
        });

        describe('сохранение одной и той же настройки выполняется дважды за короткий промежуток времени =>', function() {
            beforeEach(function() {
                Daria.resize._setSettingsWithDelay({ 'size-first-pane-3pv': 100 });
                this.clock.tick(200);
                Daria.resize._setSettingsWithDelay({ 'size-first-pane-3pv': 101 });
                this.clock.tick(1100);
            });

            it('сохранение настроек выполняется один раз', function() {
                expect(this.mSettings.setSettings).to.have.callCount(1);
            });

            it('второй вызов затирает значение настройки из первого вызова', function() {
                expect(this.mSettings.setSettings).calledWith({ 'size-first-pane-3pv': 101 });
            });
        });

        describe('сохранение разных настроек выполняется несколько раз за короткий промеждуток времени =>', function() {
            beforeEach(function() {
                Daria.resize._setSettingsWithDelay({ 'size-first-pane-3pv': 100 });
                this.clock.tick(200);
                Daria.resize._setSettingsWithDelay({ 'size-view-app': 200 });
                this.clock.tick(200);
                Daria.resize._setSettingsWithDelay({ 'size-first-pane-3pv': 301 });
                this.clock.tick(1100);
            });

            it('сохранение настроек выполняется один раз', function() {
                expect(this.mSettings.setSettings).to.have.callCount(1);
            });

            it('в итоге сохраняется аккумулированный набор настроек', function() {
                expect(this.mSettings.setSettings).calledWith({ 'size-first-pane-3pv': 301, 'size-view-app': 200 });
            });
        });

        describe('повторный вызов сохранения настроек после одиночного сохранения =>', function() {
            beforeEach(function() {
                Daria.resize._setSettingsWithDelay({ 'size-first-pane-3pv': 100 });
                this.clock.tick(1100);
                Daria.resize._setSettingsWithDelay({ 'size-view-app': 200 });
                this.clock.tick(1100);
            });

            it('сохранение настроек выполнялось дважды', function() {
                expect(this.mSettings.setSettings).to.have.callCount(2);
            });

            it('во время второго вызова сохраняются только новые изменившиеся настройки', function() {
                expect(this.mSettings.setSettings.getCall(1).args[0]).to.be.eql({ 'size-view-app': 200 });
            });
        });
    });
});
