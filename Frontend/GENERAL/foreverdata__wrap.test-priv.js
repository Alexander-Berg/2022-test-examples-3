describeBlock('foreverdata__wrap', function(block) {
    var data;

    stubBlocks([
        'foreverdata__wrap-merge',
        'foreverdata__wrap-prepare'
    ]);

    beforeEach(function() {
        data = stubData('cgi', 'experiments');
    });

    describe('for outer network and pre-search state', function() {
        beforeEach(function() {
            data.reqdata.is_yandex_net = false;
            data.entry = 'pre-search';
        });

        it('should not have foreverdata properties', function() {
            block(data);

            assert.isUndefined(data.isForeverData);
            assert.isUndefined(data.isForeverSend);
        });
    });

    describe('for inner network and post-search state', function() {
        beforeEach(function() {
            data.reqdata.is_yandex_net = true;
            data.entry = 'post-search';
        });

        it('should set props to false if flags do not exist and forever data is absent', function() {
            block(data);

            assert.isFalse(data.isForeverData);
            assert.isFalse(data.isForeverSend);
        });

        it('should set props to true if the foreverdata flag == 1 and forever data is absent',
            function() {
                data.reqdata.flags.foreverdata = 1;

                block(data);

                assert.isTrue(data.isForeverData);
                assert.isTrue(data.isForeverSend);
            }
        );

        it('should set props to false if the foreverdata flag exists but != 1 and forever data is absent',
            function() {
                data.reqdata.flags.foreverdata = 42;

                block(data);

                assert.isFalse(data.isForeverData);
                assert.isFalse(data.isForeverSend);
            }
        );

        // запрос к форевердате отправляется в т. ч. если значение флага == 1
        // но по этому хешу нет данных, и поэтому оно зарезервировано для отображения интерфейса
        // однако объект с данными (с ошибкой) всё равно приходит
        it('should set props to true if the foreverdata flag == 1 and forever data is present',
            function() {
                data.forever = {};
                data.reqdata.flags.foreverdata = 1;

                block(data);

                assert.isTrue(data.isForeverData);
                assert.isTrue(data.isForeverSend);
            }
        );

        it('should set props to true/false if the interface flag exists and forever data is present',
            function() {
                data.forever = {};
                data.reqdata.flags['foreverdata-show'] = 1;

                block(data);

                assert.isTrue(data.isForeverData);
                assert.isFalse(data.isForeverSend);
            }
        );

        it('should set props to true/false if both flags exist and forever data is present',
            function() {
                data.forever = {};
                data.reqdata.flags.foreverdata = 42;
                data.reqdata.flags['foreverdata-show'] = 1;

                block(data);

                assert.isTrue(data.isForeverData);
                assert.isFalse(data.isForeverSend);
            }
        );

        it('should set props to false if the flags does not exists and forever data is present',
            function() {
                data.forever = {};

                block(data);

                assert.isFalse(data.isForeverData);
                assert.isFalse(data.isForeverSend);
            }
        );

        it('should set props to false if the flag exists but != 1 and forever data is present',
            function() {
                data.forever = {};
                data.reqdata.flags.foreverdata = 42;

                block(data);

                assert.isFalse(data.isForeverData);
                assert.isFalse(data.isForeverSend);
            }
        );
    });
});
