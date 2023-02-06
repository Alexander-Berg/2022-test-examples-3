describe('Daria.nsTreeWalker', function() {

    describe('.findView', function() {

        before(function() {
            ns.View.define('tree1-app', {yateModule: 'mail'});
            ns.View.define('tree1-folders', {yateModule: 'mail'});
            ns.View.define('tree1-labels', {yateModule: 'mail'});
            ns.View.define('tree1-messages-wrap', {yateModule: 'mail'});
            ns.View.define('tree1-messages', {yateModule: 'mail'});
        });

        beforeEach(function() {
            ns.layout.define('ns-tree-walker-findViews-1', {
                'tree1-app': {
                    'left-box@': {
                        'tree1-folders': {},
                        'tree1-labels': {}
                    },
                    'right-box@': {
                        'messages-list-box@': {
                            'tree1-messages-wrap': {
                                'tree1-messages': {}
                            }
                        }
                    }
                }
            });

            this.vAPP = ns.View.create('tree1-app');

            return this.vAPP.updateByLayout('ns-tree-walker-findViews-1');
        });

        afterEach(function() {
            ns.layout.undefine('ns-tree-walker-findViews-1');
        });

        it('должен найти вид (vFolders)', function() {
            var toFind = ['left-box', 'tree1-folders'];
            var view = Daria.nsTreeWalker.findView(this.vAPP, toFind);

            expect(view)
                .to.be.instanceof(ns.View)
                .and.to.have.property('id', 'tree1-folders');
        });

        it('должен найти вид (vMessages)', function() {
            var toFind = ['right-box', 'messages-list-box', 'tree1-messages-wrap', 'tree1-messages'];
            var view = Daria.nsTreeWalker.findView(this.vAPP, toFind);

            expect(view)
                .to.be.instanceof(ns.View)
                .and.to.have.property('id', 'tree1-messages');
        });

        it('должен искать с помощью регулярки, если она передана', function() {
            this.sinon.spy(RegExp.prototype, 'test');

            var toFind = ['right-box', 'messages-list-box', 'tree1-messages-wrap', /^tree1-messages$/];
            var view = Daria.nsTreeWalker.findView(this.vAPP, toFind);

            expect(RegExp.prototype.test).to.have.callCount(1);
            expect(view)
                .to.be.instanceof(ns.View)
                .and.to.have.property('id', 'tree1-messages');
        });

        it('должен вернуть null, если вид не найден', function() {
            var toFind = ['left-box', 'collectors'];
            var view = Daria.nsTreeWalker.findView(this.vAPP, toFind);

            expect(view).to.be.equal(null);
        });

    });

});
