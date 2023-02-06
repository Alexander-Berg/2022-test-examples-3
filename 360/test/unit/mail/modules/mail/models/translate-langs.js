describe('Daria.mTranslateLangs', function() {
    beforeEach(function() {
        this.model = ns.Model.get('translate-langs');
    });

    afterEach(function() {
        ns.Model.traverse('translate-langs', function(model) {
            model.destroy();
        });
    });

    describe('#getName', function() {
        it('Должен вернуть название языка', function() {
            this.model.setData({ langs: { ru: 'Русский' } });
            expect(this.model.getName('ru')).to.be.equal('Русский');
        });

        it('Должен вернуть пустую строку, если язык не найден', function() {
            expect(this.model.getName('ru')).to.be.equal('');
        });
    });

    describe('#getLangs', function() {
        it('Должен вернуть список объектов доступных языков', function() {
            this.model.setData({ langs: { ru: 'Русский' } });
            expect(this.model.getLangs()).to.deep.equal([
                {
                    name: 'Русский',
                    lang: 'ru'
                }
            ]);
        });
    });

    describe('#getRecent', function() {
        beforeEach(function() {
            this.stubGetSetting = this.sinon.stub(ns.Model.get('settings'), 'getSetting').withArgs('translateRecent');
        });

        it('Должен вернуть список объектов последних используемых языков', function() {
            this.model.setData({ langs: { ru: 'Русский' } });
            this.stubGetSetting.returns('ru');

            expect(this.model.getRecent()).to.deep.equal([
                {
                    name: 'Русский',
                    lang: 'ru'
                }
            ]);
        });

        it('Список последних языков состоит только из определенных языков', function() {
            this.model.setData({ langs: { ru: 'Русский' } });
            this.stubGetSetting.returns('ru,en');

            expect(this.model.getRecent()).to.deep.equal([
                {
                    name: 'Русский',
                    lang: 'ru'
                }
            ]);
        });

        it('Дубликаты языков должны быть удалены', function() {
            this.model.setData({ langs: { ru: 'Русский' } });
            this.stubGetSetting.returns('ru,ru');

            expect(this.model.getRecent()).to.deep.equal([
                {
                    name: 'Русский',
                    lang: 'ru'
                }
            ]);
        });
    });

    describe('#setRecent', function() {
        beforeEach(function() {
            this.stubSetSettings = this.sinon.stub(ns.Model.get('settings'), 'setSettings');
            this.stubGetSetting = this.sinon.stub(ns.Model.get('settings'), 'getSetting').withArgs('translateRecent');
        });

        it('Должен добавить вначало списка переданный язык', function() {
            this.model.setData({ langs: { ru: 'Русский' } });
            this.stubGetSetting.returns('');

            this.model.setRecent('ru');
            expect(this.stubSetSettings).to.be.calledWithMatch({
                translateRecent: 'ru'
            });
        });

        it('Должен сдвинуть новый язык в начало списка', function() {
            this.model.setData({ langs: { ru: 'Русский', en: 'Английский' } });
            this.stubGetSetting.returns('ru,en');

            this.model.setRecent('en');
            expect(this.stubSetSettings).to.be.calledWithMatch({
                translateRecent: 'en,ru'
            });
        });
    });
});

