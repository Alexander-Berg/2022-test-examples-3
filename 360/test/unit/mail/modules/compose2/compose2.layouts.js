xdescribe('compose2.layouts.js', function() {

    describe('compose-attach-select-location', function() {
        it('если Диск недоступен, должен возвращать только вид добавления аттача с компа', function() {
            this.sinon.stub(ns.Model.get('compose-state'), 'hasDisk').returns(false);

            var layout = ns.layout.page('compose-attach-select-location');

            expect(layout).to.only.have.keys('compose-attach-select-location');
            expect(layout[ 'compose-attach-select-location' ].views).to.only.have.keys(
                'compose-attach-button-computer'
            );
        });

        it('если Диск доступен, должен возвращать виды добавления аттачей с компа, с диска и из почты', function() {
            this.sinon.stub(ns.Model.get('compose-state'), 'hasDisk').returns(true);

            var layout = ns.layout.page('compose-attach-select-location');

            expect(layout).to.only.have.keys('compose-attach-select-location');
            expect(layout[ 'compose-attach-select-location' ].views).to.only.have.keys(
                'compose-attach-button-computer',
                'compose-attach-button-disk',
                'compose-attach-button-mail'
            );
        });
    });

    describe('compose-attach-button-box', function() {
        it('если Диск недоступен, должен возвращать только вид добавления аттача с компа', function() {
            this.sinon.stub(ns.Model.get('compose-state'), 'hasDisk').returns(false);

            var layout = ns.layout.page('compose-attach-button-box');

            expect(layout).to.only.have.keys('compose-attach-button-box');
            expect(layout[ 'compose-attach-button-box' ].views).to.only.have.keys(
                'compose-attach-button-computer-inline'
            );
        });

        it('если Диск доступен, должен возвращать вид кнопки выбора вариантов добавления аттачей', function() {
            this.sinon.stub(ns.Model.get('compose-state'), 'hasDisk').returns(true);

            var layout = ns.layout.page('compose-attach-button-box');

            expect(layout).to.only.have.keys('compose-attach-button-box');
            expect(layout[ 'compose-attach-button-box' ].views).to.only.have.keys(
                'compose-attach-button'
            );
        });
    });
});

