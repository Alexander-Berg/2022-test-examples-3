const helperOperation = require('helpers/operation');

describe('Модель операции', () => {
    describe('при подписке на изменение статусов', () => {
        beforeEach(function(done) {
            this.history = [];
            this.operation = helperOperation.initialize('stub');

            ['created', 'started', 'progressing', 'done'].forEach(function(status) {
                this.operation.on('status.' + status, () => {
                    this.history.push(this.operation.get('.status'));
                });
            }, this);

            this.operation.on('status.done', done.bind(null, null));

            this.operation.run();
        });

        afterEach(function() {
            delete this.operation;
        });

        it('должны быть вызваны обработчики в определенном порядке', function() {
            expect(this.history).to.be.eql(['created', 'started', 'progressing', 'done']);
        });
    });
});
