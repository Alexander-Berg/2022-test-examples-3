describe('b-edit-banner-add2', function() {
    var block;

    describe('если не достигнут лимит', function() {
        beforeEach(function() {
            block = u.createBlock({
                block: 'b-edit-banner-add2',
                bannersLimit: 3,
                availableToCreateBannersCount: 2,
                mainTitle: 'Добавить',
                pluralTitles: ['баннер', 'баннера', 'баннеров']
            });
        });

        it('отображается сообщение о доступном для создания количестве объявлений', function() {
            expect(block.elem('text').text()).to.be.eql('В эту группу можно добавить еще 2 баннера');
        });

        it('кнопка добавления активна', function() {
            expect(block._addBtn).to.not.haveMod('disabled');
        });

        it('клик по кнопке добавления вызывает событие событие #add', function() {
            expect(block).to.triggerEvent('add', function() {
                block._addBtn.trigger('click');
            });
        });

        afterEach(function() {
            block.destruct();
        });
    });

    describe('при достижении лимита', function() {
        beforeEach(function() {
            block = u.createBlock({
                block: 'b-edit-banner-add2',
                bannersLimit: 3,
                availableToCreateBannersCount: 0,
                mainTitle: 'Добавить',
                pluralTitles: ['баннер', 'баннера', 'баннеров']
            });
        });

        it('отображается сообщение о лимите', function() {
            expect(block.elem('text').text()).to.be.eql('В группу может быть добавлено не более 3 баннеров');
        });

        it('дизейблится кнопка', function() {
            expect(block._addBtn).to.haveMod('disabled', 'yes');
        });

        it('клик по кнопке добавления не вызывает событие событие #add', function() {
            expect(block).not.to.triggerEvent('add', function() {
                block._addBtn.trigger('click');
            });
        });
    });

    it('setAvailableToCreateBannersCount обновляет отображаемое сообщение', function() {
        block = u.createBlock({
            block: 'b-edit-banner-add2',
            bannersLimit: 3,
            availableToCreateBannersCount: 2,
            mainTitle: 'Добавить',
            pluralTitles: ['баннер', 'баннера', 'баннеров']
        });

        block.setAvailableToCreateBannersCount(0);

        expect(block.findElem('text').text()).to.be.eql('В группу может быть добавлено не более 3 баннеров');
    });

    it('setAvailableToCreateBannersCount обновляет состояние кнопки', function() {
        block = u.createBlock({
            block: 'b-edit-banner-add2',
            bannersLimit: 3,
            availableToCreateBannersCount: 2,
            mainTitle: 'Добавить',
            pluralTitles: ['баннер', 'баннера', 'баннеров']
        });

        block.setAvailableToCreateBannersCount(0);

        expect(block._addBtn).to.haveMod('disabled', 'yes');
    });

    afterEach(function() {
        block.destruct();
    });
});
