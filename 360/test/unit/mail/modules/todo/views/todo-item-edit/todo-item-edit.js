describe('Daria.vTodoItemEdit', function() {
    beforeEach(function() {
        this.view = ns.View.create('todo-item-edit', {
            'list-external-id': 'TtCTbjdbyandex.ru',
            'todo-external-id': '63ce256e8yandex.ru'
        });

        this.mTodoItem = {
            get: this.sinon.stub().withArgs('.list-id').returns('list-id-123')
        };
        this.mTodoList = {
            get: this.sinon.stub().withArgs('.external-id').returns('external-id-234')
        };
        this.mTodoLists = {
            getListById: this.sinon.stub().withArgs('list-id-123').returns(this.mTodoList)
        };

        this.sinon.stub(ns.Model, 'get').withArgs('todo-lists').returns(this.mTodoLists);
        this.sinon.stub(this.view, '_removeItemFrom');

        this.view._removeItemFromLists(this.mTodoItem);
    });

    describe('#_removeItemFromLists', function() {
        it('удаляет дело из основного списка дел, где оно лежит', function() {
            expect(this.view._removeItemFrom).to.be.calledWith(this.mTodoItem, 'external-id-234');
        });

        it('удаляет дело из текущего списка, откуда запущено удаление', function() {
            expect(this.view._removeItemFrom).to.be.calledWith(this.mTodoItem, this.view.params['list-external-id']);
        });

        it('удаляет дело из списка "Сегодня"', function() {
            expect(this.view._removeItemFrom).to.be.calledWith(this.mTodoItem, 'today');
        });

        it('удаляет дело из списка "На этой неделе"', function() {
            expect(this.view._removeItemFrom).to.be.calledWith(this.mTodoItem, 'week');
        });

        it('удаляет дело из списка "Выполненые дела"', function() {
            expect(this.view._removeItemFrom).to.be.calledWith(this.mTodoItem, 'completed');
        });
    });
});
