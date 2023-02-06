xdescribe('Daria.vSetupTodo', function() {
    beforeEach(function() {
        // инитим обязательную модель
        ns.Model.get("settings").setData(mock['handler.settings']);

        this.bSetupTodo = ns.View.create('setup-todo');
        this.sinon.stub(nb, 'init');

        return this.bSetupTodo.update();
    });

    it('should initialize nanoislands inside block', function() {
        expect(nb.init).to.have.callCount(1);
    });
});
