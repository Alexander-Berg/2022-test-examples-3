beforeEach(function() {
    this.sinon = sinon.createSandbox({
        useFakeServer: true
    });

});

afterEach(function() {
    this.sinon.restore();

    // Очистка контекста тестов от созданных в нём, в процессе выполнения, элементов
    for (var key in this) {
        if (this.hasOwnProperty(key)) {
            delete this[key];
        }
    }
});
