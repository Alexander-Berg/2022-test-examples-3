describe('b-creative-list-status-tabs', function() {
    var block,
        getTabElem = function (status) {
            return block.findElem('tab', 'val', status)
        };


    describe('Проверка отрисовки значений в поле со счетчиком', function() {
        before(function() {
            block = u.createBlock({
                block: 'b-creative-list-status-tabs',
                statusData: { wait: 3, rejected: 4, draft: 5, all: 12 }
            });
        });

        after(function() {
            block && block.destruct()
        });

        [
            { name: 'accepted', count: '' },
            { name: 'wait', count: '3' },
            { name: 'rejected', count: '4' },
            { name: 'draft', count: '5' },
            { name: 'all', count: '12' }
        ].map(function(data) {
            it('В табе ' + data.name + ' должен содержаться счетчик ' + data.count, function() {
                expect(block.findElem(block.findElem('tab', 'val', data.name), 'count').text()).to.be.equal(data.count);
            });
        });


    });

    describe('Проверка отрисовки блока', function() {
        afterEach(function() {
            block && block.destruct()
        });

        it('Таб с пустым числом значений должен отрисоваться задисабленным', function() {
            block = u.createBlock({
                block: 'b-creative-list-status-tabs',
                statusData: { wait: 3, rejected: 4, draft: 5, all: 12 }
            }, { inject: true });

            expect(block.tabs).to.haveMod(block.tabs.findElem(getTabElem('accepted'), 'tab'), 'disabled', 'yes');
        });

        it('Если не передано значение activeTab, то активным отрисовывается таб "Все"', function() {
            block = u.createBlock({
                block: 'b-creative-list-status-tabs',
                statusData: { wait: 3, rejected: 4, draft: 5, all: 12 }
            });
            expect(block.tabs).to.haveMod(block.tabs.findElem(getTabElem('all'), 'tab'), 'active', 'yes');
        });

        it('Если передано значение activeTab и оно не задисаблено, то активным отрисовывается таб переданный в activeTab', function() {
            block = u.createBlock({
                block: 'b-creative-list-status-tabs',
                statusData: { wait: 3, rejected: 4, draft: 5, all: 12 },
                activeTab: 'draft'
            });
            expect(block.tabs).to.haveMod(block.tabs.findElem(getTabElem('draft'), 'tab'), 'active', 'yes');
        });

        it('Если передано значение activeTab и оно задисаблено, то активным отрисовывается таб "Все"', function() {
            block = u.createBlock({
                block: 'b-creative-list-status-tabs',
                statusData: { wait: 3, rejected: 4, draft: 5, all: 12 },
                activeTab: 'accepted'
            });
            expect(block.tabs).to.haveMod(block.tabs.findElem(getTabElem('all'), 'tab'), 'active', 'yes');
        });
    });

    describe('Проверка событий', function() {
        after(function() {
            block && block.destruct();
        });

        it('При клике по табу блок триггерит событие с данными { value: "имя таба" } ', function() {
            block = u.createBlock({
                block: 'b-creative-list-status-tabs',
                statusData: { wait: 3, rejected: 4, draft: 5, all: 12 }
            }, { inject: true });

            expect(block).to.triggerEvent('change', { value: 'draft'}, function() {
                block.findBlockInside(getTabElem('draft'), 'link').domElem.click();
            })
        })
    })
});
