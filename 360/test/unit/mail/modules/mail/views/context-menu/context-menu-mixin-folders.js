describe('context-menu-mixin-folders', function() {
    beforeEach(function() {
        this.folders = ns.View.create('context-menu-mixin-folders');
    });

    it('Миксин присутствует', function() {
        expect(this.folders).ok;
    });

    describe('__getContextMenuItems ->', function() {
        beforeEach(function() {
            var foldersHash = {
                1: {
                    fid: '1',
                    default: true
                },
                1000: {
                    fid: '1000'
                },
                333: {
                    fid: '333',
                    parent_id: '1000'
                }
            };

            this.sinon.stub(ns.Model.get('folders'), 'getFolderById')
                .withArgs('1').returns(foldersHash['1'])
                .withArgs('1000').returns(foldersHash['1000'])
                .withArgs('333').returns(foldersHash['333']);
        });

        it('Не дает удалить дефолтные папки', function() {
            var items = this.folders.__getContextMenuItems({
                id: '1',
                $item: $('<div data-params=\'fid=1\'></div>')
            });

            expect(_.any(items, { icon: 'delete' })).to.not.ok;
        });

        it('Не дает удалить дефолтные папки', function() {
            var items = this.folders.__getContextMenuItems({
                id: '1000',
                $item: $('<div data-params=\'fid=1000\'></div>')
            });

            expect(_.any(items, { icon: 'delete' })).to.not.ok;
        });

        it('Не дает удалить дефолтные папки', function() {
            var items = this.folders.__getContextMenuItems({
                id: '333',
                $item: $('<div data-params=\'fid=333\'></div>')
            });

            expect(_.any(items, { icon: 'delete' })).to.not.ok;
        });
    });

    xdescribe('__extractUrl ->', function() {
        beforeEach(function() {
            this.$html = $(
                '<div>' +
                    '<div>' +
                        '<span class="target3"></span>' +
                    '</div>' +
                    '<a href="tt">' +
                        '<span>' +
                            '<span class="target1"></span>' +
                        '</span>' +
                    '</a>' +
                    '<a href="tt2" class="b-folders__folder__link">' +
                        '<span class="target"></span>' +
                    '</a>' +
                '</div>'
            );
        });

        it('Возвращает обрамляющую ссылку', function() {
            var url = this.folders.__extractUrl({
                target: this.$html.find('.target1'),
                currentTarget: this.$html
            });

            expect(url).to.be.equal('tt');
        });

        it('Если нет обрамляющей ссылки, то в качестве ссылки берется элемент ".b-folders__folder__link"', function() {
            var url = this.folders.__extractUrl({
                target: this.$html.find('.target3'),
                currentTarget: this.$html
            });

            expect(url).to.be.equal('tt2');
        });
    });
});
