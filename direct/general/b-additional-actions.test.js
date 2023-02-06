describe('b-additional-actions', function() {
    var block,
        createBlock = function(ctx) {
            block = u.createBlock(u._.extend({
                block: 'b-additional-actions'
            }, ctx));
        };

    [
        {
            type: 'chief',
            text: 'информация о главном представителе',
            ctx: {
                chief: {
                    name: 'Petya',
                    email: 'petya@example.com'
                }
            }
        },
        {
            type: 'manager',
            text: 'информация о менеджере',
            ctx: {
                manager: {
                    name: 'Petya',
                    email: 'petya@example.com'
                }
            }
        }
    ].forEach(function(test) {
        describe(test.text, function() {
            it('Если передана ' + test.text + ', она отображается', function() {
                createBlock(test.ctx);

                expect(block.elem('personal-info', 'type', test.type).length).not.to.equal(0);
            });

            it('Если не передана ' + test.text + ', она не отображается', function() {
                createBlock({});

                expect(block.elem('personal-info', 'type', test.type).length).to.equal(0);
            });
        });
    });

    describe('Подписи', function() {
        it('Если переданы подписи, они отображается', function() {
            createBlock({
                footnotes: [
                    { sup: '1', text: 'some text'}
                ]
            });

            expect(block.elem('footnotes').length).not.to.equal(0);
        });

        it('Если не переданы подписи, они не отображается', function() {
            createBlock({});

            expect(block.elem('footnotes').length).to.equal(0);
        });
    });

    describe('Группы действий', function() {
        it('Если группы не переданы, они не отображаются', function() {
            createBlock({});

            expect(block.elem('action-group-wrapper').length).to.equal(0);
        });

        [
            { groups: 1, columns: 1 },
            { groups: 2, columns: 2 },
            { groups: 3, columns: 2 },
            { groups: 4, columns: 2 },
            { groups: 5, columns: 3 },
            { groups: 6, columns: 3 },
            { groups: 7, columns: 4 },
            { groups: 8, columns: 4 },
            { groups: 9, columns: 5 },
            { groups: 10, columns: 5 }
        ].forEach(function(test) {
            it('Если передана ' + test.group + ' групп(а, ы), то отрисовывается ' + test.columns + ' стобец(а, ов)', function() {
                createBlock({
                    groups: (function() {
                        var res = [];
                        for (var i =0; i < test.groups; i++) {
                            res.push(
                                {
                                    title: 'group No ' + i,
                                    links: []
                                }
                            )
                        }
                        return res;
                    })()
                });

                expect(block.elem('action-group-wrapper').length).to.equal(test.columns);
            });
        })
    });
});
