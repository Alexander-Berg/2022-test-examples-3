describe('client/modules/common/actions/common/common.js', function() {
    function htmlToNode(html) {
        return $(html)[0];
    }

    function createFakeClickEventOnHtml(html) {
        return {
            type: 'click',
            currentTarget: htmlToNode(html),
            isPropagationStopped: function() {
                // Это нужно, что экшн не отменился.
                return false;
            }
        };
    }

    beforeEach(function() {
        this.sinon.stub(Daria, 'counter');
        this.sinon.stub(Jane, 'c');
    });

    describe('Вызов Jane.c() в случае, если указан параметр metrika в @data-params', function() {

        beforeEach(function() {
            ns.action._process(createFakeClickEventOnHtml(
                '<a class="ns-action" href="#setup" ' +
                    'data-click-action="common.clck" ' +
                    'data-params="metrika=Переключение 3pane:Клик На отдельной странице">' +
                    'Click ooooooon meeeeeee' +
                '</a>'
            ));
        });

        it('Вызывается Jane.c()', function() {
            expect(Jane.c.called).to.be.ok;
        });

        it('Jane.c() вызывается с параметром metrika поспличенным по ":"', function() {
            expect(Jane.c.calledWith([ 'Переключение 3pane', 'Клик На отдельной странице' ])).to.be.ok;
        });

    });

});
