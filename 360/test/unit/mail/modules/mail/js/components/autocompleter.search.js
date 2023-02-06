describe('Поисковый саджест ', function() {
    describe('#enumerate', function() {
        it('должен уметь нумеровать элементы массива', function() {
            var array = [{ a: 'a' }, { b: 'b' }];

            var enumeratedArray = [
                { a: 'a', ix: 3 },
                { b: 'b', ix: 4 }
            ];

            expect(Daria.Autocompleter.enumerate(array, 3, 'ix')).to.be.eql(enumeratedArray);
        });
    });

    describe('Логирование саджеста', function() {
        var mockSelectSuggestInfo;
        var mockFetchSuggestInfo;
        var mockSuggestInfo;

        beforeEach(function() {
            this.sinon.spy(Daria.Autocompleter, 'logSuggestInfo');
            mockSelectSuggestInfo = {
                suggest_group: 'history',
                user_input: 'lol',
                user_select_idx: 2,
                idx: 2,
                suggest_text: 'lol lolovich'
            };

            mockFetchSuggestInfo = {
                user_input: 'lol',
                suggest_group: 'history',
                empty_suggest: false
            };

            mockSuggestInfo = {};

            this.originalUid = Daria.uid;
            Daria.uid = 248;

            this.sinon.stub(Jane.ErrorLog, 'send');
            this.sinon.stub(Daria.React, 'getSearchId');
        });

        afterEach(function() {
            Daria.uid = this.originalUid;
        });

        describe('#logSuggestInfo', function() {
            it('должен логировать информацию через ErrorLog.send c проставленным uid', function() {
                Daria.Autocompleter.logSuggestInfo(mockSuggestInfo, 'ololo');

                expect(Jane.ErrorLog.send).to.be.calledWithMatch({ uid: 248 });
            });
        });
    });
});
