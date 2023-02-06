describe('js/daria.action-log', function() {

    beforeEach(function() {
        /**
         * We dont want side-effect
         * on other tests
         */
        this.sinon.stub(ns.page.current, 'params').value({});
        this.sinon.stub(jQuery, 'post');

        setModelsByMock('message');
    });

    it('Не должен передавать данные о письме в запросе если нету письма', function() {
        Daria.actionLog('message.action.complete', {
            ids: {
                ids: ['1']
            }
        });

        expect(JSON.parse(jQuery.post.args[0][1].data).message).to.be.equal(null);
    });

    it('Не должен передавать данные о письме в запросе если у письма нету даты', function() {
        ns.Model.get('message', {ids: '1'}).setData({
            date: null
        });

        Daria.actionLog('message.action.complete', {
            ids: {
                ids: ['1']
            }
        });

        expect(JSON.parse(jQuery.post.args[0][1].data).message).to.be.equal(null);
    });

    it('Должен передавать mid письма, если выполнено действие над письмом', function() {
        Daria.actionLog('message.action.complete', {
            ids: {
                ids: ['5']
            }
        });

        expect(JSON.parse(jQuery.post.args[0][1].data).message.mid).to.be.eql('5');
    });

    it('Должен передавать tid треда, если выполнено действие над тредом', function() {
        Daria.actionLog('message.action.complete', {
            ids: {
                tids: ['t7']
            }
        });

        expect(JSON.parse(jQuery.post.args[0][1].data).message.tid).to.be.eql('t7');
    });

    it('Должны логироваться параметры поиска на странице отдельного письма после перехода со страницы поиска', function() {
        this.sinon.stub(ns.page.current, 'page').value('message');
        this.sinon.stub(ns.page.history, 'getPrevious').returns('#search?request=hello');
        this.sinon.stub(ns, 'router').returns({ page: 'messages', params: { search: 'search', request: 'hello' } });
        this.sinon.stub(Daria.React, 'getSearchSessionId').returns('1234567');

        Daria.actionLog('message.action.complete', {
            'action': 'show',
            'eventObject': {
                'params': {
                    'ids': '159033361841522064'
                },
                'ids': {
                    'ids': ['159033361841522064'],
                    'mids': ['159033361841522064']
                }
            }
        });

        expect(JSON.parse(jQuery.post.args[0][1].data).filter)
            .to.be.eql({ page: 'message', type: 'search', param: 'request=hello&reqid=1234567' });
    });

});
