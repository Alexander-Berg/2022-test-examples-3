describe('Daria.vThreadSidebar', function() {
    beforeEach(function() {
        this.view = ns.View.create('thread-sidebar', {
            thread_id: 't1',
            mid: '1',
            ids: '1',
            from: 'ekhurtina@yandex-team.ru'
        });
        this.view.$node = $("<div></div>");
        this.sinon.stubGetModel(this.view, ['thread-sidebar-state', 'thread-sidebar', 'settings']);
    });
    describe('toggleExpand', function() {
        function testColumnIsCollapsed(that, isColumnFlight, isExpand, expectedValue) {
            that.sinon.stub(that.view, 'isColumnFlight').returns(isColumnFlight);
            that.sinon.stub(that.mSettings, 'getSign').withArgs('right_column_expanded').returns(isExpand);

            that.view.toggleExpand();

            expect(that.view.$node.hasClass('is-collapsed')).to.be.equal(expectedValue);
        }

        describe('-> Если колонка свернута уже, то класс is-collapsed не снимается', function() {
            beforeEach(function() {
                this.view.$node.addClass('is-collapsed');
            });
            it('-> Колонка плавающая и свернута - класс is-collapsed есть', function() {
                testColumnIsCollapsed(this, true, false, true);
            });
            it('-> Колонка не плавающая и свернута - класса is-collapsed нет', function() {
                testColumnIsCollapsed(this, false, false, false);
            });
            it('-> Колонка не плавающая и развернута - класса is-collapsed нет', function() {
                testColumnIsCollapsed(this, false, true, false);
            });
        });
        describe('-> Если колонка развернута, то класса is-collapsed не будет', function() {
            beforeEach(function() {
                this.view.$node.removeClass('is-collapsed');
            });
            it('-> Колонка не плавающая и развернута - класса is-collapsed нет', function() {
                testColumnIsCollapsed(this, false, true, false);
            });
            it('-> Колонка не плавающая и свернута - класса is-collapsed нет', function() {
                testColumnIsCollapsed(this, false, false, false);
            });
            it('-> Колонка плавающая и развернута - класса is-collapsed нет', function() {
                testColumnIsCollapsed(this, true, true, false);
            });
        });
    });
});
