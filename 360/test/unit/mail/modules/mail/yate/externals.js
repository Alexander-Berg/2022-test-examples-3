describe('externals.js', function() {
    describe('#get-count-attachments-to-show ->', function() {
        beforeEach(function() {
            this.getCountAttachmentsToShow = yr.externals['get-count-attachments-to-show'];
        });

        describe('параметры функции ->', function() {
            describe('принимает на входе массив ->', function() {
                sit('пустой массив => 0', [], 0);

                sit('массив длинной менее 3 => все', [1, 2], 2);

                sit('один аттач => 1', [1], 1);

                sit('два аттача => 2', [1, 2], 2);

                sit('три аттача => 3', [1, 2, 3], 3);

                sit('четыре аттача => 3', [1, 2, 3, 4], 3);
            });

            sit('принимает на входе объект (одиночный аттач)', {}, 1);
        });

        function sit(testTitle, attachments, expectedResult) {
            it(testTitle, function() {
                this.sinon.stub(yr, 'nodeset2data').returns(attachments);
                expect(this.getCountAttachmentsToShow()).to.be.equal(expectedResult);
            });
        }
    });
});
