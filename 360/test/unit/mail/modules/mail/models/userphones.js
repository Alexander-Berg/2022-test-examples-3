/*global mock*/

describe('Daria.mUserphones', function() {
    beforeEach(function() {
        this.mUserphones = ns.Model.get('userphones');
        setModelByMock(this.mUserphones);
    });

    describe('.hasActiveNumber', function() {
        it('при отсутствии данных нет активного номера', function() {
            this.mUserphones.setData({
                phone: []
            });

            expect(this.mUserphones.hasActiveNumber()).to.be.equal(false);
        });

        it('при отсутствии активных номеров нет активного номера', function() {
            this.mUserphones.setData({
                phone: [ { masked_number: '+7 999 999-**-99', id: 'id', active: '0' } ]
            });

            expect(this.mUserphones.hasActiveNumber()).to.be.equal(false);
        });

        it('валидный и активный может быть активным', function() {
            expect(this.mUserphones.hasActiveNumber()).to.be.equal(true);
        });
    });

    it('should normalize user phone', function() {
        expect(this.mUserphones.normalize('80000000000')).to.be.equal('+70000000000');
    });

    describe('.getActivePhone', function() {
        it('Только активный и валидный считается по настоящему активным', function() {
            expect(this.mUserphones.getActivePhone().id).to.be.equal('987654321');
        });
    });

    describe('.getUnconfirmedPhone (deprecated)', function() {
        it('Вернет undefined', function() {
            expect(this.mUserphones.getUnconfirmedPhone()).to.be.equal(undefined);
        });
    });

    describe('.getActiveMaskedNumber', function() {
        it('Возвращает номер активного валидного телефона', function() {
            expect(this.mUserphones.getActiveMaskedNumber()).to.be.equal('+7 000 000-**-01');
        });
    });

    describe('.getUnconfirmedNumber (deprecated)', function() {
        it('Вернет undefined', function() {
            expect(this.mUserphones.getUnconfirmedNumber()).to.be.equal(undefined);
        });
    });
});
