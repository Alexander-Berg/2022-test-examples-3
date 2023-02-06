describe('Daria.vMessagesHeadersMixin', function() {

    describe('#_initHeaders', function() {

        beforeEach(function() {
            this.view = ns.View.create('messages');
            this.view.$node = $('<div/>');
        });

        describe('иницилизация только для поисковой выдачи у пользователя', function() {

            sit('фича Top Results активна, но это не результаты поиска -> заголовки не иницилизируются', false, false);
            sit('фича Top Results активна, это результаты поиска -> заголовки иницилизируются', true, true);

            function sit(testTitle, isSearchResults, shouldInitialize) {
                it(testTitle, function() {
                    this.sinon.stub(this.view, 'isSearchResults').returns(isSearchResults);

                    this.view._initHeaders();

                    if (shouldInitialize) {
                        expect(!!this.view.$headerTopResults).to.be.ok;
                        expect(!!this.view.$headerOtherResults).to.be.ok;
                    } else {
                        expect(this.view.$headerTopResults).to.not.be.ok;
                        expect(this.view.$headerOtherResults).to.not.be.ok;
                    }
                });
            }

        });

    });

    describe('#_destroyHeaders', function() {

        beforeEach(function() {
            this.view = ns.View.create('messages');
            this.view.$node =
                $('<div>' +
                    '<div class="js-messages-list">' +
                        '<div class="js-is-search-top-result"></div>' +
                        '<div class="js-messages-item"></div>' +
                    '</div>' +
                '</div>');
        });

        describe('заголовки не инициализарованы ->', function() {

            beforeEach(function() {
                this.sinon.stub(this.view, 'isSearchResults').returns(false);

                this.view._initHeaders();
            });

            it('вызов _destroyHeaders ничего не делает, ссылок на ноды заголовков нет', function() {
                this.view._destroyHeaders();

                expect(this.view.$headerTopResults).to.not.be.ok;
                expect(this.view.$headerOtherResults).to.not.be.ok;
            });
        });

        describe('заголовки инициализарованы', function() {

            beforeEach(function() {
                this.sinon.stub(this.view, 'isSearchResults').returns(true);

                this.view._initHeaders();

                expect(this.view.$headerTopResults).to.be.ok;
                expect(this.view.$headerOtherResults).to.be.ok;
            });

            it('отписываемся от событий', function() {
                var spy = this.sinon.spy();
                var $headerTopResults = this.view.$headerTopResults;
                this.view.$headerTopResults.on('test-event', spy);

                this.view._destroyHeaders();
                $headerTopResults.trigger('test-event');

                expect(spy).to.have.callCount(0);
            });

            it('удаляем заголовок из DOM-а', function() {
                var spy = this.sinon.spy();
                var $headerTopResults = this.view.$headerTopResults;

                expect(this.view.$node.find($headerTopResults).length).to.be.equal(1);

                this.view._destroyHeaders();

                expect(this.view.$node.find($headerTopResults).length).to.be.equal(0);
            });

            it('удаляем ссылки на DOM ноды', function() {
                var spy = this.sinon.spy();
                this.view._destroyHeaders();

                expect(this.view.$headerTopResults).to.not.be.ok;
                expect(this.view.$headerOtherResults).to.not.be.ok;
            });

        });

    });

    describe('#_positionHeaderBefore', function() {

        beforeEach(function() {
            this.view = ns.View.create('messages');
            this.view.$node =
                $('<div>' +
                    '<div class="js-messages-list">' +
                        '<div class="js-is-search-top-result"></div>' +
                    '</div>' +
                '</div>');

            this.view2 = ns.View.create('messages');
            this.view2.$node =
                $('<div>' +
                    '<div class="js-messages-list">' +
                        '<div class="js-is-search-top-result">1</div>' +
                        '<div class="js-messages-item">2</div>' +
                        '<div class="js-messages-item">3</div>' +
                    '</div>' +
                '</div>');
        });

        it('если заголовка нет - нечего не делаем', function() {
            this.sinon.stub(this.view, 'isSearchResults').returns(false);

            this.view._initHeaders();

            expect(this.view.$headerTopResults).to.not.be.ok;
            expect(this.view.$headerOtherResults).to.not.be.ok;
        });

        describe('если нет ноды, относительно которой позиционируемся', function() {

            beforeEach(function() {
                this.sinon.stub(this.view, 'isSearchResults').returns(true);

                this.view._initHeaders();

                // Заголовки:
                // - Top Results в DOM-е - есть (нода есть - показываем заголовок)
                // - остальных результатов - нет (не отображает заголовок, если нет ноды для позиции)
                expect(this.view.$node.find(this.view.$headerTopResults).length).to.be.equal(1);
                expect(this.view.$node.find(this.view.$headerOtherResults).length).to.be.equal(0);
            });

            it('не был в DOM-е - ничего не делаем', function() {
                this.view._updateHeaders();

                expect(this.view.$node.find(this.view.$headerOtherResults).length).to.be.equal(0);
            });

            it('был в DOM-е => удаляем', function() {
                // нода есть - показывается, нода ушла - скрывается
                this.view.$node.find('.js-is-search-top-result').remove();
                this.view._updateHeaders();

                expect(this.view.$node.find(this.view.$headerTopResults).length).to.be.equal(0);
            });
        });

        describe('комплексные сценарии ->', function() {

            beforeEach(function() {
                this.sinon.stub(this.view, 'isSearchResults').returns(true);
                this.sinon.stub(this.view2, 'isSearchResults').returns(true);

                this.sinon.stub(Jane, 'tt').callsFake(function(template) {
                    if (template === 'messages:vMessages-header-top-results') {
                        return '<div class="test-header-top"/>';
                    } else if (template === 'messages:vMessages-header-other-results') {
                        return '<div class="test-header-other"/>';
                    }
                });

                this.sinon.stub(this.view2, '_getHeaderOtherResultsRenderData').returns({
                    headerText: 'Остальные',
                    messagesCount: 10
                });

                this.view._initHeaders();
                this.view2._initHeaders();

                // Заголовки:
                // - Top Results в DOM-е - есть (нода есть - показываем заголовок)
                // - остальных результатов - нет (не отображает заголовок, если нет ноды для позиции)
                expect(this.view.$node.find(this.view.$headerTopResults).length).to.be.equal(1);
                expect(this.view.$node.find(this.view.$headerOtherResults).length).to.be.equal(0);

                // У второго вида - оба заголовка.
                expect(this.view2.$node.find(this.view2.$headerTopResults).length).to.be.equal(1);
                expect(this.view2.$node.find(this.view2.$headerOtherResults).length).to.be.equal(1);
            });

            it('была нода для позиционирования - был заголовок => ' +
                'удалили ноду - скрылся заголовок => ' +
                'вернули ноду - заголовок показался', function() {

                expect(this.view.$node.find(this.view.$headerTopResults).length).to.be.equal(1);

                var $topResultItem = this.view.$node.find('.js-is-search-top-result').remove();
                this.view._updateHeaders();
                expect(this.view.$node.find(this.view.$headerTopResults).length).to.be.equal(0);

                this.view.$node.find('.js-messages-list').append($topResultItem);
                this.view._updateHeaders();
                expect(this.view.$node.find(this.view.$headerTopResults).length).to.be.equal(1);
            });

            it('правильно позиционируется после пересортировки нод в контейнере', function() {
                // Начальная расположение нод в контейнере:
                expect(this.view2.$node.find('.js-messages-list').html())
                    .to.be.equal(
                        '<div><div class="test-header-top"></div></div>' +
                        '<div class="js-is-search-top-result">1</div>' +
                        '<div><div class="test-header-other"></div></div>' +
                        '<div class="js-messages-item">2</div>' +
                        '<div class="js-messages-item">3</div>'
                    );

                // У ns.ViewCollection есть метод __sortViewItems(), который сортирует элементы коллекции в DOM-е.
                // Эмулируем ситуацию, после которой заголовки могут оказаться в конце списка.
                this.view2.$headerOtherResults.appendTo(this.view2.$node.find('.js-messages-list'));
                this.view2.$headerTopResults.appendTo(this.view2.$node.find('.js-messages-list'));

                expect(this.view2.$node.find('.js-messages-list').html())
                    .to.be.equal(
                        '<div class="js-is-search-top-result">1</div>' +
                        '<div class="js-messages-item">2</div>' +
                        '<div class="js-messages-item">3</div>' +
                        '<div><div class="test-header-other"></div></div>' +
                        '<div><div class="test-header-top"></div></div>'
                    );

                // После вызова _updateHeaders() - все должны встать на свои места.
                this.view2._updateHeaders();
                expect(this.view2.$node.find('.js-messages-list').html())
                    .to.be.equal(
                        '<div><div class="test-header-top"></div></div>' +
                        '<div class="js-is-search-top-result">1</div>' +
                        '<div><div class="test-header-other"></div></div>' +
                        '<div class="js-messages-item">2</div>' +
                        '<div class="js-messages-item">3</div>'
                    );
            });
        });

    });

});
