describe('map-city-complaint', function() {
    describe('MapCityComplaint', function() {
        beforeEach(function() {
            this.address = 'whatever';
            this.viewParams = {address: this.address};
            this.vMapCityComplaint = ns.View.create('map-city-complaint', this.viewParams);

            this.mAccountInformation = ns.Model.get('account-information');
        });

        describe('#_getSlide', function() {
            beforeEach(function() {
                this.sinon.stub(this.vMapCityComplaint, 'slides').value([1, 2, 3]);
            });

            it('должен возвращать запрошенный элемент', function() {
                expect(this.vMapCityComplaint._getSlide(1)).eql(1);
                expect(this.vMapCityComplaint._getSlide(2)).eql(2);
                expect(this.vMapCityComplaint._getSlide(3)).eql(3);
            });

            it('должен возвращать пустую выборку jQuery', function() {
                var res = this.vMapCityComplaint._getSlide(0);
                expect(res).an('object');
                expect(res).length(0);

                res = this.vMapCityComplaint._getSlide(4);
                expect(res).an('object');
                expect(res).length(0);
            });
        });

        describe('#_init', function() {
            beforeEach(function() {
                this._aiSid = this.sinon.stub(Daria, 'hasSid');
                this._aiSid.returns(true);
                this._$node = $('<div/>');
                this.sinon.stub(this.vMapCityComplaint, '_getSlide').returns(this._$node);
            });

            it('должен переключать позиционирование 2 слайда', function() {
                this.vMapCityComplaint._initMapCityComplaint();

                expect(this._aiSid.calledWith(669));
                expect(this._$node.prop('class')).to.be.ok;
            });

            it('должен выйти если не тот сид', function() {
                this._aiSid.returns(false);

                this.vMapCityComplaint._initMapCityComplaint();

                expect(this._$node.prop('class')).not.to.be.ok;
            });
        });

        describe('#_serializeMessage', function() {
            it('должен вернуть форматированный текст', function() {
                var data = {
                    'a': 1,
                    'b': [
                        1, '2', {}
                    ]
                };
                var text = 'a:\n1\n\nb:\n[1,"2",{}]';
                expect(this.vMapCityComplaint._serializeMessage(data)).equal(text);
            });
        });
    });
});
