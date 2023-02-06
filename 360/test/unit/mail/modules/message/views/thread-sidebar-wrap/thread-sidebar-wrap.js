describe('Daria.vThreadSidebarWrap', function() {

    beforeEach(function() {
        return ns.forcedRequest([ 'folders' ]);
    });

    describe('#checkShow', function() {
        it('не должен создать вид правой колонки, если письмо в папке "спам"', function() {
            this.vThreadSidebarWrap = ns.View.create('thread-sidebar-wrap', { ids: '5' });
            return ns.forcedRequest('message', { ids: '5' }).then(function(data) {
                this.sinon.stub(this.vThreadSidebarWrap, 'getModel').withArgs('message').returns(data[0]);
                expect(this.vThreadSidebarWrap.checkShow()).to.be.equal(false);
            }, this);
        });

        it('не должен создать вид правой колонки, если письмо в папке "удаленные"', function() {
            this.vThreadSidebarWrap = ns.View.create('thread-sidebar-wrap', { ids: '4' });
            return ns.forcedRequest('message', { ids: '4' }).then(function(data) {
                this.sinon.stub(this.vThreadSidebarWrap, 'getModel').withArgs('message').returns(data[0]);
                expect(this.vThreadSidebarWrap.checkShow()).to.be.equal(false);
            }, this);
        });

        it('должен создать вид правой колонки, если письмо не в папке "удаленные" или "спам"', function() {
            this.vThreadSidebarWrap = ns.View.create('thread-sidebar-wrap', { ids: '666' });
            return ns.forcedRequest('message', { ids: '666' }).then(function(data) {
                this.sinon.stub(this.vThreadSidebarWrap, 'getModel').withArgs('message').returns(data[0]);
                expect(this.vThreadSidebarWrap.checkShow()).to.be.equal(true);
            }, this);
        });
    });
});
