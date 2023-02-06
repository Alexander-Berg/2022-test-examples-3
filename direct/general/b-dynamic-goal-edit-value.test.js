describe('b-dynamic-goal-edit-value', function() {
    var block,
        blockTree,
        goal = {
            kind:  'exact',
            value:  [{ value: 'url' }],
            type:  'URL'
        },
        gModel = BEM.MODEL.create({ name: 'b-dynamic-goal-edit' }, goal),
        lim = 100,
        createBlock = function() {
            block = u.createBlock({
                block: 'b-dynamic-goal-edit-value',
                data: BEM.MODEL.create({ name: 'b-dynamic-goal-edit-value'}, { value: 'http://ya.ru' }),
                goal: gModel,
                mods: { 'is-last': 'yes' },
                js: {
                    goalModelId: gModel.id,
                    modelParams: {
                        name: 'b-dynamic-goal-edit-value',
                        id: 'myvalue'
                    }
                }
            });
        },
        getLongString = function() {
            var str = 'long';

            while(str.length < lim) str += 'long';
            str += '.ru';

            return str;
        },
        genUrl = function(length, withHttp) {
            var res = withHttp ? 'http://' : '';

            return res + new Array(length - (withHttp ? 9 : 2)).join('a') + '.ru'
        },
        cleanUp = function() {
            block && block.destruct();
        },
        validationData = [
            {
                rule: 'required',
                title: 'не указана строка',
                val: ''
            },
            {
                rule: 'maxLength',
                title: 'строка слишком длинная',
                val: getLongString()
            },
            {
                rule: 'url',
                title: 'введите корректный url',
                val: 'https://.com'
            },
            {
                rule: 'hasOnlyAllowedSymbols',
                title: 'строка содержит некорректные символы',
                val: '{}--'
            }
        ];
    describe('Валидация', function() {
        beforeEach(function() {
            createBlock();
        });

        afterEach(function() {
            cleanUp();
        });

        validationData.forEach(function(field) {
            it('если ' + field.title + ' - ошибка по правилу ' + field.rule, function() {
                block.model.set('value', field.val);

                var checkResult = block.model.validate();

                var error = u._.find(checkResult.errors, function(errorField) {
                    return errorField.rule == field.rule
                });

                expect(!!error).to.be.equal(true);
            });
        }, this);

        describe('если выбран тип контента URL_prodlist', function() {
            beforeEach(function() {
                block.model.set('isUrl', true);
            });

            it('и указан некорретный URL - ошибка по правилу url', function() {
                block.model.set('value', 'qwerty');

                var checkResult = block.model.validate();

                checkResult.errors.forEach(function(field, i) {
                    expect((field.rule == 'url')).to.be.equal(true);
                });
            });

            //без http - длина до 1017 включительно, с http - до 1024 включительно
            [
                {
                    title: 'длина ссылки со страницей предложений равна 1017 и она не содержит http',
                    val: genUrl(1017, false),
                    isValid: true
                },
                {
                    title: 'длина ссылки со страницей предложений равна 1024 и она содержит http',
                    val: genUrl(1024, true),
                    isValid: true
                },
                {
                    title: 'длина ссылки со страницей предложений равна 1018 и она не содержит http',
                    val: genUrl(1018, false),
                    isValid: false
                },
                {
                    title: 'длина ссылки со страницей предложений равна 1025 и она содержит http',
                    val: genUrl(1025, true),
                    isValid: false
                }
            ].forEach(function(field) {
                it('и если ' + field.title + (field.isValid ? ' - валидное значение' : ' - ошибка'), function() {
                    block.model.set('value', field.val);

                    var checkResult = block.model.validate(),
                        isValid = !!checkResult.valid;

                    expect(isValid).to.be.equal(field.isValid);
                });
            });
        });
    })
});
