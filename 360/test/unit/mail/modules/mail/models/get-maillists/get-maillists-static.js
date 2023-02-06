describe('Daria.mGetMaillistsStatic', function() {
    beforeEach(function() {
        this.model = ns.Model.get('get-maillists-static', {});
    });

    describe('#_onInit', function() {
        it('должен устанавливать пустые данные', function() {
            this.model._onInit();
            expect(this.model.get('.emails')).to.be.not.ok;
        });

    });

    describe('#getMaillists', function() {
        it('кеш заполнен, не надо делать запрос', function() {
            var emails = ['mail-verstka@yandex-team.ru', 'ekhurtina@yandex-team.ru', 'mail-dev@yandex-team.ru'];
            var cash = {
                'mail-verstka@yandex-team.ru': true,
                'ekhurtina@yandex-team.ru': false,
                'mail-dev@yandex-team.ru': true
            };

            this.model.setData({emails: cash});

            return this.model.getMaillists(emails).then(function() {
                expect(ns.request.models).to.have.callCount(0);
            });

        });

        it('в кеше пусто, должен сделать запрос', function() {
            var emails = ['mail-verstka@yandex-team.ru', 'ekhurtina@yandex-team.ru', 'mail-dev@yandex-team.ru'];

            var mGetMaillists = ns.Model.get('get-maillists', {emails: emails.join(',')});
            mGetMaillists.setData({'maillists': []});

            this.model.setData({'emails': {}});

            ns.request.models.restore();
            this.sinon.stub(ns.request, 'models').returns(vow.resolve([mGetMaillists]));

            return this.model.getMaillists(emails).then(function() {
                expect(ns.request.models).to.have.callCount(1);
            });

        });
        it('кеш заполнен, вернул правильные емейлы', function() {
            var emails = ['mail-verstka@yandex-team.ru', 'ekhurtina@yandex-team.ru', 'test@yandex-team.ru', 'daria-dev@yandex-team.ru'];
            var cash = {
                'mail-verstka@yandex-team.ru': true,
                'ekhurtina@yandex-team.ru': false,
                'test@yandex-team.ru': false,
                'daria-dev@yandex-team.ru': true
            };

            this.model.setData({emails: cash});

            return this.model.getMaillists(emails).then(function(data) {
                expect(data).to.eql(['mail-verstka@yandex-team.ru', 'daria-dev@yandex-team.ru']);
            });
        });

        it('кеш пуст, делаем запрос и получаем правильные емейлы из нового кеша', function() {
            var emails = ['mail-verstka@yandex-team.ru', 'ekhurtina@yandex-team.ru', 'mail-verstka@yandex-team.ru'];

            var mGetMaillists = ns.Model.get('get-maillists', {emails: emails.join(',')});
            mGetMaillists.setData({'maillists': ['mail-verstka@yandex-team.ru', 'mail-verstka@yandex-team.ru']});

            this.model.setData({'emails': {}});

            return this.model.getMaillists(emails).then(function(data) {
                expect(data).to.eql(['mail-verstka@yandex-team.ru', 'mail-verstka@yandex-team.ru']);
            });
        });
    });
});
