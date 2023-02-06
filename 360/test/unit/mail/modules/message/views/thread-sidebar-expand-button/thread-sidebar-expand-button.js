describe('Daria.vThreadSidebarExpandButton', function() {
    beforeEach(function() {
        this.view = ns.View.create('thread-sidebar-expand-button');
        this.sinon.stubGetModel(this.view, ['thread-sidebar-state', 'settings']);
    });

    describe('onClick', function() {
        function testSettingExpanded(that, currentValue, metrikaExpectedArray) {
            that.sinon.stub(that.mSettings, 'getSign').withArgs('right_column_expanded').returns(currentValue);
            that.mSettings.setData(getModelMockByName('settings', 'right_column_expanded_' + (currentValue ? 'true' : 'false')));
            that.sinon.stub(Jane, 'c');
            that.view.onClick();
            expect(that.mSettings.getSetting('right_column_expanded')).to.be.equal(!currentValue);
            expect(Jane.c).to.have.callCount(1);
            expect((Jane.c.getCall(0)).args).to.be.eql(metrikaExpectedArray);
        }
        it('При клике на кнопку меняется режим колонки с развернутой на свернутую и сыпется метрика', function() {
            testSettingExpanded(this, true, ['Правая колонка', 'Плавающая правая колонка', 'Клик в "Свернуть"']);
        });
        it('При клике на кнопку меняется режим колонки со свернутой на развернутую и сыпется метрика', function() {
            testSettingExpanded(this, false, ['Правая колонка', 'Плавающая правая колонка', 'Клик в "Развернуть"']);
        });
    });
});
