describe('ButtonGroup', () => {
    describe('static', () => {
        it('default', function() {
            return this.browser
                .url('ButtonGroup/hermione/hermione.html')
                .click('body')
                .assertView('plain', ['.Hermione#default']);
        });

        it('vertical', function() {
            return this.browser
                .url('ButtonGroup/hermione/hermione.html')
                .assertView('plain', ['.Hermione#vertical .Hermione-Item']);
        });

        describe('gap', function() {
            describe('default', function() {
                it('s', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#gap .Hermione-Item.s']);
                });

                it('m', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#gap .Hermione-Item.m']);
                });

                it('l', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#gap .Hermione-Item.l']);
                });

                it('xl', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#gap .Hermione-Item.xl']);
                });
            });
            describe('vertical', function() {
                it('s-vertical', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#gap .Hermione-Item.s-vertical']);
                });

                it('m-vertical', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#gap .Hermione-Item.m-vertical']);
                });

                it('l-vertical', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#gap .Hermione-Item.l-vertical']);
                });

                it('xl-vertical', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#gap .Hermione-Item.xl-vertical']);
                });
            });

            describe('pin', function() {
                it('pin-circle', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#pin .Hermione-Item.pin-circle']);
                });

                it('pin-round', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#pin .Hermione-Item.pin-round']);
                });

                it('pin-circle-vertical', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#pin .Hermione-Item.pin-circle-vertical']);
                });

                it('pin-round-vertical', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#pin .Hermione-Item.pin-round-vertical']);
                });

                it('pin-circle-link', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#pin .Hermione-Item.pin-circle-link']);
                });

                it('pin-round-link', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#pin .Hermione-Item.pin-round-link']);
                });

                it('pin-circle-link-vertical', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#pin .Hermione-Item.pin-circle-link-vertical']);
                });

                it('pin-round-link-vertical', function() {
                    return this.browser
                        .url('ButtonGroup/hermione/hermione.html')
                        .assertView('plain', ['.Hermione#pin .Hermione-Item.pin-round-link-vertical']);
                });
            });
        });
    });
});
