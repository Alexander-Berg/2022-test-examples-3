describe('Daria.mFolders', function() {
    beforeEach(function() {
        /** @type Daria.mFolders */
        this.mFolders = ns.Model.get('folders');
        setModelByMock(this.mFolders);
    });

    describe('#Drag&Drop', function() {
        it('isDraggable должен возвращать false для шареных папок', function() {
            expect(this.mFolders.isDraggable('10')).to.not.be.ok;
        });

        it('isDroppable должен возвращать false для шареных папок', function() {
            expect(this.mFolders.isDroppable('10')).to.not.be.ok;
        });

        it('isDraggable для пользовательских папок должен возвращать true', function() {
            expect(this.mFolders.isDraggable('11')).to.be.ok;
        });
    });

    describe('#getFolderById', function() {
        it('должен вернуть данные для папки по fid', function() {
            var data = this.mFolders.select('.folder[1]')[0];
            expect(this.mFolders.getFolderById('2')).to.be.equal(data);
        });

        it('должен вернуть null, если папки нет', function() {
            expect(this.mFolders.getFolderById('21')).to.be.equal(null);
        });

        it('должен использовать кеш и не искать по jpath', function() {
            this.sinon.spy(no.jpath, 'expr');
            this.mFolders.getFolderById('2');

            expect(no.jpath.expr).to.have.callCount(0);
        });
    });

    describe('#getFolderByName', function() {
        beforeEach(function() {
            this.mFolders.setData({
                new: 3,
                folder: [
                    {
                        count: 10,
                        fid: '1',
                        new: 2,
                        subfolder: [],
                        symbol: 'inbox'
                    },
                    {
                        fid: '9',
                        name: 'aa',
                        parent_id: '1',
                        subfolder: [
                            '11'
                        ],
                        user: true
                    },
                    {
                        fid: '11',
                        name: 'bb',
                        parent_id: '9',
                        subfolder: [],
                        user: true
                    },
                    {
                        count: 10,
                        fid: '2',
                        name: 'Спам',
                        new: 2,
                        subfolder: [],
                        symbol: 'spam'
                    }
                ]
            });
        });

        describe('Передали parent_id ->', function() {
            it('должен вернуть данные папки, если она есть у такого родителя', function() {
                var expectFolder = ns.Model.get('folder', { fid: '11' }).getData();
                expect(this.mFolders.getFolderByName('bb', '9')).to.be.equal(expectFolder);
            });

            it('должен вернуть undefined, если нет такой папки у родителя', function() {
                expect(this.mFolders.getFolderByName('bb', '1')).to.be.equal(undefined);
            });
        });

        describe('не передали parent_id ->', function() {
            it('должен вернуть данные папки, если дочерняя к инбоксу', function() {
                var expectFolder = ns.Model.get('folder', { fid: '9' }).getData();
                expect(this.mFolders.getFolderByName('aa')).to.be.equal(expectFolder);
            });

            it('должен вернуть данные папки верхнего уровня', function() {
                var expectFolder = ns.Model.get('folder', { fid: '2' }).getData();
                expect(this.mFolders.getFolderByName('Спам')).to.be.equal(expectFolder);
            });

            it('должен вернуть undefined, если нет такой папки у родителя', function() {
                expect(this.mFolders.getFolderByName('bb')).to.be.equal(undefined);
            });
        });
    });

    describe('#adjustUnreadCounters.', function() {
        beforeEach(function() {
            this.sinon.spy(ns.Model.prototype, 'set');
            this.sinon.spy(this.mFolders, 'adjustAllUnreads');
        });

        it('должен обновить непрочитанные для указанных папок', function() {
            this.mFolders.adjustUnreadCounters({
                1: 1,
                3: 2
            });

            expect(this.mFolders.select('.folder[ .fid == "1"].new')[0]).to.be.equal(3);
            expect(this.mFolders.select('.folder[ .fid == "3"].new')[0]).to.be.equal(3);
            expect(this.mFolders.adjustAllUnreads).to.have.callCount(1);
        });

        it('не должен вызывать adjustAllUnreads если обновляем спам', function() {
            this.mFolders.adjustUnreadCounters({
                2: 3
            });
            expect(this.mFolders.select('.folder[ .fid == "2"].new')[0]).to.be.equal(3);
            expect(this.mFolders.adjustAllUnreads).to.have.callCount(0);
        });

        it('не должен вызывать adjustAllUnreads если обновляем удаленные', function() {
            this.mFolders.adjustUnreadCounters({
                7: 10
            });
            expect(this.mFolders.select('.folder[ .fid == "7"].new')[0]).to.be.equal(10);
            expect(this.mFolders.adjustAllUnreads).to.have.callCount(0);
        });

        it('не должен вызывать adjustAllUnreads если обновляем скрытые', function() {
            this.mFolders.adjustUnreadCounters({
                12: 1
            });
            expect(this.mFolders.select('.folder[ .fid == "12"].new')[0]).to.be.equal(3);
            expect(this.mFolders.adjustAllUnreads).to.have.callCount(1);
        });

        it('должен обновить общее количество непрочитанных', function() {
            this.mFolders.adjustUnreadCounters({
                1: 1,
                3: 2
            });

            expect(this.mFolders.select('.new')[0]).to.be.equal(6);
            expect(this.mFolders.adjustAllUnreads).to.have.callCount(1);
        });

        it('должен обновить общее количество непрочитанных и не учитывать спам и удаленные', function() {
            this.mFolders.adjustUnreadCounters({
                1: 1,
                3: 2,
                2: 4,
                7: 8,
                12: 16
            });

            expect(this.mFolders.select('.new')[0]).to.be.equal(22);
            expect(this.mFolders.adjustAllUnreads).to.have.callCount(1);
        });

        it('не должен обновлять не отрицательные значения непрочитанные у папки', function() {
            this.mFolders.adjustUnreadCounters({
                1: -100
            });

            expect(this.mFolders.select('.folder[ .fid == "1"].new')[0]).to.be.equal(0);
        });

        it('не должен обновлять не отрицательные значения общее количество непрочитанных', function() {
            this.mFolders.adjustUnreadCounters({
                1: -100
            });

            expect(this.mFolders.select('.new')[0]).to.be.equal(0);
        });
    });

    describe('#logDragndropAbility()', function() {
        beforeEach(function() {
            this.sinon.stub(Jane, 'c');
            this.sinon.stub(this.mFolders, 'isDraggable').callsFake(Jane.Common._true);
        });

        it('нет лога, если папок меньше 2', function() {
            this.mFolders.data = {
                folder: [
                    { fid: 1 }
                ]
            };
            this.mFolders.logDragndropAbility();

            expect(Jane.c).to.have.callCount(0);
        });

        it('есть лог, если 2 папки', function() {
            this.mFolders.data = {
                folder: [
                    { fid: 1 },
                    { fid: 2 }
                ]
            };
            this.mFolders.logDragndropAbility();

            expect(Jane.c).to.have.callCount(1);
        });

        it('есть лог, если 3 папки', function() {
            this.mFolders.data = {
                folder: [
                    { fid: 1 },
                    { fid: 2 },
                    { fid: 3 }
                ]
            };
            this.mFolders.logDragndropAbility();

            expect(Jane.c).to.have.callCount(1);
        });

        it('isDraggable не вызывается больше 2 раз (тест на оптимизированность)', function() {
            this.mFolders.data = {
                folder: [
                    { fid: 1 },
                    { fid: 2 },
                    { fid: 3 },
                    { fid: 4 }
                ]
            };
            this.mFolders.logDragndropAbility();

            expect(this.mFolders.isDraggable).to.have.callCount(2);
        });
    });

    describe('#isFolder', function() {
        it('Возвращает ложь, если fid не определен, ' +
            'и у пользователя нет хотя бы одной из переданных папок', function() {
            var folders = [ 'sent', 'draft', 'outbox', 'template' ];

            var result = this.mFolders.isFolder(undefined, folders);

            expect(result).to.not.be.ok;
        });
    });

    describe('#isSystemFolder', function() {
        beforeEach(function() {
            this.mFolders.setData({
                new: 3,
                folder: [
                    {
                        count: 10,
                        fid: '1',
                        new: 2,
                        subfolder: [],
                        symbol: 'inbox',
                        user: false
                    },
                    {
                        count: 10,
                        fid: '4',
                        subfolder: [],
                        symbol: 'sent',
                        user: false
                    },
                    {
                        fid: '9',
                        name: 'aa',
                        parent_id: '1',
                        subfolder: [
                            '11'
                        ],
                        user: true
                    }
                ]
            });
        });
        it('Возвращает false, если папки с переданным fid-ом нет', function() {
            expect(this.mFolders.isSystemFolder('423')).to.be.eql(false);
        });
        it('Возвращает false, если папка не системная', function() {
            expect(this.mFolders.isSystemFolder('9')).to.be.eql(false);
        });
        it('Возвращает true, если папка системная', function() {
            expect(this.mFolders.isSystemFolder('1')).to.be.eql(true);
            expect(this.mFolders.isSystemFolder('4')).to.be.eql(true);
        });
    });

    describe('#isSystemFolderChild', function() {
        beforeEach(function() {
            this.countOfSystemFolders = 10;
            this.testFid = 'testFid';
            this.testSystemFidFirst = 'testSystemFidFirst';
            this.testSystemFidSecond = 'testSystemFidSecond';

            this.sinon.stub(this.mFolders, 'getFolderById').withArgs(this.testFid).returns({
                user: true
            });

            this.sinon.stub(this.mFolders, 'isSystemFolder').returns(false);

            this.sinon.stub(this.mFolders, 'hasChildByFid')
                .returns(false)
                .withArgs(this.testSystemFidSecond, this.testFid).returns(true);

            this.sinon.stub(this.mFolders, 'getFidBySymbol')
                .returns(this.testSystemFidFirst)
                .withArgs('template').returns(this.testSystemFidSecond);
        });

        describe('Должен вернуть false', function() {
            it('Если нет папки с переданным фидом', function() {
                this.mFolders.getFolderById.withArgs(this.testFid).returns(null);
                expect(this.mFolders.isSystemFolderChild(this.testFid)).to.equal(false);
            });

            it('Если папка с переданным фидом не юзерская', function() {
                this.mFolders.getFolderById.withArgs(this.testFid).returns({
                    user: false
                });

                const result = this.mFolders.isSystemFolderChild(this.testFid);

                expect(this.mFolders.getFolderById).to.have.callCount(1);
                expect(this.mFolders.isSystemFolder).to.have.callCount(0);
                expect(result).to.equal(false);
            });

            it('Если папка с переданным фидом системная', function() {
                this.mFolders.isSystemFolder.returns(true);

                const result = this.mFolders.isSystemFolderChild(this.testFid);

                expect(this.mFolders.getFolderById).to.have.callCount(1);
                expect(this.mFolders.isSystemFolder).to.have.callCount(1);
                expect(result).to.equal(false);
            });

            it('Если после фильтрации не осталось системных папок, по которым нужно проверять детей', function() {
                const result = this.mFolders.isSystemFolderChild(this.testFid, () => false);

                expect(this.mFolders.hasChildByFid).to.have.callCount(0);
                expect(result).to.equal(false);
            });

            it('Если папка с переданным фидом не является дочерней ни для одной системной папки', function() {
                this.mFolders.hasChildByFid.withArgs(this.testSystemFidSecond, this.testFid).returns(false);
                expect(this.mFolders.isSystemFolderChild(this.testFid)).to.equal(false);
            });
        });

        it('Должен вернуть true, если папка является дочерней для одной из системных папок', function() {
            expect(this.mFolders.isSystemFolderChild(this.testFid)).to.equal(true);
        });

        it('Должен применить переданную ф-цию фильтрации', function() {
            const filterFunc = this.sinon.stub().callsFake((symbol) => symbol === 'inbox');

            this.mFolders.getFidBySymbol.withArgs('inbox').returns('inboxFid');

            this.mFolders.isSystemFolderChild(this.testFid, filterFunc);

            expect(filterFunc).have.callCount(this.countOfSystemFolders);
            expect(this.mFolders.hasChildByFid).have.callCount(1);
            expect(this.mFolders.hasChildByFid).to.have.been.calledWith('inboxFid', this.testFid);
        });

        it('Должен не фильтровать системные папки, если ф-ция фильтрации не передана', function() {
            this.mFolders.hasChildByFid.withArgs(this.testSystemFidSecond, this.testFid).returns(false);
            this.mFolders.isSystemFolderChild(this.testFid);

            expect(this.mFolders.hasChildByFid).have.callCount(this.countOfSystemFolders);
        });
    });

    describe('#createTemplateFolder', function() {
        describe('Папка «Шаблоны» существует →', function() {
            it('Должен резолвить промис с инстансом папки', function() {
                return this.mFolders.createTemplateFolder().then(function(folder) {
                    expect(folder.fid).to.be.equal('6');
                });
            });
        });

        describe('Папка «Шаблоны» не существует →', function() {
            beforeEach(function() {
                this.sinon.stub(this.mFolders, 'getFolderBySymbol')
                    .withArgs('template')
                    .returns(undefined);
            });

            describe('Базовый кейс →', function() {
                beforeEach(function() {
                    var modelData = getModelMockByName('folders', 'no_template_symbol');
                    this.mFolders.setData(modelData);
                    this.sinon.stub(this.mFolders, 'getSubfoldersFid')
                        .withArgs('4')
                        .onFirstCall().returns([])
                        .onFirstCall().returns([ '041' ]);
                    this.sinon.stub(ns, 'forcedRequest')
                        .returns(Vow.fulfill([
                            { getFid: this.sinon.stub().returns('041') }
                        ]));
                    this.sinon.stub(this.mFolders, 'setSymbol')
                        .withArgs('041', 'template')
                        .returns(Vow.fulfill({
                            fid: '041',
                            parent_id: '4',
                            name: 'template'
                        }));
                });

                it('Должен создавать папку «template» и задавать ей символ «template»', function() {
                    return this.mFolders.createTemplateFolder().then(function() {
                        expect(ns.forcedRequest).to.be.calledWith([
                            { id: 'do-folders-add', params: { folder_name: 'template', parent_id: '4' } }
                        ]);

                        expect(this.mFolders.setSymbol).to.be.calledWithExactly('041', 'template');
                    }.bind(this));
                });

                it('Должен резолвить промис с инстансом папки', function() {
                    return this.mFolders.createTemplateFolder().then(function(folder) {
                        expect(folder.fid).to.be.equal('041');
                    });
                });
            });

            describe('В папке «Черновики» есть подпапка с именем «template» →', function() {
                beforeEach(function() {
                    var modelData = getModelMockByName('folders', 'no_template_symbol_and_template_name_inside_drafts');
                    this.mFolders.setData(modelData);
                    this.sinon.stub(this.mFolders, 'setSymbol')
                        .withArgs('41', 'template')
                        .returns(Vow.fulfill(this.mFolders.data.folder[1]));
                });

                it('Должен проставлять этой папке символ «template»', function() {
                    this.mFolders.createTemplateFolder();

                    expect(this.mFolders.setSymbol).to.be.calledWithExactly('41', 'template');
                });

                it('Должен резолвить промис с инстансом папки', function() {
                    return this.mFolders.createTemplateFolder().then(function(folder) {
                        expect(folder.fid).to.be.equal('41');
                    });
                });
            });

            describe('В папке «Черновики» есть подпапка с локализованным именем «Шаблоны» →', function() {
                beforeEach(function() {
                    var modelData = getModelMockByName(
                        'folders',
                        'no_template_symbol_and_localized_template_name_inside_drafts'
                    );
                    this.mFolders.setData(modelData);
                    this.sinon.stub(ns, 'forcedRequest')
                        .returns(Vow.fulfill());
                    this.sinon.stub(this.mFolders, 'setSymbol')
                        .withArgs('411', 'template')
                        .returns(Vow.fulfill(this.mFolders.data.folder[1]));
                });

                it('Должен переименовывать папку в «template» и задавать ей символ «template»', function() {
                    return this.mFolders.createTemplateFolder().then(function() {
                        expect(ns.forcedRequest).to.be.calledWithExactly([
                            {
                                id: 'do-folder-rename', params: {
                                    fname: 'template',
                                    fid: '411',
                                    parent_id: '4'
                                }
                            },
                            { id: 'folders' }
                        ]);
                        expect(this.mFolders.setSymbol).to.be.calledWithExactly('411', 'template');
                    }.bind(this));
                });

                it('Должен резолвить промис с инстансом папки', function() {
                    return this.mFolders.createTemplateFolder().then(function(folder) {
                        expect(folder.fid).to.be.equal('411');
                    });
                });
            });
        });
    });

    describe('#hasChildByFid', function() {
        beforeEach(function() {
            var modelData = getModelMockByName('folders', 'father');
            this.mFolders.setData(modelData);
            this.folder = this.mFolders.getFolderById('333');
        });

        it('должна возвращать true, если переданный fid является ребенком первого уровня', function() {
            expect(this.mFolders.hasChildByFid('333', '444')).to.be.equal(true);
        });

        it('должна возвращать true, если переданный fid является ребенком второго уровня', function() {
            expect(this.mFolders.hasChildByFid('333', '777')).to.be.equal(true);
        });

        it('должна возвращать true, если переданный fid является ребенком третьего уровня', function() {
            expect(this.mFolders.hasChildByFid('333', '888')).to.be.equal(true);
        });

        it('должна возвращать false, если переданный fid не является ребенком ни на одном уровне', function() {
            expect(this.mFolders.hasChildByFid('333', '999')).to.be.equal(false);
        });
    });

    describe('#getOpenedFolders', function() {
        beforeEach(function() {
            this.mSettings = ns.Model.get('settings');
        });

        describe('получение открытых папок из настройки ->', function() {
            it('должен вернуть пустой массив, если папок в настройки нет', function() {
                this.mSettings.setData({
                    folders_open: ''
                });

                expect(this.mFolders.getOpenedFolders()).to.have.length(0);
            });

            it('должен вернуть массив с id папок, если они есть в настройки', function() {
                this.mSettings.setData({
                    folders_open: '1,5,9,20'
                });

                expect(this.mFolders.getOpenedFolders()).to.have.members([ '1', '5', '9', '20' ]);
            });
        });

        describe('получение открытых папок из данных модели ->', function() {
            it('должен вернуть массив с id "Входящие", "Черновики" и всех вложенных папок,' +
                ' если нет настройки', function() {
                var mockData = getModelMockByName('folders', 'forGetOpenedFolders');
                this.mFolders.setData(mockData);

                expect(this.mFolders.getOpenedFolders()).to.have.members([ '1', '2', '3', '4', '6' ]);
            });
        });
    });
});
