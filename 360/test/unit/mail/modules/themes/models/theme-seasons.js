describe('Daria.mThemeSeasons', function() {

    beforeEach(function() {
        this.model = ns.Model.get('theme-seasons');
    });

    describe('#getSavedSkin', function() {
        it('Должен вернуть массив ["winter", "7"]', function() {
            this.sinon.stub(ns.Model.get('settings'), 'getSetting').returns('winter-7');
            expect(this.model.getSavedSkin()).to.be.eql(['winter', '7']);
        });

        it('Должен вернуть ["winter"]', function() {
            var mSettings = ns.Model.get('settings');

            this.sinon.stub(mSettings, 'getSetting');

            mSettings.getSetting.withArgs('seasons-modifier').returns('winter');
            mSettings.getSetting.withArgs('seasons-skin').returns(undefined);

            expect(this.model.getSavedSkin()).to.be.eql(['winter']);
        });
    });

    // TODO: Базовый метод #getSkin научился работать с сохраненными скинами. Нужно покрыть его тестами,
    // а этот тест удалить
    describe('#getSkin', function() {
        it('Должен вернуть 7', function() {
            this.sinon.stub(ns.Model.get('settings'), 'getSetting').returns('winter-7');
            expect(this.model.getSkin()).to.be.eql(7);
        });
    });

    describe('#getScope', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, 'getSavedSkin').returns([]);
            this.data = {
                skins: {
                    scopes: {
                        winter: {},
                        spring: {},
                        summer: {},
                        autumn: {}
                    }
                }
            }
        });

        it('Должен вернуть spring', function() {
            this.sinon.useFakeTimers((new Date(2015, 2, 1)).getTime());
            expect(this.model.getScope(this.data)).to.be.eql('spring');
        });

        it('Должен вернуть summer', function() {
            this.sinon.useFakeTimers((new Date(2015, 5, 1)).getTime());
            expect(this.model.getScope(this.data)).to.be.eql('summer');
        });

        it('Должен вернуть autumn', function() {
            this.sinon.useFakeTimers((new Date(2015, 8, 1)).getTime());
            expect(this.model.getScope(this.data)).to.be.eql('autumn');
        });

        it('Должен вернуть winter', function() {
            this.sinon.useFakeTimers((new Date(2015, 11, 1)).getTime());
            expect(this.model.getScope(this.data)).to.be.eql('winter');
        });
    });

    describe('#getPageModifiers', function() {
        beforeEach(function() {
            this.model.pageModifiersCache = {};
        });

        it('Должен вернуть пустой объект', function() {
            var data = {
                skins: {
                    scopes: {
                        winter: {
                            'skin-5': {}
                        }
                    }
                }
            };

            expect(this.model.getPageModifiers(data, 'winter', 5)).to.be.deep.equal({
                page_is_dark: false,
                aside_is_dark: false,
                content_is_dark: false
            });
        });

        it('Должен найти page_is_dark', function() {
            var data = {
                skins: {
                    scopes: {
                        winter: {
                            'skin-5': {
                                page_is_dark: true
                            }
                        }
                    }
                }
            };

            expect(this.model.getPageModifiers(data, 'winter', 5)).to.be.deep.equal({
                page_is_dark: true,
                aside_is_dark: false,
                content_is_dark: false
            });
        });
    });
});
