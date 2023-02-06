describe('Daria.vSetupLeft', function() {
    beforeEach(function() {
        this.view = ns.View.create('setup-left');
        this.mAccountInformation = ns.Model.get('account-information');
    });

    describe('#getCurrentUTCString', function() {
        it('Если offset отрицательный, вернем время со знаком +', function() {
            this.mAccountInformation.setData({
                'tz_offset': -180
            });
            expect(this.view.getCurrentUTCString()).to.eql('UTC+03:00');
        });
        it('Если offset положительный, вернем время со знаком -', function() {
            this.mAccountInformation.setData({
                'tz_offset': 120
            });
            expect(this.view.getCurrentUTCString()).to.eql('UTC-02:00');
        });
        it('Если часы больше 10, то вернем их без лидирующего нуля', function() {
            this.mAccountInformation.setData({
                'tz_offset': 800
            });
            expect(this.view.getCurrentUTCString()).to.eql('UTC-13:20');
        });

        it('Если часы меньше 10, то вернем их с лидирующим нулем', function() {
            this.mAccountInformation.setData({
                'tz_offset': 160
            });
            expect(this.view.getCurrentUTCString()).to.eql('UTC-02:40');
        });

        it('Если минуты меньше 10, то вернем их с лидирующим нулем', function() {
            this.mAccountInformation.setData({
                'tz_offset': 300
            });
            expect(this.view.getCurrentUTCString()).to.eql('UTC-05:00');
        });

        it('Если минуты больше 10, то вернем их без лидирующего нуля', function() {
            this.mAccountInformation.setData({
                'tz_offset': 340
            });
            expect(this.view.getCurrentUTCString()).to.eql('UTC-05:40');
        });
    });
});
