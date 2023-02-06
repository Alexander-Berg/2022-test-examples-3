describe('message-body', function() {
    beforeEach(function() {
        this.bMessageBody = ns.View.create('message-body', {ids: '1'});

        this.sinon.stub(this.bMessageBody, 'invalidate');
        this.sinon.stub(this.bMessageBody, 'update').returns(Vow.fulfill());

        this.sinon.stub(Jane, 'c');
        this.block = ns.View.create('message-body', {ids: '1'});
    });

    describe('#_onMessageMove', function() {
        it('должен отключать фосированный показ картинок и ссылок', function() {
            this.bMessageBody._onMessageMove();

            expect(this.bMessageBody.forceShowImages).to.be.undefined;
            expect(this.bMessageBody.forceShowHrefs).to.be.undefined;
            // TODO: обновить sinon-chai и заменить на calledOnceWithExactly
            // после мержа https://github.com/domenic/sinon-chai/pull/117
            expect(this.bMessageBody.invalidate).to.have.been.calledOnce.and.calledWithExactly();
        });
    });

    describe('#_onMessageWidgetSpamShowContent', function() {
        it('должен включать фосированный показ картинок', function() {
            this.bMessageBody._onMessageWidgetSpamShowContent();

            expect(this.bMessageBody.forceShowImages).to.be.ok;
        });

        it('должен включать фосированный показ ссылок', function() {
            this.bMessageBody._onMessageWidgetSpamShowContent();

            expect(this.bMessageBody.forceShowHrefs).to.be.ok;
        });

        it('должен инвалидировать блок', function() {
            this.bMessageBody._onMessageWidgetSpamShowContent();

            expect(this.bMessageBody.invalidate.callCount).to.be.equal(1);
        });
    });

    describe('parseLocations', function() {
        beforeEach(function() {
            this.firstAddress = 'Новосибирск, ул. Лесосечная 2, кв. 398';
            this.secondAddress = 'Новосибирск, ул. Ильича 15, кв. 70';

            this.firstAddress$node = $('<span class="js-extracted-address"></span>').html(this.firstAddress).attr('data-address', this.firstAddress);
            this.secondAddress$node = $('<span class="js-extracted-address"></span>').html(this.secondAddress).attr('data-address', this.secondAddress);

            this.$node = $('<div></div>')
                .append(this.firstAddress$node)
                .append(this.secondAddress$node);

            this.parsedAddresses = [];
        });

        it('должен возвращать массив созданных location', function() {
            var locations = this.block.parseLocations($('<div></div>').append(this.firstAddress$node), this.parsedAddresses);
            expect(locations).to.be.an('array');
            expect(locations.length).to.be.equal(1);
        });
    });

    describe('#_onClickExtractedAddress', function() {
        beforeEach(function() {
            this.event = {
                currentTarget: $('<div data-address-query="fake-address-query" data-ids="fake-ids" />')[0]
            };
            this.vMapUp = {
                open: this.sinon.stub()
            };
            this.sinon.stub(ns.View, 'create').returns(this.vMapUp);
        });

        it('Должен создать и показать вид с картой', function() {
            this.bMessageBody._onClickExtractedAddress(this.event);

            expect(ns.View.create)
                .to.have.callCount(1)
                .and
                .to.be.calledWithExactly('map-up', { address: 'fake-address-query', ids: 'fake-ids' });

            expect(this.vMapUp.open).to.have.callCount(1);
        });

        it('Не должен показывать вид с картой, если мы не знаем ids', function() {
            this.event.currentTarget.removeAttribute('data-ids');
            this.bMessageBody._onClickExtractedAddress(this.event);
            expect(this.vMapUp.open).to.have.callCount(0);
        });

        it('Не должен показывать вид с картой, если не задан адрес', function() {
            this.event.currentTarget.removeAttribute('data-address-query'); // removeAttribute('data-ids');
            this.bMessageBody._onClickExtractedAddress(this.event);
            expect(this.vMapUp.open).to.have.callCount(0);
        });
    });
});
