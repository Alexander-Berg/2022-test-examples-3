xdescribe('vMessagesItemThread', function() {

    beforeEach(function() {
        this.getActualMessagesCount = ns.View.infoLite('messages-item-thread').methods.getActualMessagesCount;
    });

    describe('#getActualMessagesCount', function() {

        beforeEach(function() {
            this.data = {
                modelsCount: 0,
                totalCount: 100,
                portion: 10
            };
        });

        it('Должен вернуть modelsCount если он больше 0', function() {
            var d = this.data;
            var VALUE = 10;
            d.modelsCount = VALUE;
            var result = this.getActualMessagesCount(d.modelsCount, d.totalCount, d.portion);

            expect(result).to.be.equal(VALUE);
        });

        it('Не должен возвращать modelsCount если он равен 0', function() {
            var d = this.data;
            var VALUE = 0;
            d.modelsCount = VALUE;
            var result = this.getActualMessagesCount(d.modelsCount, d.totalCount, d.portion);

            expect(result).not.to.be(VALUE);
        });

        it('Должен вернуть totalCount если нет моделей и он меньше portion', function() {
            var d = this.data;
            var VALUE = 5;
            d.totalCount = VALUE;
            var result = this.getActualMessagesCount(d.modelsCount, d.totalCount, d.portion);

            expect(result).to.be.equal(VALUE);
        });

        it('Должен вернуть portion если нет моделей и он меньше totalCount', function() {
            var d = this.data;
            var VALUE = 5;
            d.portion = VALUE;
            var result = this.getActualMessagesCount(d.modelsCount, d.totalCount, d.portion);

            expect(result).to.be.equal(VALUE);
        });
    });

    describe('#onCheckedReset', function() {
        beforeEach(function() {
            this.vMessagesThreadItem = ns.View.create('messages-item-thread');
            this.sinon.stub(ns.View.infoLite('messages-item').methods, 'onCheckedReset');
        });

        it('Должен вызывать стандартную обработку сброса выбранности для messages-item', function() {
            this.vMessagesThreadItem.onCheckedReset();
            expect(ns.View.infoLite('messages-item').methods.onCheckedReset).to.have.callCount(1);
        });

        it('Должен сбрасывать колличество выбранных писем, входящих в тред', function() {
            this.vMessagesThreadItem.onCheckedReset();

            expect(this.vMessagesThreadItem.getModel('messages-item-thread-state').get('.checkedCount')).to.be.equal(0);
        });
    });
});
