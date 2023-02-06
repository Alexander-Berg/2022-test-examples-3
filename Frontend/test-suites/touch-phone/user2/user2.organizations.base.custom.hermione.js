describe('user2.organizations.base.custom', function() {
    beforeEach(function() {
        this.browser
            .url('dist/examples/touch-phone/user2/user2.organizations.base.html')
            .execute(function() { // чтобы попап полностью помещался на экран
                document.body.style.paddingLeft = '250px';
                document.body.style.height = '500px';
                return document.body.offsetHeight;
            });
    });

    describe('opened', function() {
        [...Array(6).keys()].forEach(function(i) {
            it(`${i + 1}-user`, function() {
                return this.browser
                    .execute(function() {
                        return 1;
                    })
                    .waitForVisible(`.legouser:nth-of-type(${i + 1})`, 10000)
                    .moveToObject(`.legouser:nth-of-type(${i + 1})`)
                    .click(`.legouser:nth-of-type(${i + 1})`)
                    .pause(100)
                    .assertView(`${i + 1}-opened`, 'body', { ignoreElements: ['.user-account__pic'] });
            });
        });
    });
});
