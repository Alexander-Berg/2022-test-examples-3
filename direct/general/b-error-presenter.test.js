describe('b-error-presenter', function() {
    var block,
        blocks,
        flatHash = {
            "groups[0].performance_filters": [{"text": "error 3"}],
            "groups[0].performance_filters[0]": [{"text": "error 4"}],
            "groups[0].performance_filters[0].filter_name": [
                {
                    "text": "Используются недопустимые символы",
                    "description": "В поле \"Название фильтра\" допускается использование букв латинского, турецкого, русского, украинского, казахского алфавитов, цифр и знаков пунктуации"
                },
                {
                    "text": "Используются недопустимые символы",
                    "description": "В поле \"Название фильтра\" допускается использование букв латинского, турецкого, русского, украинского, казахского алфавитов, цифр и знаков пунктуации"
                }
            ],
            "groups[0].performance_filters[0].condition": [{"text": "error 5"}],
            "groups[0].performance_filters[0].condition[2]": [
                {
                    "text": "Неверный формат",
                    "description": "Неправильный формат правила: значение содержит недопустимые символы"
                },
                {
                    "text": "Неверный формат",
                    "description": "Неправильный формат правила: значение содержит недопустимые символы"
                }
            ]
        },
        expectedArgsVariants = {
            groups: []
                .concat(flatHash['groups[0].performance_filters'])
                .concat(flatHash['groups[0].performance_filters[0]']),
            'groups[0].performance_filters[0].filter_name': flatHash['groups[0].performance_filters[0].filter_name'],
            'groups[0].performance_filters[0].condition': []
                .concat(flatHash['groups[0].performance_filters[0].condition'])
                .concat(flatHash['groups[0].performance_filters[0].condition[2]']),
            'fake.empty.path': null
        },
        getErrorBlocks = function(blocks, path) {
            return blocks.filter(function(b) { return b.params.path == path; })
        };

    before(function() {
        // блок-заглушка, реализующий i-error-view-interface
        BEM.DOM.decl({ block: 'b-error-presenter-test-error-output', implements: 'i-error-view-interface' }, {
            showErrors: function(errors) {},
            clearErrors: function() {}
        });
    });

    beforeEach(function() {
        block = u.createBlock({
            block: 'b-error-presenter',
            content: u._.keys(expectedArgsVariants).map(function(path) {
                return { block: 'b-error-presenter-test-error-output', js: { path: path } };
            })
        });

        blocks = block.findBlocksByInterfaceInside('i-error-view-interface').map(function(viewBlock) {
            viewBlock.showErrors = sinon.spy();
            viewBlock.clearErrors = sinon.spy();

            return viewBlock;
        });
    });

    afterEach(function() {
        block.destruct();
    });

    u._.forOwn(expectedArgsVariants, function(expectArgs, path) {

        describe('путь `' + path + '`', function() {

            expectArgs ?

                it('в showErrors блока вывода ошибки передаётся массив ошибок', function() {
                    block.showErrors(flatHash);

                    expect(u._.every(getErrorBlocks(blocks, path), function(viewBlock) {
                        return viewBlock.showErrors.calledWith(expectArgs);
                    })).to.be.true;
                }) :

                it('showErrors блока вывода ошибки не вызывается', function() {
                    block.showErrors(flatHash);

                    expect(u._.every(getErrorBlocks(blocks, path), function(viewBlock) {
                        return !viewBlock.showErrors.called;
                    })).to.be.true;
                });

            it('при вызове clearErrors("' + path + '") у каждого блока с путём ' + path + ', для которого был вызван showErrors, должен вызываться clearErrors', function() {
                block.showErrors(flatHash);
                block.clearErrors(path);

                expect(u._.every(getErrorBlocks(blocks, path), function(viewBlock) {
                    return viewBlock.showErrors.called ?
                        viewBlock.clearErrors.called :
                        !viewBlock.clearErrors.called;
                })).to.be.true;
            });

            it('clearErrors("' + path + '") должен возвращать список оставшихся ошибок (либо пустой, либо со строками путей)', function() {
                var errorsLeft;

                block.showErrors(flatHash);

                errorsLeft = block.clearErrors(path);

                expect(errorsLeft).to.be.an.array;
                expect(u._.every(errorsLeft, function(key) { return key in expectedArgsVariants; })).to.be.true;
            });

        });

    });

    describe('общие условия для clearErrors', function() {

        it('у каждого блока, для которого был вызван showErrors, должен вызываться clearErrors', function() {
            block.showErrors(flatHash);
            block.clearErrors();

            expect(u._.every(blocks, function(viewBlock) {
                return viewBlock.showErrors.called ?
                    viewBlock.clearErrors.called :
                    !viewBlock.clearErrors.called;
            })).to.be.true;
        });

    });

});
