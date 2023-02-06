describe('ns.page.history', function() {
    describe('#getPreviousPageUrl', function() {
        beforeEach(function() {
            this.sinon.stub(ns.page.history, 'getPrevious');
            this.sinon.stub(ns, 'router');

            var urlPageHash = {
                '#compose/1': 'compose',
                '#compose': 'compose',
                '#inbox': 'messages',
                '#message': 'message'
            };

            _(urlPageHash).keys().each(function(url, index) {
                ns.page.history.getPrevious.withArgs(index).returns(url);
            }).value();

            _(urlPageHash).each(function(page, url) {
                ns.router.withArgs(url).returns({ page: page });
            }).value();

            this.getPrevUrl = ns.page.history.getPreviousPageUrl.bind(ns.page.history);
        });

        it('Должен вернуть undefined, если страниц в истории нет', function() {
            ns.page.history.getPrevious.withArgs(0).returns(undefined);

            expect(this.getPrevUrl()).to.be.equal(undefined);
        });

        describe('Критерий соответствия `options.match`', function() {
            it('Должен вернуть первую подходящую страницу (критерий-строка)', function() {
                expect(this.getPrevUrl({ match: 'messages' })).to.be.equal('#inbox');
            });

            it('Должен вернуть первую подходящую страницу (критерий-массив)', function() {
                expect(this.getPrevUrl({ match: ['message', 'messages'] })).to.be.equal('#inbox');
            });

            it('Должен вернуть undefined, если страница, соответсвующая критерию, не была найдена', function() {
                expect(this.getPrevUrl({ match: 'done' })).to.be.equal(undefined);
            });
        });

        describe('Критерий соответствия `options.mismatch`', function() {
            it('Должен вернуть первую подходящую страницу (критерий-строка)', function() {
                expect(this.getPrevUrl({ mismatch: 'compose' })).to.be.equal('#inbox');
            });

            it('Должен вернуть первую подходящую страницу (критерий-массив)', function() {
                expect(this.getPrevUrl({ mismatch: ['compose', 'messages'] })).to.be.equal('#message');
            });

            it('Должен вернуть undefined, если страница, соответсвующая критерию, не была найдена', function() {
                expect(this.getPrevUrl({ mismatch: ['compose', 'messages', 'message'] })).to.be.equal(undefined);
            });
        });

        it('Не должен вернуть страницу, если она одновременно находится и в match, и в ьшыьфеср', function() {
            expect(this.getPrevUrl({
                match: ['message', 'messages', 'compose'],
                mismatch: ['message', 'messages', 'compose']
            })).to.be.equal(undefined);
        });
    });

    describe('#getFirstValidPrevious', function() {
        beforeEach(function() {
            this.sinon.stub(ns.page.history, '_history').value([
                '/touch/label/112233',
                '/touch/label/445566',
                '/touch/folder/112233',
                '/touch/message/445566'
            ]);

            this.sinon.stub(ns.page, 'currentUrl').value('/inbox');
        });

        it('Должен найти самый поздний урл с меткой', function() {
            expect(ns.page.history.getFirstValidPrevious(function(url) {
                if (/label\/\d+/.test(url)) {
                    return true;
                }
            })).to.be.equal('/touch/label/445566');
        });

        it('Должен вернуть null, если ничего не найдено', function() {
            expect(ns.page.history.getFirstValidPrevious(function(url) {
                if (/no_such_url/.test(url)) {
                    return true;
                }
            })).to.be.equal(null);
        });

        it('Должен найти текущий урл, если он подходит под условия', function() {
            expect(ns.page.history.getFirstValidPrevious(function(url) {
                if (/^\/(inbox|touch)/.test(url)) {
                    return true;
                }
            })).to.be.equal(ns.page.currentUrl);
        });

    });
});
