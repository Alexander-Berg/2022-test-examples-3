describe('Daria.vComposeAttachOpenDiskMixin', function() {
    beforeEach(function() {
        this.params = { 'test': '42' };
        this.view = ns.View.create('compose-attach-open-disk-mixin');
        this.sinon.stub(this.view, 'params').value(this.params);

        this.$node = $('<div>');
        this.view.$node = this.$node;
        this.view.node = this.$node[0];

        this.sinon.stub(Function.prototype, 'bind').callsFake(function() {
            return this;
        });
    });

    describe('#openDiskInit', function() {
        it('Должен подписаться на клик по виду', function() {
            this.sinon.stub(this.$node, 'on');

            this.view.openDiskInit();

            expect(this.$node.on).to.be.calledWithExactly('click', this.view.openDiskOnClick);
        });
    });

    describe('#openDiskInitDestroy', function() {
        it('Должен отписаться от клика по виду', function() {
            this.sinon.stub(this.$node, 'off');

            this.view.openDiskDestroy();

            expect(this.$node.off).to.be.calledWithExactly('click', this.view.openDiskOnClick);
        });
    });

    describe('#_openDiskOnClick', function() {
        beforeEach(function() {
            this.sinon.stub(ns.View, 'create').returns({
                open: this.openStub = this.sinon.stub().callsFake(() => vow.reject()),
                on: this.sinon.stub()
            });

            this.view['MODE'] = 'test';
            this.view.METRIKA_PARTS.test = 'metrika_part';
            this.view['PATHS']['test'] = 'disk_path';
            this.sinon.stub(Jane, 'c');

            this.sinon.stub(Daria.DiskResources, 'create').returns({});

            this.view._openDiskOnClick();
        });

        it('Должен отправить метрику c QuickReply', function() {
            Jane.c.restore();
            this.sinon.stub(Jane, 'c');

            this.view._openDiskOnClick({ inQuickReply: true });
            expect(Jane.c).to.be.calledWithExactly('Quick Reply', 'Кнопка Прикрепить', 'metrika_part');
        });

        it('Должен создать вид vBrowseDisk со своими параметрами', function() {
            expect(Daria.DiskResources.create).to.have.callCount(1);
        });

        it('Должен открыть вид с правильным путем', function() {
            expect(Daria.DiskResources.create).to.be.calledWith(this.sinon.match({ path: 'disk_path' }));
        });
    });

    describe('#_onDiskPopupClosed', function() {
        it('должен вызвать unmount для попапа', function() {
            this.view._popup = {
                unmount: this.destroyStub = this.sinon.stub()
            };

            return this.view._onDiskPopupClosed().then(() => {
                expect(this.destroyStub).to.have.callCount(1);
            });
        });
    });
});
