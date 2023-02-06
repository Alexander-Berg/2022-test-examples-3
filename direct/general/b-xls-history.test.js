describe('b-xls-history', function() {
    var exported = [
            {"logdate":"2014-03-11 14:17:50","id":1785824,"cid":261,"half_md5_hash":"3918803907842827576"},
            {"logdate":"2014-03-11 15:14:42","id":1785826,"cid":261,"half_md5_hash":"7149013816539637040"},
            {"logdate":"2014-03-11 16:34:27","id":1785828,"cid":261,"half_md5_hash":"3833237722448814694"},
            {"logdate":"2014-03-11 16:58:29","id":1785829,"cid":261,"half_md5_hash":"3558188064846263609"},
            {"logdate":"2014-03-11 17:13:54","id":1785830,"cid":261,"half_md5_hash":"7161342669647853158"},
            {"logdate":"2014-03-11 17:25:42","id":1785831,"cid":261,"half_md5_hash":"7306025201515573860"},
            {"logdate":"2014-03-11 19:19:48","id":1785832,"cid":261,"half_md5_hash":"3486402058035278690"},
            {"logdate":"2014-03-11 19:20:33","id":1785833,"cid":261,"half_md5_hash":"7004050015101794609"},
            {"logdate":"2014-03-12 12:47:45","id":1785834,"cid":261,"half_md5_hash":"7076106303553233459"},
            {"logdate":"2014-03-12 12:48:01","id":1785835,"cid":261,"half_md5_hash":"7147835157204513891"},
            {"logdate":"2014-03-12 12:53:36","id":1785836,"cid":261,"half_md5_hash":"3847260666955129138"},
            {"logdate":"2014-03-12 13:02:19","id":1785837,"cid":261,"half_md5_hash":"3775815513795737700"},
            {"logdate":"2014-03-12 14:38:44","id":1785838,"cid":261,"half_md5_hash":"4049923964480665698"},
            {"logdate":"2014-03-12 18:37:25","id":1785841,"cid":261,"half_md5_hash":"7305745912677347384"},
            {"logdate":"2014-03-12 19:30:14","id":1785844,"cid":261,"half_md5_hash":"7149294208295384631"},
            {"logdate":"2014-03-13 13:14:38","id":1785845,"cid":261,"half_md5_hash":"3834643990103417954"},
            {"logdate":"2014-03-14 10:30:05","id":1785848,"cid":261,"half_md5_hash":"7161674701543465571"},
            {"logdate":"2014-03-12 18:51:15","id":1785842,"cid":1578,"half_md5_hash":"3616733966682829878"},
            {"logdate":"2014-03-12 18:52:36","id":1785843,"cid":1578,"half_md5_hash":"7365130531411998565"},
            {"logdate":"2014-03-14 17:33:41","id":1785851,"cid":1580,"half_md5_hash":"3979039338107646563"},
            {"logdate":"2014-03-11 14:19:04","id":1785825,"cid":6729,"half_md5_hash":"3919648135103865700"},
            {"logdate":"2014-03-12 16:24:23","id":1785840,"cid":52045,"half_md5_hash":"7293688879392122417"},
            {"logdate":"2014-03-13 13:15:39","id":1785846,"cid":1619101,"half_md5_hash":"4122260627491141428"},
            {"logdate":"2014-03-13 13:15:50","id":1785847,"cid":1619101,"half_md5_hash":"4121744973734950708"}
        ],
        imported = [
            {"logdate":"2014-03-12 19:33:52","status":"success","id":1662978,"cid":276308761},
            {"logdate":"2014-03-13 12:19:26","status":"success","id":1662980,"cid":276313821},
            {"logdate":"2014-03-13 12:27:45","status":"success","id":1662983,"cid":276313846},
            {"logdate":"2014-03-13 13:18:18","status":"success","id":1662984,"cid":276313886},
            {"logdate":"2014-03-13 13:19:35","status":"success","id":1662985,"cid":276313891},
            {"logdate":"2014-03-17 18:08:48","status":"success","id":1662993,"cid":276354006}
        ],
        block;

    beforeEach(function() {
        sinon.stub(u, 'getUrl').callsFake(function() { return 'yandex.ru'; });
    });

    afterEach(function() {
        u.getUrl.restore();
    });

    describe('Если нет загруженных и выгруженных файлов, то', function() {
        var elem;

        beforeEach(function() {
            block = u.createBlock(
                {
                        block: 'b-xls-history',
                        tab: 'exported_list',
                        login: 'test-user',
                        exported: [],
                        imported: [],
                        allow: { download: true, remove: true }
                },
                { inject: true });

            elem = block.elem('empty-content');
        });

        afterEach(function() {
            block.destruct();
            elem = undefined;
        });

        it('должен быть только __element empty-content', function() {
            expect(block.domElem.children().length).to.be.equal(1);
            expect(elem).not.to.be.empty;
        });

        it('содержимое __element empty-content должно быть текстом', function() {
            expect(elem.children().length).to.be.equal(0);
            expect(elem.text()).not.to.be.empty;
        });

        it('после destruct блока попытка обратиться к __element empty-content оборачивается исключением', function() {
            block.destruct();

            expect(function() { block.elem('empty-content') }).to.throw(TypeError);
        });

    });

    describe('Если есть и загруженные и выгруженные файлы, то', function() {

        beforeEach(function() {
            block = u.createBlock(
                {
                        block: 'b-xls-history',
                        tab: 'exported_list',
                        login: 'test-user',
                        exported: exported,
                        imported: imported,
                        allow: { download: true, remove: true }
                },
                { inject: true });
        });

        afterEach(function() {
            block.destruct();
        });

        it('должны быть __element header и table по 2-му каждый', function() {
            ['header', 'table'].forEach(function(name) {
                expect(block.elem(name).length).to.be.equal(2);
            });
        });

        it('не должно быть __element empty-content', function() {
            expect(block.elem('empty-content').length).to.be.equal(0);
        });

        it('после destruct блока попытка обратиться к __element header и table оборачивается исключением', function() {
            block.destruct();

            ['header', 'table'].forEach(function(name) {
                expect(function() { block.elem(name) }).to.throw(TypeError);
            });
        });
    });

    describe('Если есть только загруженные или только выгруженные файлы, то', function() {
        [
            { key: 'exported', exported: exported, description: 'выгруженных' },
            { key: 'imported', imported: imported, description: 'загруженных' }
        ].forEach(function(params) {
            describe('при наличии только ' + params.description, function() {

                beforeEach(function() {
                    block = u.createBlock(
                        {
                                block: 'b-xls-history',
                                tab: 'exported_list',
                                login: 'test-user',
                                exported: params.exported || [],
                                imported: params.imported || [],
                                allow: { download: true, remove: true }
                        },
                        { inject: true });
                });

                afterEach(function() {
                    block.destruct();
                });

                it('должны быть __element header  и table по 1-му каждый', function() {
                    ['header', 'table'].forEach(function(name) {
                        expect(block.elem(name).length).to.be.equal(1);
                    });
                });

                it('не должно быть __element empty-content', function() {
                    expect(block.elem('empty-content').length).to.be.equal(0);
                });

                it('внутри __element table должен быть блок b-data-table с строкой заголовком и списком с информацией о '+ params.description + ' файлах', function() {
                    var head,
                        rows,
                        table = block
                            .elemInstance('table')
                            .findBlockInside('b-data-table');

                    expect(table).not.to.be.null;

                    head = table.elem('head');
                    expect(head.length).to.be.equal(1);
                    expect(head.get(0).tagName.toLowerCase()).to.be.equal('tr');

                    rows = table.elem('row');
                    expect(rows.length).to.be.equal(params[params.key].length);

                    rows.each(function(i, row) {
                        expect(row.tagName.toLowerCase()).to.be.equal('tr');
                    });
                });

                it('ссылки в последней ячейке должны быть ссылками действий', function() {
                    var actions = block.elem('action-link').toArray(),
                        table = block
                            .elemInstance('table')
                            .findBlockInside('b-data-table');

                    table.elem('row').each(function(i, row) {
                        expect($(row.cells[row.cells.length - 1]).find('.link').toArray().every(function(a) {
                            return actions.indexOf(a) > -1;
                        })).to.be.equal(true);
                    });
                });

                it('после destruct блока попытка обратиться к __element header и table оборачивается исключением', function() {
                    block.destruct();

                    ['header', 'table'].forEach(function(name) {
                        expect(function() { block.elem(name) }).to.throw(TypeError);
                    });
                });
            });
        });
    });
});
