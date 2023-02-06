describe('Daria.mCheckboxTreeState', function() {
    beforeEach(function() {
        /** @type Daria.mFolders */
        this.mSettings = ns.Model.get('settings');
        setModelByMock(this.mSettings);

        /** @type Daria.mFolders */
        this.mFolders = ns.Model.get('folders');
        var modelData = getModelMockByName('folders', 'inbox_with_deeply_nested_subfolders');
        this.mFolders.setData(modelData);

        /** @type Daria.mCheckboxTreeState */
        this.model = ns.Model.get('checkbox-tree-state');
    });

    afterEach(function() {
        this.model.destroy();
        this.mFolders.destroy();
        this.mSettings.destroy();
    });

    describe('#onInit', function() {
        it('не должно быть удаленных папок в списке выделенных папок', function() {
            this.mSettings.setSettings({ subscribed_folders: '["1","-1"]' });
            this.model.onInit();
            expect(this.model.getSelectedFids()).to.be.eql([ '1' ]);
        });

        it('не должно быть общих папок в списке выделенных папок', function() {
            this.mSettings.setSettings({ subscribed_folders: '["1","1000"]' });
            this.model.onInit();
            expect(this.model.getSelectedFids()).to.be.eql([ '1' ]);
        });
        
        it('должен правильно проинициализировать данные', function() {
            this.model.onInit();
            expect(this.model.getSelectedFids()).to.be.eql(['1', '16', '17', '19', '20', '7']);
            expect(this.model.getSelectedTabIds()).to.be.eql([]);
        });
        
        it('должен правильно проинициализировать данные если есть выделенные табы', function() {
            this.mSettings.setSettings({ subscribed_tabs: '["relevant"]' });
            this.model.onInit();
            expect(this.model.getSelectedFids()).to.be.eql(['1', '16', '17', '19', '20', '7']);
            expect(this.model.getSelectedTabIds()).to.be.eql(['relevant']);
        });
    });

    describe('#getSelectedFids', function() {
        it('все папки должны быть выбраны при отсутствии сохраненной настройки', function() {
            var defaultSelectedFids = [ '1', '16', '17', '19', '20', '7' ];
            var selectedFids = this.model.getSelectedFids();
            expect(selectedFids).to.be.eql(defaultSelectedFids);
        });
    });

    describe('#getSelectedTabIds', function() {
        it('при отсутствии настройки не выбираем ничего', function() {
            expect(this.model.getSelectedTabIds()).to.be.eql([]);
        });
    });

    describe('#setSelectedFids', function() {
        it('должен сеттить выбранные папки', function() {
            this.model.setSelectedFids([ '1', '16' ]);
            var selectedFids = this.model.getSelectedFids();
            expect(selectedFids).to.be.eql([ '1', '16' ]);
        });

        it('должен сеттить пустой массив', function() {
            this.model.setSelectedFids([]);
            var selectedFids = this.model.getSelectedFids();
            expect(selectedFids).to.be.eql([]);
        });
    });

    describe('#setSelectedTabIds', function() {
        it('должен сеттить выбранные табы', function() {
            this.model.setSelectedTabIds([ 'news', 'social' ]);
            var selectedTabIds = this.model.getSelectedTabIds();
            expect(selectedTabIds).to.be.eql([ 'news', 'social' ]);
        });

        it('должен сеттить пустой массив', function() {
            this.model.setSelectedTabIds([]);
            var selectedTabIds = this.model.getSelectedTabIds();
            expect(selectedTabIds).to.be.eql([]);
        });
    });

    describe('#isAllSelectionEmpty', function() {
        it('Если выделено хоть что-то (таб(ы) или папк(и) покажем текст про выделение части папок', function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(true);
            this.sinon.stub(this.model, 'isSelectionEmpty').returns(true);
            this.sinon.stub(this.model, 'isSelectionTabsEmpty').returns(true);
            expect(this.model.isAllSelectionEmpty()).to.be.eql(true);
        });

        it('Если выделено все, то покажем текст что выделены все папки', function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(true);
            this.sinon.stub(this.model, 'isSelectionEmpty').returns(true);
            this.sinon.stub(this.model, 'isSelectionTabsEmpty').returns(false);
            expect(this.model.isAllSelectionEmpty()).to.be.eql(false);
        });

        it('если табы выключены, то учитываются только папки', function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(false);
            this.sinon.stub(this.model, 'isSelectionEmpty').returns(false);
            this.sinon.stub(this.model, 'isSelectionTabsEmpty').returns(false);
            expect(this.model.isAllSelectionEmpty()).to.be.eql(false);
            expect(this.model.isSelectionTabsEmpty).to.have.callCount(0);
        });

        it('если табы выключены и не выделены папки, считаем что не выделено ничего', function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(false);
            this.sinon.stub(this.model, 'isSelectionEmpty').returns(true);
            this.sinon.stub(this.model, 'isSelectionTabsEmpty').returns(false);
            expect(this.model.isAllSelectionEmpty()).to.be.eql(true);
            expect(this.model.isSelectionTabsEmpty).to.have.callCount(0);
        });
    });

    describe('#isSelected', function() {
        it('папка должна быть выбрана', function() {
            this.model.setSelectedFids([ '1', '16', '17' ]);
            expect(this.model.isSelected('1')).to.be.eql(true);
        });

        it('папка не должна быть выбрана', function() {
            this.model.setSelectedFids([]);
            expect(this.model.isSelected('1')).to.be.eql(false);
        });
    });

    describe('#isTabSelected', function() {
        it('таб должен быть выбран', function() {
            this.model.setSelectedTabIds([ 'relevant', 'news' ]);
            expect(this.model.isTabSelected('relevant')).to.be.eql(true);
        });

        it('таб не должен быть выбран', function() {
            this.model.setSelectedTabIds([]);
            expect(this.model.isTabSelected('relevant')).to.be.eql(false);
        });
    });

    describe('#areAllFoldersSelected', function() {
        it('должен возвращать true при отсутствии сохраненной настройки', function() {
            expect(this.model.areAllFoldersSelected()).to.be.eql(true);
        });

        it('должен возвращать true, если все папки выделены', function() {
            this.model.setSelectedFids([ '1', '16', '17', '19', '20', '7' ]);
            expect(this.model.areAllFoldersSelected()).to.be.eql(true);
        });

        it('должен возвращать false', function() {
            this.model.setSelectedFids([]);
            expect(this.model.areAllFoldersSelected()).to.be.eql(false);
        });
    });

    describe('#areAllTabsSelected', function() {
        beforeEach(function() {
            this.tabs = [ 'relevant', 'news', 'social' ];
            this.mTabs = ns.Model.get('tabs');
            this.sinon.stub(this.mTabs, 'getFoldersTabsList').returns(this.tabs);
        });

        it('выбраны все табы', function() {
            this.model.setSelectedTabIds(this.tabs);
            expect(this.model.areAllTabsSelected()).to.be.eql(true);
        });

        it('выбраны не все табы', function() {
            this.model.setSelectedTabIds([ 'relevant' ]);
            expect(this.model.areAllTabsSelected()).to.be.eql(false);
        });
    });

    describe('#areAllSelected', function() {
        it('при включенных табам выделенным считаются табы + папки', function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(true);

            this.sinon.stub(this.model, 'areAllFoldersSelected').returns(true);
            this.sinon.stub(this.model, 'areAllTabsSelected').returns(true);
            expect(this.model.areAllSelected()).to.be.eql(true);
        });

        it('при включенных табах если не выделены табы, но выделены папки, не' +
            'считаем что ничего не выделили', function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(true);

            this.sinon.stub(this.model, 'areAllFoldersSelected').returns(false);
            this.sinon.stub(this.model, 'areAllTabsSelected').returns(true);
            expect(this.model.areAllSelected()).to.be.eql(false);
        });

        it('при выключенных табах считаются выделенными папки', function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(false);

            this.sinon.stub(this.model, 'areAllFoldersSelected').returns(true);
            this.sinon.stub(this.model, 'areAllTabsSelected').returns(true);
            expect(this.model.areAllSelected()).to.be.eql(true);
            expect(this.model.areAllTabsSelected).to.have.callCount(0);
        });

        it('при выключенных табах считаются не выделенными табы + папки', function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(false);

            this.sinon.stub(this.model, 'areAllFoldersSelected').returns(false);
            this.sinon.stub(this.model, 'areAllTabsSelected').returns(false);
            expect(this.model.areAllSelected()).to.be.eql(false);
            expect(this.model.areAllTabsSelected).to.have.callCount(0);
        });

    });

    describe('#filterFolder', function() {
        it('должен отфильтровать общую папку', function() {
            expect(this.model.filterFolder({ shared: true })).to.be.eql(false);
        });

        it('не должен отфильтровать папку', function() {
            expect(this.model.filterFolder({ shared: false })).to.be.eql(true);
        });
    });

    describe('#getFolderTree', function() {
        it('должен возвращать дерево папок c корнем inbox', function() {
            var tree = this.model.getFolderTree();
            expect(tree && tree.symbol === 'inbox').to.be.eql(true);
        });
    });

    describe('#getFidsOfFolderAndSubfolders', function() {
        it('должен вернуть fids для папки с названием "первый уровень вложенности"', function() {
            var folder = this.model.getFolderTree().items.filter(function(folder) {
                return folder.fid === '16';
            })[0];
            var fids = this.model.getFidsOfFolderAndSubfolders(folder).sort();
            expect(fids).to.be.eql([ '16', '17', '19' ]);
        });
    });

    describe('#getFidsOfInboxWithSubfolders', function() {
        it('должен вернуть fids для inbox и его подпапок', function() {
            var fids = this.model.getFidsOfInboxWithSubfolders().sort();
            expect(fids).to.be.eql([ '1', '16', '17', '19', '20', '7' ]);
        });
    });

    describe('#calcSelectedFids', function() {
        it('должен выделить все папки', function() {
            var fids = this.model.calcSelectedFids(this.model.getFolderTree(), '1', true).sort();
            expect(fids).to.be.eql([ '1', '16', '17', '19', '20', '7' ]);
        });

        it('должен снять выделение со всех подпапок', function() {
            var fids = this.model.calcSelectedFids(this.model.getFolderTree(), '1', false).sort();
            expect(fids).to.be.eql([]);
        });

        it('должен выделить только одну папку', function() {
            this.model.setSelectedFids([]);
            var fids = this.model.calcSelectedFids(this.model.getFolderTree(), '19', true).sort();
            expect(fids).to.be.eql([ '19' ]);
        });

        it('должен снять выделение только с одной подпапки', function() {
            this.model.setSelectedFids([ '1', '16', '17', '19', '20', '7' ]);
            var fids = this.model.calcSelectedFids(this.model.getFolderTree(), '19', false).sort();
            expect(fids).to.be.eql([ '1', '16', '17', '20', '7' ]);
        });
    });

    describe('#calcSelectedTabIds', function() {
        it('должен выделить все табы', function() {
            this.model.setSelectedTabIds([ 'news', 'social' ]);
            const tabs = this.model.calcSelectedTabIds('relevant', true);
            expect(tabs).to.be.eql([ 'relevant', 'news', 'social' ]);
        });
        it('должен снять выделение со всех табов', function() {
            this.model.setSelectedTabIds([ 'social' ]);
            const tabs = this.model.calcSelectedTabIds('social', false);
            expect(tabs).to.be.eql([]);
        });
        it('должен снять выделение с части табов', function() {
            this.model.setSelectedTabIds([ 'news', 'social' ]);
            const tabs = this.model.calcSelectedTabIds('news', false);
            expect(tabs).to.be.eql([ 'social' ]);
        });
    });

    describe('#getHashOfSelectedFids', function() {
        it('должен вернуть хэш с fids всех папок при отсутствии сохраненной настройки', function() {
            var selectedFids = this.model.getHashOfSelectedFids();
            expect(selectedFids).to.be.eql({ 1: true, 16: true, 17: true, 19: true, 20: true, 7: true });
        });

        it('должен вернуть пустой хэш', function() {
            this.model.setSelectedFids([]);
            expect(this.model.getHashOfSelectedFids()).to.be.eql({});
        });

        it('должен вернуть хэш с fids выбранных папок', function() {
            this.model.setSelectedFids([ '1', '16', '17' ]);
            expect(this.model.getHashOfSelectedFids()).to.be.eql({ 1: true, 16: true, 17: true });
        });
    });

    describe('#getHashOfSelectedTabIds', function() {
        it('должен вернуть хэш с tabs всех табов при отсутствии сохраненной настройки', function() {
            var selectedTabIds = this.model.getHashOfSelectedTabIds();
            expect(selectedTabIds).to.be.eql({});
        });

        it('должен вернуть пустой хэш', function() {
            this.model.setSelectedTabIds([]);
            expect(this.model.getHashOfSelectedTabIds()).to.be.eql({});
        });

        it('должен вернуть хэш с tabs выбранных табов', function() {
            this.model.setSelectedTabIds([ 'news' ]);
            expect(this.model.getHashOfSelectedTabIds()).to.be.eql({ news: true });
        });
    });

    describe('#setParentFids', function() {
        it('должен проставить всем папкам массив fids родительских папок', function() {
            var folder = _.find(this.model.getFolderTree().items, function(folder) {
                return folder.fid === '16';
            });
            var foldersWithParentFids = this.model.setParentFids(folder);

            expect(foldersWithParentFids.parentFids).to.be.eql([]);
            expect(foldersWithParentFids.items[0].parentFids).to.be.eql([ '16' ]);
            expect(foldersWithParentFids.items[0].items[0].parentFids).to.be.eql([ '16', '17' ]);
        });
    });

    describe('#getHashOfFolders', function() {
        it('должен вернуть хэш папок', function() {
            var folder = _.find(this.model.getFolderTree().items, function(folder) {
                return folder.fid === '16';
            });

            expect(this.model.getHashOfFolders(folder)).to.deep.equal({
                "16": {
                    "name": "первый уровень вложенности",
                    "folder_options": {
                        "position": 0
                    },
                    "user": true,
                    "fid": "16",
                    "shared": false,
                    "folded": false,
                    "opened": true,
                    "has-unread": false,
                    "level": 1,
                    "items": [
                        {
                            "name": "второй уровень вложенности",
                            "folder_options": {
                                "position": 0
                            },
                            "user": true,
                            "fid": "17",
                            "shared": false,
                            "level": 2,
                            "folded": false,
                            "opened": true,
                            "has-unread": false,
                            "items": [
                                {
                                    "name": "третий уровень вложенности",
                                    "folder_options": {
                                        "position": 0
                                    },
                                    "user": true,
                                    "fid": "19",
                                    "shared": false,
                                    "level": 3,
                                    "folded": false,
                                    "opened": true,
                                    "has-unread": false,
                                    "items": []
                                }
                            ]
                        }
                    ]
                },
                "17": {
                    "name": "второй уровень вложенности",
                    "folder_options": {
                        "position": 0
                    },
                    "user": true,
                    "fid": "17",
                    "shared": false,
                    "level": 2,
                    "folded": false,
                    "opened": true,
                    "has-unread": false,
                    "items": [
                        {
                            "name": "третий уровень вложенности",
                            "folder_options": {
                                "position": 0
                            },
                            "user": true,
                            "fid": "19",
                            "shared": false,
                            "level": 3,
                            "folded": false,
                            "opened": true,
                            "has-unread": false,
                            "items": []
                        }
                    ]
                },
                "19": {
                    "name": "третий уровень вложенности",
                    "folder_options": {
                        "position": 0
                    },
                    "user": true,
                    "fid": "19",
                    "shared": false,
                    "level": 3,
                    "folded": false,
                    "opened": true,
                    "has-unread": false,
                    "items": []
                }
            });
        });

        it ('должен вернуть правильный хеш, если у папки нет родителя', function() {
            var folder = _.find(this.model.getFolderTree().items, function(folder) {
                return folder.fid === '16';
            });
            delete folder.fid;
            expect(this.model.getHashOfFolders(folder)).to.be.eql({
                "17": {
                    "name": "второй уровень вложенности",
                    "folder_options": {
                        "position": 0
                    },
                    "user": true,
                    "fid": "17",
                    "shared": false,
                    "level": 2,
                    "folded": false,
                    "opened": true,
                    "has-unread": false,
                    "items": [
                        {
                            "name": "третий уровень вложенности",
                            "folder_options": {
                                "position": 0
                            },
                            "user": true,
                            "fid": "19",
                            "shared": false,
                            "level": 3,
                            "folded": false,
                            "opened": true,
                            "has-unread": false,
                            "items": []
                        }
                    ]
                },
                "19": {
                    "name": "третий уровень вложенности",
                    "folder_options": {
                        "position": 0
                    },
                    "user": true,
                    "fid": "19",
                    "shared": false,
                    "level": 3,
                    "folded": false,
                    "opened": true,
                    "has-unread": false,
                    "items": []
                }
            });
        });
    });

    describe('#getHashOfHighlightedFids', function() {
        it('должен вернуть хэш подсвеченных папок', function() {
            this.model.setSelectedFids([ '19' ]);
            expect(this.model.getHashOfHighlightedFids()).to.be.eql({ 1: true, 16: true, 17: true });
        });

        it('должен вернуть пустой хэш', function() {
            this.model.setSelectedFids([]);
            expect(this.model.getHashOfHighlightedFids()).to.be.eql({});
        });
        
        it('не должен падать, если передана папка без родителя, просто не учитывает ее в хеше', function() {
            this.model.setSelectedFids(['1000']);
            expect(this.model.getHashOfHighlightedFids()).to.be.eql({});
        });
    });

    describe('#getHashOfHighlightedTabIds', function() {
        it('должен вернуть хэш подсвеченных табов', function() {
            this.model.setSelectedTabIds([ 'social' ]);
            expect(this.model.getHashOfHighlightedTabIds()).to.be.eql({ social: true });
        });

        it('должен вернуть пустой хэш', function() {
            this.model.setSelectedTabIds([]);
            expect(this.model.getHashOfHighlightedTabIds()).to.be.eql({});
        });
    });

    describe('#serializeSelectedFids', function() {
        it('должен вернуть пустую строку', function() {
            this.model.setSelectedFids([]);
            expect(this.model.serializeSelectedFids()).to.be.eql('[]');
        });

        it('должен вернуть строку с fids выделенных папок', function() {
            this.model.setSelectedFids([ '1', '16' ]);
            expect(this.model.serializeSelectedFids()).to.be.eql('["1","16"]');
        });

        it('должен вернуть строку с fids всех папок при отсутствии сохраненной настройки', function() {
            this.model.setSelectedFids([ '1', '16', '17', '19', '20', '7' ]);
            expect(this.model.serializeSelectedFids()).to.be.eql('["1","16","17","19","20","7"]');
        });
    });

    describe('#serializeSelectedTabsIds', function() {
        it('должен вернуть пустую строку при отсутствии настройки', function() {
            this.model.setSelectedTabIds([]);
            expect(this.model.serializeSelectedTabsIds()).to.be.eql('[]');
        });

        it('должен вернуть строку с fids выделенных папок', function() {
            this.model.setSelectedTabIds([ 'news', 'relevant' ]);
            expect(this.model.serializeSelectedTabsIds()).to.be.eql('["news","relevant"]');
        });
    });

    describe('#resetSelectedFids', function() {
        it('должен выделить все папки после ресета', function() {
            this.model.setSelectedFids([ '1', '16' ]);
            this.model.resetSelectedFids();
            expect(this.model.getSelectedFids()).to.be.eql([ '1', '16', '17', '19', '20', '7' ]);
        });
    });

    describe('#resetSelectedTabs', function() {
        it('должен выделить все табы после ресета', function() {
            this.model.setSelectedTabIds([ 'news', 'social' ]);
            this.model.resetSelectedTabIds();
            expect(this.model.getSelectedTabIds()).to.be.eql([]);
        });
    });

    describe('#getOnlyUserFoldersTree', function() {
         it('Вернуть папки без родителя, с уровнем меньше на единицу', function() {
             expect(this.model.getOnlyUserFoldersTree()).to.be.eql({
                 "items": [
                     {
                         "name": "без подпапок",
                         "folder_options": {
                             "position": 0
                         },
                         "user": true,
                         "fid": "20",
                         "shared": false,
                         "level": 0,
                         "folded": false,
                         "opened": true,
                         "has-unread": false,
                         "items": []
                     },
                     {
                         "name": "первый уровень вложенности",
                         "folder_options": {
                             "position": 0
                         },
                         "user": true,
                         "fid": "16",
                         "shared": false,
                         "level": 0,
                         "folded": false,
                         "opened": true,
                         "has-unread": false,
                         "items": [
                             {
                                 "name": "второй уровень вложенности",
                                 "folder_options": {
                                     "position": 0
                                 },
                                 "user": true,
                                 "fid": "17",
                                 "shared": false,
                                 "level": 2,
                                 "folded": false,
                                 "opened": true,
                                 "has-unread": false,
                                 "items": [
                                     {
                                         "name": "третий уровень вложенности",
                                         "folder_options": {
                                             "position": 0
                                         },
                                         "user": true,
                                         "fid": "19",
                                         "shared": false,
                                         "level": 3,
                                         "folded": false,
                                         "opened": true,
                                         "has-unread": false,
                                         "items": []
                                     }
                                 ]
                             }
                         ]
                     },
                     {
                         "symbol": "archive",
                         "name": "Архив",
                         "folder_options": {
                             "position": 0
                         },
                         "fid": "7",
                         "shared": false,
                         "user": true,
                         "level": 0,
                         "folded": false,
                         "opened": true,
                         "has-unread": false,
                         "items": []
                     }
                 ]
             });
         });
    });

    describe('#_isInboxFolderWasSubscribed', function() {
        it('если папка есть в массиве выбранных фидов, значит на нее была подписка пушей', function() {
            this.model.setSelectedFids(['1', '2', '3']);
            expect(this.model._isInboxFolderWasSubscribed()).to.be.eql(true);
        });

        it('на папку не было подписки пушей', function() {
            this.model.setSelectedFids(['2', '3']);
            expect(this.model._isInboxFolderWasSubscribed()).to.be.eql(false);
        });
    });

    describe('#_isInboxTabWasSubscribed', function() {
        it('если таб в списке выбранных, значит на него была подписка пушей', function() {
            this.model.setSelectedTabIds(['news', 'relevant', 'social']);
            expect(this.model._isInboxTabWasSubscribed()).to.be.eql(true);
        });

        it('на таб не было подписки пушей', function() {
            this.model.setSelectedTabIds(['news', 'social']);
            expect(this.model._isInboxTabWasSubscribed()).to.be.eql(false);
        });
    });

    describe('методы переподписок табов и папок', function() {
        beforeEach(function() {
            this.sinon.stub(this.mSettings, 'setSettings');
        });

        describe('#_subscribeInboxFolder', function() {
            it('должен добавить папку Входящие в список подписок', function() {
                this.model.setSelectedFids([ '12', '13' ]);

                expect(this.model._subscribeInboxFolder()).to.be.eql({
                    subscribed_folders: JSON.stringify([ '1', '12', '13' ])
                });
                expect(this.model.getSelectedFids()).to.be.eql([ '1', '12', '13' ]);
            });
        });

        describe('#_unsubscribeInboxFolder', function() {
            it('должен удалить папку Входящие из списка подписок', function() {
                this.model.setSelectedFids([ '12', '13', '1' ]);

                expect(this.model._unsubscribeInboxFolder()).to.be.eql({
                    subscribed_folders: JSON.stringify([ '12', '13' ])
                });
                expect(this.model.getSelectedFids()).to.be.eql([ '12', '13' ]);
            });
        });


        describe('#_subscribeOnlyInboxTab', function() {
            it('должен добавить только таб Входящие в список подписок', function() {
                this.model.setSelectedTabIds([ 'news' ]);

                expect(this.model._subscribeOnlyInboxTab()).to.be.eql({
                    subscribed_tabs: JSON.stringify([ 'relevant' ])
                });
                expect(this.model.getSelectedTabIds()).to.be.eql([ 'relevant' ]);
            });
        });

        describe('#_unsubscribeAllTabs', function() {
            it('должен удалить все табы из списка подписок', function() {
                this.model.setSelectedTabIds([ 'news', 'relevant', 'social' ]);

                expect(this.model._unsubscribeAllTabs()).to.be.eql({
                    subscribed_tabs: JSON.stringify([])
                });
                expect(this.model.getSelectedTabIds()).to.be.eql([]);
            });
        });
    });

    describe('#resubscribeOnFoldersTabsNotifications', function() {
        beforeEach(function() {
            this.sinon.stub(this.mSettings, 'getSetting');
            this.mSettings.getSetting.withArgs('browser_notify_message').returns('on');
            this.sinon.stub(this.mSettings, 'setSettings');
        });

        it('Если выключены браузерные нотификации, ничего не делаем', function() {
            this.mSettings.getSetting.withArgs('browser_notify_message').returns('');
            this.sinon.stub(this.model, '_resubscribe');

            this.model.resubscribeOnFoldersTabsNotifications('on');

            expect(this.model._resubscribe).to.have.callCount(0);
        });

        it('Если объект изменяемых настроек непуст, проставляем настройки и переподписываемся', async function() {
            const settingsDiff = {
                subscribed_tabs: ['news'],
                subscribed_folders: ['123', '12']
            };

            this.sinon.stub(this.model, 'syncronizeInboxBetween').returns(settingsDiff);
            this.sinon.spy(this.model, '_updateSettings');
            this.sinon.spy(this.model, '_resubscribe');
            this.sinon.stub(Daria.PushManager, 'subscribe');

            await this.model.resubscribeOnFoldersTabsNotifications(settingsDiff);
            
            expect(this.model._updateSettings).to.have.callCount(1);
            expect(Daria.PushManager.subscribe).to.have.callCount(1);
        });
    });

    describe('syncronizeInboxBetween', function() {
        describe('Табы выключены', function() {
            it('Если был таб Входящие в подписке - отписываем его и подписываем папку Входящие', function() {
                this.model.setSelectedTabIds([ 'relevant' ]);
                this.model.setSelectedFids([ '12', '13' ]);

                this.model.syncronizeInboxBetween('');

                expect(this.model.getSelectedFids()).to.be.eql([ '1', '12', '13' ]);
                expect(this.model.getSelectedTabIds()).to.be.eql([]);
            });

            it('Если были хоть какие-то табы (не Входящие), сносим их', function() {
                this.model.setSelectedTabIds([ 'social' ]);
                this.model.setSelectedFids([ '12', '13' ]);

                this.model.syncronizeInboxBetween('');

                expect(this.model.getSelectedFids()).to.be.eql([ '12', '13' ]);
                expect(this.model.getSelectedTabIds()).to.be.eql([]);
            });

            it('Если есть папка Входящие, то оставляем её в подписке и удаляем табы из подписки', function() {
                this.model.setSelectedTabIds([ 'social' ]);
                this.model.setSelectedFids([ '12', '13', '1' ]);

                this.model.syncronizeInboxBetween('');

                expect(this.model.getSelectedFids()).to.be.eql([ '1', '12', '13' ]);
                expect(this.model.getSelectedTabIds()).to.be.eql([]);
                });
        });

        describe('Табы включены', function() {
            it('Если была папка Входящие - отписываем её и оставляем только таб Входящие', function() {
                this.model.setSelectedTabIds([ 'social' ]);
                this.model.setSelectedFids([ '12', '13', '1' ]);

                this.model.syncronizeInboxBetween('on');

                expect(this.model.getSelectedFids()).to.be.eql([ '12', '13' ]);
                expect(this.model.getSelectedTabIds()).to.be.eql(['relevant']);
            });

            it('Если была только папка Входящие - отписываем её и оставляем только таб Входящие',
                function() {
                    this.model.setSelectedFids([ '1' ]);
                    this.model.setSelectedTabIds([ 'news' ]);

                    this.model.syncronizeInboxBetween('on');

                    expect(this.model.getSelectedTabIds()).to.be.eql([ 'relevant' ]);
                    expect(this.model.getSelectedFids()).to.be.eql([]);
                });

            it('Если не было папки Входящие и таба Входящие, то ничего не меняется 1', function() {
                this.model.setSelectedFids([ '12' ]);
                this.model.setSelectedTabIds([]);

                this.model.syncronizeInboxBetween('on');

                expect(this.model.getSelectedFids()).to.be.eql([ '12' ]);
                expect(this.model.getSelectedTabIds()).to.be.eql([]);
            });

            it('Если не было папки Входящие и таба Входящие, то ничего не меняется 2', function() {
                this.model.setSelectedFids([]);
                this.model.setSelectedTabIds([ 'relevant' ]);

                this.model.syncronizeInboxBetween('on');

                expect(this.model.getSelectedFids()).to.be.eql([]);
                expect(this.model.getSelectedTabIds()).to.be.eql(['relevant']);
            });
        });
    });
});
