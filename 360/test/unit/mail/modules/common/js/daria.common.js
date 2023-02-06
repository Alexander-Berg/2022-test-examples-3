describe('daria.common.js', function() {
    describe('lowerifyObjectKeys', function() {
        it('должен перевести ключи в нижний регистр', function() {
            var testObject = {
                mailTo: '',
                Cc: 'addr',
                SubJect: 'subj'
            };
            var expectObject = {
                mailto: '',
                cc: 'addr',
                subject: 'subj'
            };

            expect(Daria.lowerifyObjectKeys(testObject)).to.be.eql(expectObject);
        });
    });

    describe('#hasSid', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'SIDS').value([ '1', '2', '3' ]);
        });

        it('Должен вернуть true, когда в Daria.SIDS есть переданный сид', function() {
            expect(Daria.hasSid(3)).to.eql(true);
        });

        it('Должен вернуть false, когда в Daria.SIDS нет переданного сида', function() {
            expect(Daria.hasSid(99)).to.be.eql(false);
        });
    });

    describe('#tid2mid', function() {
        it('Должен преобразовать tid в mid', function() {
            expect(Daria.tid2mid('t160722211701786846')).to.eql('160722211701786846');
        });

        it('Должен оставить mid без изменений', function() {
            expect(Daria.tid2mid('160722211701786846')).to.eql('160722211701786846');
        });
    });

    describe('#arrangeMatchingFirstInline ->', function() {
        beforeEach(function() {
            this.matcher = function(item) {
                return item.cool;
            };
        });

        it('перестановки выполняются inline - ссылка на массив остаётся той же самой', function() {
            var ar = [ { id: 1 }, { id: 2 }, { id: 3, cool: true } ];
            var result = Daria.arrangeMatchingFirstInline(ar, this.matcher);
            expect(ar).to.be.equal(result);
        });

        it('элементы удовлетворяющие условию - перемещаются в начало массива, остальные остаются на месте', function() {
            expect(Daria.arrangeMatchingFirstInline([ { id: 1 }, { id: 2 }, { id: 3, cool: true } ], this.matcher))
                .to.be.eql([ { id: 3, cool: true }, { id: 1 }, { id: 2 } ]);
        });

        describe(
            'все элементы удовлетворяющие условию - перемещаются в начало массива, сохраняя порядок ->',
            function() {
                it('несколько элементов, идущие подряд', function() {
                    expect(
                        Daria.arrangeMatchingFirstInline(
                            [ { id: 1 }, { id: 2 }, { id: 3, cool: true }, { id: 4, cool: true } ],
                            this.matcher
                        )
                    ).to.be.eql([ { id: 3, cool: true }, { id: 4, cool: true }, { id: 1 }, { id: 2 } ]);
                });
                it('несколько элементов, идущие вразнобой', function() {
                    expect(
                        Daria.arrangeMatchingFirstInline(
                            [ { id: 1 }, { id: 3, cool: true }, { id: 2 }, { id: 4, cool: true } ],
                            this.matcher
                        )
                    ).to.be.eql([ { id: 3, cool: true }, { id: 4, cool: true }, { id: 1 }, { id: 2 } ]);
                });
                it('несколько элементов, идущие вразнобой + несколько, идущие подряд', function() {
                    expect(
                        Daria.arrangeMatchingFirstInline([
                            { id: 1 },
                            { id: 3, cool: true },
                            { id: 2 },
                            { id: 4, cool: true },
                            { id: 5, cool: true }
                        ], this.matcher))
                        .to.be.eql([
                            { id: 3, cool: true },
                            { id: 4, cool: true },
                            { id: 5, cool: true },
                            { id: 1 },
                            { id: 2 }
                        ]);
                });
            }
        );

        describe('число перестановок минимально', function() {
            it('если элемент уже стоит на нужной позиции - перестановка не выполняется', function() {
                var ar = [ { id: 3, cool: true }, { id: 1 }, { id: 2 } ];
                var spy = this.sinon.spy(ar, 'splice');
                Daria.arrangeMatchingFirstInline(ar, this.matcher);
                expect(spy).to.have.callCount(0);
            });

            it('в массиве ни один элемент не удовлетворяет условию - ничего не переставляется', function() {
                var ar = [ { id: 3 }, { id: 1 }, { id: 2 } ];
                var spy = this.sinon.spy(ar, 'splice');
                Daria.arrangeMatchingFirstInline(ar, this.matcher);
                expect(spy).to.have.callCount(0);
            });

            it('в массиве все элементы удовлетворяет условию - ничего не переставляется', function() {
                var ar = [ { id: 3, cool: true }, { id: 1, cool: true }, { id: 2, cool: true } ];
                var spy = this.sinon.spy(ar, 'splice');
                Daria.arrangeMatchingFirstInline(ar, this.matcher);
                expect(spy).to.have.callCount(0);
            });
        });
    });

    describe('Jane.getHumanSize', function() {
        it('empty string', function() {
            expect(Jane.getHumanSize('')).to.be.equal('');
        });

        it('0', function() {
            expect(Jane.getHumanSize(0)).to.be.equal('0 байт');
        });

        it('object', function() {
            expect(Jane.getHumanSize({})).to.be.equal('');
        });

        it('string', function() {
            expect(Jane.getHumanSize('aaa')).to.be.equal('');
        });

        it('10', function() {
            expect(Jane.getHumanSize(10)).to.be.equal('10 байт');
        });

        it('1025', function() {
            expect(Jane.getHumanSize(1025)).to.be.equal('1 КБ');
        });
    });

    describe('Daria.toIntFlag', function() {
        it('Должен сконвертировать true в 1', function() {
            expect(Daria.toIntFlag(true)).to.be.eql(1);
        });
        it('Должен сконвертировать "true" в 1', function() {
            expect(Daria.toIntFlag('true')).to.be.eql(1);
        });
        it('Должен сконвертировать 1 в 1', function() {
            expect(Daria.toIntFlag(1)).to.be.eql(1);
        });
        it('Должен сконвертировать "1" в 1', function() {
            expect(Daria.toIntFlag('1')).to.be.eql(1);
        });

        it('Должен сконвертировать false в 0', function() {
            expect(Daria.toIntFlag(false)).to.be.eql(0);
        });
        it('Должен сконвертировать "false" в 0', function() {
            expect(Daria.toIntFlag(false)).to.be.eql(0);
        });
        it('Должен сконвертировать 0 в 0', function() {
            expect(Daria.toIntFlag(0)).to.be.eql(0);
        });
        it('Должен сконвертировать "0" в 0', function() {
            expect(Daria.toIntFlag('0')).to.be.eql(0);
        });
        it('Должен сконвертировать undefined в 0', function() {
            expect(Daria.toIntFlag(undefined)).to.be.eql(0);
        });
    });

    describe('Daria.getParamsHash', function() {
        it('не переданы params', function() {
            expect(Daria.getParamsHash()).to.be.equal('');
        });
        it('пустой объект', function() {
            expect(Daria.getParamsHash({})).to.be.equal('');
        });
        it('null', function() {
            expect(Daria.getParamsHash(null)).to.be.equal('');
        });
        it('один параметр', function() {
            expect(Daria.getParamsHash({ a: 1 })).to.be.equal('a=1');
        });
        it('несколько параметров', function() {
            expect(Daria.getParamsHash({ 'a': 1, 'b': false, 'wierd key': 'Hi' }))
                .to.be.equal('a=1&b=false&wierd key=Hi');
        });
        it('порядок ключей в объекте не важен', function() {
            expect(Daria.getParamsHash({ a: 1, b: 'test' })).to.be.equal(Daria.getParamsHash({ b: 'test', a: 1 }));
        });
        describe('значения параметров urlencode-ятся ->', function() {
            it('символы & и = будут заэнкожены', function() {
                expect(Daria.getParamsHash({ a: '=', b: '&' })).to.be.equal('a=%3D&b=%26');
            });
            it('строится однозначный ключ', function() {
                expect(Daria.getParamsHash({ a: '1&b=2' })).to.not.be.equal(Daria.getParamsHash({ a: 1, b: 2 }));
            });
        });
    });

    describe('#promoNow', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'urlParams').value({ debugClientPromoDate: '' });

            Daria._debugClientPromoTimestamp = 0;
        });

        it('Должен вернуть дату из debugClientPromoDate, если в debugClientPromoDate верное значение без TZ',
            function() {
                this.sinon.stub(Daria.urlParams, 'debugClientPromoDate').value('2017-03-30T11:30');
                expect(Daria.promoNow()).to.be.equal(Date.parse('2017-03-30T11:30'));
            }
        );

        it('Должен вернуть дату из debugClientPromoDate, если в debugClientPromoDate верное значение с TZ (+ разница)',
            function() {
                this.sinon.stub(Daria.urlParams, 'debugClientPromoDate').value('2017-03-30T11:30+05:00');
                expect(Daria.promoNow()).to.be.equal(Date.parse('2017-03-30T11:30+05:00'));
            }
        );

        it('Должен вернуть дату из debugClientPromoDate, если в debugClientPromoDate верное значение с TZ' +
            ' с пробелом вместо +, при этом пробел интерпретируется как +',
            function() {
                this.sinon.stub(Daria.urlParams, 'debugClientPromoDate').value('2017-03-30T11:30 05:00');
                expect(Daria.promoNow()).to.be.equal(Date.parse('2017-03-30T11:30+05:00'));
            }
        );

        it('Должен вернуть дату из debugClientPromoDate, если в debugClientPromoDate верное значение с TZ (- разница)',
            function() {
                this.sinon.stub(Daria.urlParams, 'debugClientPromoDate').value('2017-03-30T11:30-05:00');
                expect(Daria.promoNow()).to.be.equal(Date.parse('2017-03-30T11:30-05:00'));
            }
        );

        it('Должен вернуть дату из Daria.now, если нет параметра debugClientPromoDate', function() {
            this.sinon.stub(Daria, 'now').returns(123);
            expect(Daria.promoNow()).to.be.equal(Daria.now());
        });

        describe('Должен вернуть дату из Daria.now, если значение параметра debugClientPromoDate не валидное',
            function() {
                var formats = [ '2017-03-30T11:30  05:00', '2017-03-30T11:30+05:00 ', '2017-03 -30T11', '2017-03-3' ];

                formats.forEach(function(format, index) {
                    it('format ' + index, function() {
                        this.sinon.stub(Daria, 'now').returns(123);
                        this.sinon.stub(Daria.urlParams, 'debugClientPromoDate').value(format);
                        expect(Daria.promoNow()).to.be.equal(Daria.now());
                    });
                });
            }
        );

        describe('Должен вернуть дату из Daria.now, если значение параметра debugClientPromoDate является ' +
            'одним из форматов ISO8106, но не поддерживается функцией',
            function() {
                var formats = [ '2017-03-30T11:30:00-05:00', '2017-03-30T11:30:00', '2017-03-30T11', '2017-03-30' ];

                formats.forEach(function(format, index) {
                    it('format ' + index, function() {
                        this.sinon.stub(Daria, 'now').returns(123);
                        this.sinon.stub(Daria.urlParams, 'debugClientPromoDate').value(format);
                        expect(Daria.promoNow()).to.be.equal(Daria.now());
                    });
                });
            }
        );
    });

    describe('#showGoToThreadControl', function() {
        it('На БП не должен показываться', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(false);
            expect(Daria.showGoToThreadControl()).to.eql(false);
        });
        describe('На корпе должен показываться, если:', function() {
            it('если 3pane + корп + страница поиска + тредный режим', function() {
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.sinon.stub(Daria, 'is3pane').returns(true);
                this.sinon.stub(Daria, 'isSearchPage').returns(true);
                this.mSettings = ns.Model.get('settings');
                this.sinon.stub(ns.Model, 'get').returns(this.mSettings);
                this.sinon.stub(this.mSettings, 'isThreaded').returns(true);
                expect(Daria.showGoToThreadControl()).to.eql(true);
            });
        });
        describe('На корпе должен показываться, если:', function() {
            it('если 3pane + корп + страница поиска + тредный режим и хотя бы одно не выполняется', function() {
                this.sinon.stub(Daria, 'IS_CORP').value(true);
                this.sinon.stub(Daria, 'is3pane').returns(true);
                this.sinon.stub(Daria, 'isSearchPage').returns(false);
                this.mSettings = ns.Model.get('settings');
                this.sinon.stub(ns.Model, 'get').returns(this.mSettings);
                this.sinon.stub(this.mSettings, 'isThreaded').returns(true);
                expect(Daria.showGoToThreadControl()).to.eql(false);
            });
        });
    });

    describe('#invalidateMailHandlers', function() {
        describe('обновление текущей коллекции писем', function() {
            beforeEach(function() {
                this.sinon.stub(ns.Model, 'invalidateAll');
                this.sinon.stub(ns.Model, 'invalidate');
                this.sinon.stub(ns, 'forcedRequest');
                this.sinon.stub(Daria, 'areFoldersTabsEnabled');
                Daria.areFoldersTabsEnabled.returns(false);

                this.mMessages = ns.Model
                    .get('messages', {
                        mid: '1234'
                    })
                    .setData({ message: [] });

                this.mMessagesOnlyNew = ns.Model
                    .get('messages', {
                        mid: '1234',
                        extra_cond: 'only_new'
                    })
                    .setData({ message: [] });

                this.sinon.stub(this.mMessages, 'refresh');
                this.sinon.stub(this.mMessages, 'loadMore');

                this.sinon.stub(this.mMessagesOnlyNew, 'refresh');
                this.sinon.stub(this.mMessagesOnlyNew, 'loadMore');
                this.sinon.stub(this.mMessagesOnlyNew, 'isUnreadMessagesCollection').returns(true);
            });

            describe('Перезапрос моделей при включенных или выключенных табах', function() {
                it('Если табы не включены, перезапрашиваем folders и labels', function() {
                    this.sinon.stub(ns.Model, 'get').returns(this.mMessages);

                    Daria.invalidateMailHandlers(false, 'mailbox.check');
                    expect(ns.forcedRequest).to.be.calledWith([ 'folders', 'labels' ]);
                });

                it('Если табы не включены, перезапрашиваем folders, labels и tabs', function() {
                    this.sinon.stub(ns.Model, 'get').returns(this.mMessages);
                    Daria.areFoldersTabsEnabled.returns(true);

                    Daria.invalidateMailHandlers(false, 'mailbox.check');
                    expect(ns.forcedRequest).to.be.calledWith([ 'folders', 'labels', 'tabs' ]);
                });
            });

            describe('для обычной коллекции писем всегда вызывается refresh()', function() {
                beforeEach(function() {
                    this.sinon.stub(ns.Model, 'get').returns(this.mMessages);
                });

                it('mailbox.check', function() {
                    Daria.invalidateMailHandlers(false, 'mailbox.check');
                    expect(this.mMessages.refresh).to.have.callCount(1);
                });

                it('xiva.invalid_lcn', function() {
                    Daria.invalidateMailHandlers(false, 'xiva.invalid_lcn');
                    expect(this.mMessages.refresh).to.have.callCount(1);
                });

                it('xiva.lcn_changed_after_offline', function() {
                    Daria.invalidateMailHandlers(false, 'xiva.lcn_changed_after_offline');
                    expect(this.mMessages.refresh).to.have.callCount(1);
                });
            });

            describe(
                'для коллекции "только непрочитанные письма" вызывается loadMore когда инициатор - разъехавшийся lcn',
                function() {
                    beforeEach(function() {
                        this.sinon.stub(ns.Model, 'get').returns(this.mMessagesOnlyNew);
                    });

                    it('mailbox.check', function() {
                        Daria.invalidateMailHandlers(false, 'mailbox.check');
                        expect(this.mMessagesOnlyNew.refresh).to.have.callCount(1);
                    });

                    it('xiva.invalid_lcn', function() {
                        Daria.invalidateMailHandlers(false, 'xiva.invalid_lcn');
                        expect(this.mMessagesOnlyNew.loadMore).to.have.callCount(1);
                    });

                    it('xiva.lcn_changed_after_offline', function() {
                        Daria.invalidateMailHandlers(false, 'xiva.lcn_changed_after_offline');
                        expect(this.mMessagesOnlyNew.loadMore).to.have.callCount(1);
                    });
                });
            describe('Проверяем параметры messages для инвалидации', function() {
                beforeEach(function() {
                    this.sinon.stub(ns.Model, 'get').returns(this.mMessagesOnlyNew);
                });
                it('Если есть нормальные параметры, то вызываем ns.Model.get с ними', function() {
                    this.sinon.stub(ns.page, 'current').value({ params: { extra_cond: true, unread: 'unread' } });
                    this.sinon.stub(this.mMessagesOnlyNew, 'canRequest').returns(true);
                    this.sinon.stub(this.mMessagesOnlyNew, '_hasParamsForRequest').returns(true);
                    Daria.invalidateMailHandlers(false, 'xiva.invalid_lcn');
                    expect(ns.Model.get).calledWith('messages', { extra_cond: true, unread: 'unread' });
                });

                it('Если нет нормальных параметров, то вызываем ns.Model.get с ними + allow_empty: true', function() {
                    this.sinon.stub(ns.page, 'current').value({ params: { tab: 'sender' } });
                    this.sinon.stub(this.mMessagesOnlyNew, 'canRequest').returns(true);
                    this.sinon.stub(this.mMessagesOnlyNew, '_hasParamsForRequest').returns(false);
                    Daria.invalidateMailHandlers(false, 'xiva.invalid_lcn');
                    expect(ns.Model.get).calledWith('messages', { allow_empty: true, tab: 'sender' });
                });
            });
        });
    });

    describe('#getPassportTzOffset', function() {
        beforeEach(function() {
            this.sinon.stub(Date.prototype, 'getTimezoneOffset').returns(-180);
            Daria.tz_offset = -360;
        });

        it('Должен вернуть смещение часового пояса в миллисекундах', function() {
            expect(Daria.getPassportTzOffset()).to.be.equal(180 * 60 * 1000);
        });
    });

    describe('#passportNow', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'now').returns(100);
            this.sinon.stub(Daria, 'getPassportTzOffset').returns(10);
        });

        it('Должен вернуть текущую дату с учётом часового пояса из паспорта', function() {
            expect(Daria.passportNow()).to.be.equal(110);
        });
    });

    describe('#getPassportDate', function() {
        beforeEach(function() {
            this.date = new Date(100);
            this.passportDate = new Date(110);
            this.sinon.stub(Daria, 'getPassportTzOffset').returns(10);
        });

        it('Должен вернуть timestamp по дате, скорректированный на часовую зону из паспорта', function() {
            expect(Daria.getPassportDate(this.date)).to.be.eql(this.passportDate);
        });
    });

    describe('#convertPassportDateToTimestamp', function() {
        beforeEach(function() {
            this.date = new Date(110);
            this.sinon.stub(Daria, 'getPassportTzOffset').returns(10);
        });

        it('Должен вернуть timestamp по дате, скорректированный на часовую зону из паспорта', function() {
            expect(Daria.convertPassportDateToTimestamp(this.date)).to.be.equal(100);
        });
    });

    describe('#checkTimelineAvailable', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'IS_CORP').value(false);
            this.sinon.stub(Daria.Config, 'pddDomain').value(undefined);
            this.sinon.stub(Daria.Config, 'workspace').value(false);
        });

        it('Должен вернуть false', function() {
            expect(Daria.checkTimelineAvailable()).to.be.equal(false);
        });

        it('Должен вернуть true для корпа', function() {
            this.sinon.stub(Daria, 'IS_CORP').value(true);
            expect(Daria.checkTimelineAvailable()).to.be.equal(true);
        });

        it('Должен вернуть true для ПДД', function() {
            this.sinon.stub(Daria.Config, 'pddDomain').value('my.domain');
            expect(Daria.checkTimelineAvailable()).to.be.equal(true);
        });

        it('Должен вернуть true для коннекта', function() {
            this.sinon.stub(Daria.Config, 'workspace').value(true);
            expect(Daria.checkTimelineAvailable()).to.be.equal(true);
        });
    });

    describe('#toggleSearchAppMode', function() {
        beforeEach(function() {
            this.$app = $('<div/>');
            this.$search = $('<div/>');

            this.sinon.stub(this.$app, 'toggleClass');
            this.sinon.stub(this.$search, 'toggleClass');
            this.sinon.stub(ns.events, 'trigger');

            this.sinon.stub(window, '$')
                .withArgs('.js-mail-App').returns(this.$app)
                .withArgs('.mail-Header .js-mail-search').returns(this.$search);
        });

        it('активация поиска', function() {
            Daria.toggleSearchAppMode(true);
            expect(this.$app.toggleClass).to.be.calledWithExactly('mail-App-Search', true);
            expect(this.$search.toggleClass).to.be.calledWithExactly('is-folded', false);
            expect(ns.events.trigger).to.be.calledWithExactly('daria:search:toggled', true);
        });

        it('деактивация поиска', function() {
            Daria.toggleSearchAppMode(false);
            expect(this.$app.toggleClass).to.be.calledWithExactly('mail-App-Search', false);
            expect(this.$search.toggleClass).to.be.calledWithExactly('is-folded', true);
            expect(ns.events.trigger).to.be.calledWithExactly('daria:search:toggled', false);
        });
    });

    describe('#toggleVerticalScrollBar', function() {
        beforeEach(function() {
            this.NO_VSCROLL_CLASS = 'mail-Page_no_vscroll';

            this.$html = {
                css: this.sinon.stub(),
                addClass: this.sinon.stub(),
                removeClass: this.sinon.stub(),
                outerWidth: this.sinon.stub()
            };

            this.$whenNoVScrollBarFix = {
                css: this.sinon.stub()
            };

            const jquery = this.sinon.stub(window, '$');
            jquery.withArgs('.js-page').returns(this.$html);
            jquery.withArgs('.when-page-no-vscroll-fix').returns(this.$whenNoVScrollBarFix);
        });

        describe('визуальное скрытие скроллбара', function() {
            it('должен отключить вертикальный скролл на документе', function() {
                Daria.toggleVerticalScrollBar(true);
                expect(this.$html.addClass).to.be.calledWithExactly(this.NO_VSCROLL_CLASS);
            });

            describe('вертикальный скролл ненулевой ширины', function() {
                beforeEach(function() {
                    this.$html.outerWidth.onCall(0).returns(1000); // со скроллом
                    this.$html.outerWidth.onCall(1).returns(1024); // после скрытия скролла
                    this.$html.outerWidth.throws();

                    Daria.toggleVerticalScrollBar(true);
                });

                it('должен выставить правый отступ у документа для компенсации ширины скроллбара', function() {
                    expect(this.$html.css).have.been.calledOnce.and.calledWith({ 'padding-right': 24 });
                });

                it('должен сдвинуть все элементы с классом .when-page-no-vscroll-fix на ширину скроллбара', function() {
                    expect(this.$whenNoVScrollBarFix.css).have.been.calledOnce.and.calledWith({ 'margin-right': 24 });
                });
            });

            describe('вертикальный скролл нулевой ширины', function() {
                beforeEach(function() {
                    this.$html.outerWidth.returns(1000); // стабильная ширина документа вне зависимости от скролла

                    Daria.toggleVerticalScrollBar(true);
                });

                it('не должен выставлять правый отступ у документа', function() {
                    expect(this.$html.css).have.been.notCalled;
                });

                it('не должен трогать элементы с классом .when-page-no-vscroll-fix', function() {
                    expect(this.$whenNoVScrollBarFix.css).have.been.notCalled;
                });
            });
        });

        describe('восстановление отображения скроллбара', function() {
            beforeEach(function() {
                Daria.toggleVerticalScrollBar(false);
            });

            it('должен восстановить вертикальный скролл на документе', function() {
                expect(this.$html.removeClass).have.been.calledOnce.and.calledWithExactly(this.NO_VSCROLL_CLASS);
            });

            it('должен убрать отступ у документа', function() {
                expect(this.$html.css).have.been.calledOnce.and.calledWithExactly({ 'padding-right': 0 });
            });

            it('должен убрать отступ у элементов с классом .when-page-no-vscroll-fix', function() {
                expect(this.$whenNoVScrollBarFix.css).have.been.calledOnce.and.calledWithExactly({ 'margin-right': 0 });
            });
        });
    });

    describe('#isFullThreadPage', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.Page, 'isParamsForSingleThread').returns(true);
            this.sinon.stub(ns.page, 'current').value({ params: { thread_id: 't1', full: 'true' }, page: 'messages' });
        });

        it(
            'Если текущая страница - messages и есть параметры для треда на отдельной странице и есть параметр full, ' +
            'то должен вернуть true',
            function() {
                expect(Daria.isFullThreadPage()).to.equal(true);
            }
        );

        it('Если текущая страница не messages, то должен вернуть false', function() {
            this.sinon.stub(ns.page, 'current').value({ params: { thread_id: 't1', full: 'true' }, page: 'message' });

            expect(Daria.isFullThreadPage()).to.equal(false);
        });

        it('Если нет параметров для треда на отдельной странице, то должен вернуть false', function() {
            Daria.Page.isParamsForSingleThread.returns(false);
            expect(Daria.isFullThreadPage()).to.equal(false);
        });

        it('Если нет параметра full, то должен вернуть false', function() {
            this.sinon.stub(ns.page, 'current').value({ params: { thread_id: 't1' }, page: 'messages' });

            expect(Daria.isFullThreadPage()).to.equal(false);
        });
    });

    describe('#isSearchPage', function() {
        it('Должен вернуть true, на странице поиска', function() {
            expect(Daria.isSearchPage({ page: 'messages', params: { search: 'search' } })).to.eql(true);
        });

        it('Должен вернуть false, на других страницах', function() {
            expect(Daria.isSearchPage({ page: 'messages', params: { } })).to.be.eql(false);
        });
    });

    describe('#isMessagesListPage', function() {
        it('Должен вернуть true, на списке писем', function() {
            expect(Daria.isMessagesListPage({ page: 'messages', params: { } })).to.eql(true);
        });

        it('Должен вернуть false, на других страницах', function() {
            expect(Daria.isMessagesListPage({ page: 'settings', params: { } })).to.be.eql(false);
        });
    });

    describe('#isMessageOpenedInList', function() {
        it('Должен вернуть true, когда открыто письмо в списке', function() {
            expect(Daria.isMessageOpenedInList({ page: 'messages', params: { ids: [ 1 ] } })).to.eql(true);
        });

        it('Должен вернуть false, когда письмо открыто не в списке', function() {
            expect(Daria.isMessageOpenedInList({ page: 'message', params: { ids: [ 1 ] } })).to.be.eql(false);
        });

        it('Должен вернуть false, когда открыт только список', function() {
            expect(Daria.isMessageOpenedInList({ page: 'messages', params: { } })).to.be.eql(false);
        });

        it('Должен вернуть false, когда открыта другая страница', function() {
            expect(Daria.isMessageOpenedInList({ page: 'settings', params: { } })).to.be.eql(false);
        });
    });
});
