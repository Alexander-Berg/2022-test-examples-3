describe('b-banner-preview-filter2', function() {
    var block,
        sandbox,
        createBlock = function(bemjson) {
            return $(BEMHTML.apply(bemjson)).bem('b-banner-preview-filter2');
        },
        setType = function(block, value) {
            block.findBlockInside('type-filter', 'radio-button').val(value);
        },
        setDevice = function(block, value) {
            block.findBlockInside('device-filter', 'radio-button').val(value);
        },
        setTarget = function(block, value) {
            block.findBlockInside('target-answer', 'check-button').setMod('checked', value ? 'yes' : '');
        },
        getDevice = function(block) {
            return block.findBlockInside('device-filter', 'radio-button').val();
        },
        getDevices = function(block) {
            return block.findBlockInside('device-filter', 'radio-button').findElem('radio').toArray().map(function(elem) {
                return $(elem).find('input').val();
            });
        },
        getTargetValue = function(block) {
            return block.findBlockInside('target-answer', 'check-button').isChecked();
        },
        getTypes = function(block) {
            return block.findBlockInside('type-filter', 'radio-button').findElem('radio').toArray().map(function(elem) {
                return $(elem).find('input').val();
            });
        },
        isTargetPresent = function(block) {
            return !!block.findBlockInside('target-answer', 'check-button');
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true, useFakeServer: true });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('val()', function() {
        it('должен возвращать текущее значение, указанное в контексте bemhtml', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    'context'
                ],
                currentTab: {
                    type: 'context'
                }
            });

            expect(block.val()).to.be.eql({ type: 'context' });
        });

        it('должен возвращать первый таб как текущий, если currentTab не указан', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    'context'
                ]
            });

            expect(block.val()).to.be.eql({ type: 'base' });
        });

        it('должен возвращать текущее значение после его смены пользовательскими действиями', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    'context'
                ],
                currentTab: {
                    type: 'context'
                }
            });

            setType(block, 'base');

            expect(block.val()).to.be.eql({ type: 'base' });
        });

        it('должен возвращать только те ключи в значении, которые применимы к выбранному табу', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'context',
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile', isTargetPresent: true }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'search',
                    device: 'mobile',
                    target: true
                }
            });

            setType(block, 'context');

            expect(block.val()).to.be.eql({
                type: 'context'
            });
        });

        it('должен возвращать только те ключи в значении, которые применимы к выбранному девайсу', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile', isTargetPresent: true }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'search',
                    device: 'mobile',
                    target: true
                }
            });

            setDevice(block, 'desktop');

            expect(block.val()).to.be.eql({
                type: 'search',
                device: 'desktop'
            });
        });

        it('при переключении таба должен возвращать девайс по умолчанию, если в текущем табе девайс не выбран', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    {
                        type: 'context',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' }
                            ],
                            'default': 'mobile'
                        }
                    }
                ],
                currentTab: {
                    type: 'base'
                }
            });

            setType(block, 'context');

            expect(block.val()).to.be.eql({
                type: 'context',
                device: 'mobile'
            });
        });

        it('должен возвращать первый девайс как девайс по умолчанию, если девайс по умолчанию не задан', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    {
                        type: 'context',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' }
                            ]
                        }
                    },
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' },
                                { type: 'tablet' }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'search',
                    device: 'tablet'
                }
            });

            setType(block, 'context');

            expect(block.val()).to.be.eql({
                type: 'context',
                device: 'desktop'
            });
        });
    });

    describe('события', function() {
        it('должен единожды выстреливать событие change с данными нового выбранного таба при переключение вида', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    {
                        type: 'context',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' }
                            ]
                        }
                    },
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'search',
                    device: 'mobile'
                }
            });

            var spy = sinon.spy();
            block.on('change', spy);

            setType(block, 'context');
            sandbox.clock.tick(0);

            sinon.assert.calledWithMatch(spy, sinon.match.any, { value: block.val() });
            sinon.assert.callCount(spy, 1);
        });

        it('должен единожды выстреливать событие change с данными нового выбранного таба при переключение устройства', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'search',
                    device: 'mobile'
                }
            });

            var spy = sinon.spy();
            block.on('change', spy);

            setDevice(block, 'desktop');
            sandbox.clock.tick(0);

            sinon.assert.calledWithMatch(spy, sinon.match.any, { value: block.val() });
            sinon.assert.callCount(spy, 1);
        });

        it('должен единожды выстреливать событие change с данными нового выбранного таба при переключение навигационного ответа', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop', isTargetPresent: true },
                                { type: 'mobile' }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'search',
                    device: 'desktop',
                    target: true
                }
            });

            var spy = sinon.spy();
            block.on('change', spy);

            setTarget(block, false);
            sandbox.clock.tick(0);

            sinon.assert.calledWithMatch(spy, sinon.match.any, { value: block.val() });
            sinon.assert.callCount(spy, 1);
        });
    });

    describe('реакция на действия', function() {
        it('должен перерисовывать устройства при переключении вида', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'base'
                }
            });

            setType(block, 'search');

            expect(getDevices(block)).to.be.eql(['desktop', 'mobile']);
        });

        it('должен правильно выбирать текущее устройство при переключении вида', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' }
                            ]
                        },
                        isTargetPresent: true
                    }
                ],
                currentTab: {
                    type: 'base'
                }
            });

            setType(block, 'search');

            expect(getDevice(block)).to.be.eql('desktop');
        });

        it('должен перерисовывать навигационный ответ при переключении вида', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop', isTargetPresent: true },
                                { type: 'mobile' }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'base'
                }
            });

            setType(block, 'search');
            expect(isTargetPresent(block)).to.be.equal(true);
        });

        it('должен правильно выставлять выбранность навигационного ответа при переключении вида', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop', isTargetPresent: true },
                                { type: 'mobile' }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'base'
                }
            });

            setType(block, 'search');

            expect(getTargetValue(block)).to.be.equal(false);
        });

        it('должен запоминать состояние устройства при переключении на другой вид и обратно', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop' },
                                { type: 'mobile' }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'search',
                    device: 'mobile'
                }
            });

            block.domElem.appendTo('body');

            setType(block, 'base');
            setType(block, 'search');

            expect(getDevice(block)).to.be.equal('mobile');
        });

        it('должен запоминать состояние навигационного ответа при переключении на другой вид и обратно', function() {
            block = createBlock({
                block: 'b-banner-preview-filter2',
                tabList: [
                    'base',
                    {
                        type: 'search',
                        devices: {
                            list: [
                                { type: 'desktop', isTargetPresent: true }
                            ]
                        }
                    }
                ],
                currentTab: {
                    type: 'search',
                    device: 'desktop',
                    target: true
                }
            });

            setType(block, 'base');
            setType(block, 'search');

            expect(getTargetValue(block)).to.be.equal(true);
        });

    });

    describe('интеграция', function() {
        describe('DIRECT-50736', function(){
            it('на виде "на поиске" при выбранном типе устройстве "мобильное" без сайтлинков в группе отсутствует кнопка БНО', function() {
                block = createBlock({
                    block: 'b-banner-preview-filter2',
                    tabList: u['b-banner-preview-filter2'].getTabsList({
                        strategySearchName: 'autobudget',
                        dontShowYacontext: 'No',
                        campMediaType: 'text',
                        banners: [{
                            sitelinks: [ //так выглядит отсутствие сайтлинков
                                { title: '' },
                                { title: '' },
                                { title: '' },
                                { title: '' }
                            ],
                            banner_type: 'desktop'
                        }]
                    }),
                    currentTab: {
                        type: 'search',
                        device: 'mobile'
                    }
                });

                expect(isTargetPresent(block)).to.be.false;
            });

            it('на виде "на поиске" при выбранном типе устройстве "мобильное" и 4 сайтлинках без описания в группе отображается кнопка БНО', function() {
                block = createBlock({
                    block: 'b-banner-preview-filter2',
                    tabList: u['b-banner-preview-filter2'].getTabsList({
                        strategySearchName: 'autobudget',
                        dontShowYacontext: 'No',
                        campMediaType: 'text',
                        banners: [{
                            sitelinks: [
                                { title: '1', url_protocol: 'http://', href: 'ya.ru?1' },
                                { title: '2', url_protocol: 'http://', href: 'ya.ru?2' },
                                { title: '3', url_protocol: 'http://', href: 'ya.ru?3' },
                                { title: '4', url_protocol: 'http://', href: 'ya.ru?4' }
                            ],
                            banner_type: 'desktop'
                        }]
                    }),
                    currentTab: {
                        type: 'search',
                        device: 'mobile',
                        target: true
                    }
                });

                expect(isTargetPresent(block)).to.be.true;
            });

            it('на виде "на поиске" при выбранном типе устройстве "десктоп" без сайтлинков в группе отсутствует кнопка БНО', function() {
                block = createBlock({
                    block: 'b-banner-preview-filter2',
                    tabList: u['b-banner-preview-filter2'].getTabsList({
                        strategySearchName: 'autobudget',
                        dontShowYacontext: 'No',
                        campMediaType: 'text',
                        banners: [{
                            sitelinks: [],
                            banner_type: 'desktop'
                        }]
                    }),
                    currentTab: {
                        type: 'search',
                        device: 'desktop'
                    }
                });

                expect(isTargetPresent(block)).to.be.false;
            });

            it('на виде "на поиске" при выбранном типе устройстве "десктоп" и 4 сайтлинках без описания в группе не отображается кнопка БНО', function() {
                block = createBlock({
                    block: 'b-banner-preview-filter2',
                    tabList: u['b-banner-preview-filter2'].getTabsList({
                        strategySearchName: 'autobudget',
                        dontShowYacontext: 'No',
                        campMediaType: 'text',
                        banners: [{
                            sitelinks: [
                                { title: '1', url_protocol: 'http://', href: 'ya.ru?1' },
                                { title: '2', url_protocol: 'http://', href: 'ya.ru?2' },
                                { title: '3', url_protocol: 'http://', href: 'ya.ru?3' },
                                { title: '4', url_protocol: 'http://', href: 'ya.ru?4' }
                            ],
                            banner_type: 'desktop'
                        }]
                    }),
                    currentTab: {
                        type: 'search',
                        device: 'desktop'
                    }
                });

                expect(isTargetPresent(block)).to.be.false;
            });

            it('на виде "на поиске" при выбранном типе устройстве "десктоп" и 4 сайтлинках с описаниями в группе отображается кнопка БНО', function() {
                block = createBlock({
                    block: 'b-banner-preview-filter2',
                    tabList: u['b-banner-preview-filter2'].getTabsList({
                        strategySearchName: 'autobudget',
                        dontShowYacontext: 'No',
                        campMediaType: 'text',
                        banners: [{
                            sitelinks: [
                                { title: '1', url_protocol: 'http://', href: 'ya.ru?1', description: '1' },
                                { title: '2', url_protocol: 'http://', href: 'ya.ru?2', description: '2' },
                                { title: '3', url_protocol: 'http://', href: 'ya.ru?3', description: '3' },
                                { title: '4', url_protocol: 'http://', href: 'ya.ru?4', description: '4' }
                            ],
                            banner_type: 'desktop'
                        }]
                    }),
                    currentTab: {
                        type: 'search',
                        device: 'desktop',
                        target: true
                    }
                });

                expect(isTargetPresent(block)).to.be.true;
            });
        });

        describe('DIRECT-58948', function() {
            it('Для РМП баннера только с ГО и без баннеров с картинками должны присутствовать вкладки "Базовый вид" и "В сети"', function() {
                block = createBlock({
                    block: 'b-banner-preview-filter2',
                    tabList: u['b-banner-preview-filter2'].getTabsList({
                        strategySearchName: 'autobudget',
                        dontShowYacontext: 'No',
                        campMediaType: 'mobile_content',
                        banners: [{
                            ad_type: 'image_ad'
                        }]
                    }),
                    currentTab: {
                        type: 'search',
                        device: 'desktop'
                    }
                });

                expect(getTypes(block)).to.eql([ 'base', 'context' ]);
            })
        });

        describe('Для cpm_banner кампаний', function() {
            beforeEach(function() {
                block = createBlock({
                    block: 'b-banner-preview-filter2',
                    tabList: u['b-banner-preview-filter2'].getTabsList({
                        strategySearchName: 'stop',
                        dontShowYacontext: 'No',
                        campMediaType: 'cpm_banner',
                        banners: [{
                            ad_type: 'cpm_banner'
                        }]
                    })
                });
            });

            it('Доступна только вкладка "Сети"', function() {
                expect(getTypes(block)).to.eql([ 'context' ]);
            });

            it('Кнопка "Все форматы" недоступна', function() {
                expect(block.elem('all-formats-button').length).to.eql(0);
            });
        });

        describe('Для cpm_deals кампаний', function() {
            beforeEach(function() {
                block = createBlock({
                    block: 'b-banner-preview-filter2',
                    tabList: u['b-banner-preview-filter2'].getTabsList({
                        strategySearchName: 'stop',
                        dontShowYacontext: 'No',
                        campMediaType: 'cpm_deals',
                        banners: [{
                            ad_type: 'cpm_deals'
                        }]
                    })
                });
            });

            it('Доступна только вкладка "Сети"', function() {
                expect(getTypes(block)).to.eql([ 'context' ]);
            });

            it('Кнопка "Все форматы" недоступна', function() {
                expect(block.elem('all-formats-button').length).to.eql(0);
            });
        });

    });


});
