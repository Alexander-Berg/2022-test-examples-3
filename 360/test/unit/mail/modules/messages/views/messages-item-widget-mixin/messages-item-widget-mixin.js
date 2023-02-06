describe('Daria.mMessagesItemWidgetMixin', function() {
    beforeEach(function() {
        this.view = ns.View.create('messages-item-widget-mixin', { ids: '123456' });
    });

    describe('#_getWidgetMetrika', function() {
        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: 'widget-ids' })
                .setData({
                    widgets: [
                        {
                            info: {
                                type: 'calendar',
                                subtype: 'updated',
                                valid: true,
                                showType: 'list'
                            }
                        }
                    ]
                });
        });

        describe('Название виджета', function() {
            it('для валидного виджета - это его тип', function() {
                expect(this.view._getWidgetMetrika(this.mMessage)).to.be.eql([
                    'Виджеты',
                    'calendar',
                    'updated',
                    'показ',
                    '+'
                ]);
            });

            it('для невалидного виджета - это его тип с суффиксом "Протухший"', function() {
                this.sinon.stub(this.mMessage, 'getMessagesListWidget').returns({
                    info: {
                        type: 'calendar',
                        subtype: 'updated',
                        valid: false,
                        showType: 'list'
                    }
                });

                expect(this.view._getWidgetMetrika(this.mMessage)).to.be.eql([
                    'Виджеты',
                    'Протухший calendar',
                    'updated',
                    'показ',
                    '+'
                ]);
            });
        });

        describe('Название кнопки / ссылки', function() {
            sit('pkpass', true);
            sit('avia', true);
            sit('hotels', true);
            sit('calendar', true);

            sit('st', false);

            it('не логируется для pkpass, если пустое', function() {
                this.sinon.stub(this.mMessage, 'getMessagesListWidget').returns({
                    info: {
                        type: 'pkpass',
                        subtype: 'updated',
                        valid: true,
                        showType: 'list'
                    }
                });

                expect(this.view._getWidgetMetrika(this.mMessage, { label: '' })).to.be.eql([
                    'Виджеты',
                    'pkpass',
                    'updated',
                    'показ',
                    '+'
                ]);
            });

            function sit(type, shouldLog) {
                it((shouldLog ? 'логируется' : 'не логируется') + ' для ' + type, function() {
                    this.sinon.stub(this.mMessage, 'getMessagesListWidget').returns({
                        info: {
                            type: type,
                            subtype: 'updated',
                            valid: true,
                            showType: 'list'
                        }
                    });
                    if (shouldLog) {
                        expect(this.view._getWidgetMetrika(this.mMessage, { label: 'кнопка' })).to.be.eql([
                            'Виджеты',
                            type,
                            'updated',
                            'кнопка',
                            'показ',
                            '+'
                        ]);
                    } else {
                        expect(this.view._getWidgetMetrika(this.mMessage, { label: 'кнопка' })).to.be.eql([
                            'Виджеты',
                            type,
                            'updated',
                            'показ',
                            '+'
                        ]);
                    }
                });
            }
        });

        describe('действие', function() {
            it('клик', function() {
                expect(this.view._getWidgetMetrika(this.mMessage, { label: 'кнопка', isClick: true })).to.be.eql([
                    'Виджеты',
                    'calendar',
                    'updated',
                    'кнопка',
                    'клик'
                ]);
            });

            it('показ', function() {
                expect(this.view._getWidgetMetrika(this.mMessage, { label: 'кнопка', isClick: false })).to.be.eql([
                    'Виджеты',
                    'calendar',
                    'updated',
                    'кнопка',
                    'показ',
                    '+'
                ]);
            });
        });
    });
});
