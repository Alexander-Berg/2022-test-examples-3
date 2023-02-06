describe('Daria.signs', function() {
    /* global mock */

    beforeEach(function() {
        Daria.Config.layout = Daria.Config.layout || '2pane';
        Daria.Config.locale = Daria.Config.locale || 'ru';
        this.sinon.stub(Daria.Config, 'layout').value('2pane');
        this.sinon.stub(Daria.Config, 'locale').value('ru');

        var mAccountInformation = ns.Model.get('account-information');
        setModelByMock(mAccountInformation);

        setModelByMock(ns.Model.get('signs'));

        this.mSettings = ns.Model.get('settings');
        setModelByMock(this.mSettings);

        this.signature = Daria.signs;
    });

    describe('getHtml', function() {
        it('plain подпись преобразовывает к html в плоском виде', function() {
            var signs = this.signature.getHtml();
            var sign = signs.filter(function(sign) {
                return sign._idx === 0;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '<div>--&nbsp;</div><div>plain</div><div>sign</div>');
            expect(sign).to.have.property('preview', '-- \nplain\nsign');
        });

        it('html подпись преобразовывает к html в плоском виде', function() {
            var signs = this.signature.getHtml();
            var sign = signs.filter(function(sign) {
                return sign._idx === 1;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '<div>--&nbsp;</div><div>html</div><div>sign</div>');
            expect(sign).to.have.property('preview', '-- \nhtml\nsign');
        });

        it('смешанную подпись (html и text) преобразовывает к html в плоском виде', function() {
            var signs = this.signature.getHtml();
            var sign = signs.filter(function(sign) {
                return sign._idx === 2;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '<div>--&nbsp;<br></div><div>html and plain</div><div>sign</div>');
            expect(sign).to.have.property('preview', '-- \nhtml and plain\nsign');
        });

        it('для многострочной подписи добавляет свойство spreview с первыми 3я строками', function() {
            var signs = this.signature.getHtml();
            var sign = signs.filter(function(sign) {
                return sign._idx === 3;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '<div>--&nbsp;</div><div>string 1</div><div>string 2</div><div>string 3</div><div>string 4</div><div>sign</div>');
            expect(sign).to.have.property('preview', '-- \nstring 1\nstring 2\nstring 3\nstring 4\nsign');
            expect(sign).to.have.property('spreview', '-- \nstring 1\nstring 2');
        });

        it('заменяет пробел после -- на &nbsp;', function() {
            var signs = this.signature.getHtml();
            var sign = signs.filter(function(sign) {
                return sign._idx === 5;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '<div>--&nbsp;</div><div>test</div>');
        });

        it('добавляет &nbsp; после --, если это единственный текст в узле', function() {
            var signs = this.signature.getHtml();
            var sign = signs.filter(function(sign) {
                return sign._idx === 6;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '<div>--&nbsp;</div><div>test</div>');
        });
    });

    describe('getPlain', function() {
        it('plain подпись преобразовывает к plain', function() {
            var signs = this.signature.getPlain();
            var sign = signs.filter(function(sign) {
                return sign._idx === 0;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '-- \nplain\nsign');
            expect(sign).to.have.property('preview', '-- \nplain\nsign');
        });

        it('html подпись преобразовывает к plain', function() {
            var signs = this.signature.getPlain();
            var sign = signs.filter(function(sign) {
                return sign._idx === 1;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '-- \nhtml\nsign');
            expect(sign).to.have.property('preview', '-- \nhtml\nsign');
        });

        it('смешанную подпись (html и text) преобразовывает к plain', function() {
            var signs = this.signature.getPlain();
            var sign = signs.filter(function(sign) {
                return sign._idx === 2;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '-- \nhtml and plain\nsign');
            expect(sign).to.have.property('preview', '-- \nhtml and plain\nsign');
        });

        it('для многострочной подписи добавляет свойство spreview с первыми 3я строками', function() {
            var signs = this.signature.getPlain();
            var sign = signs.filter(function(sign) {
                return sign._idx === 3;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '-- \nstring 1\nstring 2\nstring 3\nstring 4\nsign');
            expect(sign).to.have.property('preview', '-- \nstring 1\nstring 2\nstring 3\nstring 4\nsign');
            expect(sign).to.have.property('spreview', '-- \nstring 1\nstring 2');
        });

        it('html unescape', function() {
            var signs = this.signature.getPlain();
            var sign = signs.filter(function(sign) {
                return sign._idx === 4;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '-- \n<&>\nsign');
            expect(sign).to.have.property('preview', '-- \n<&>\nsign');
        });

        it('добавляет &nbsp; после --, если это последний текст в строке', function() {
            var signs = this.signature.getPlain();
            var sign = signs.filter(function(sign) {
                return sign._idx === 7;
            })[0];

            expect(sign).to.be.an('object');
            expect(sign).to.have.property('convert', '-- \ntest');
        });
    });

    describe('appendToBody', function() {
        beforeEach(function() {
            this.sinon.stub(Modernizr, 'ios').value(false);
            this.sinon.spy(this.signature, 'getSignatureMatch');
        });

        describe('Режим редактирования html', function() {
             it('Префикс и постфикс правильные соответствуют html режиму письма, сначала подпись - потом тело письма', function() {
                 this.mSettings.setSettings({'signature_top': true});
                 var appendResult = this.signature.appendToBody('msgBody', 'html', false, 'test@example.com');
                 expect(appendResult).to.be.eql(
                     '<div><br/></div><div><br/></div><div>--&nbsp;</div><div>test</div><div><br/></div>msgBody'
                 );
                 expect(this.signature.getSignatureMatch).to.have.callCount(1);
             });
            it('Префикс и постфикс правильные соответствуют html режиму письма, сначала тело письма - потом подпись', function() {
                var appendResult = this.signature.appendToBody('msgBody', 'html', true, 'test@example.com');
                expect(appendResult).to.be.eql(
                    'msgBody<div><br/></div><div><br/></div><div>--&nbsp;</div><div>test</div><div><br/></div>'
                );
                expect(this.signature.getSignatureMatch).to.have.callCount(1);
            });
        });

        describe('Режим редактирования plain', function() {
            it('Префикс и постфикс правильные соответствуют html режиму письма, сначала подпись - потом тело письма', function() {
                this.mSettings.setSettings({'signature_top': true});
                var appendResult = this.signature.appendToBody('msgBodyPlain', 'plain', false, 'test@example.com');
                expect(appendResult).to.be.eql('\n\n-- \ntest\nmsgBodyPlain');
                expect(this.signature.getSignatureMatch).to.have.callCount(1);
            });
            it('Префикс и постфикс правильные соотвествуют html режиму письма, сначала тело письма - потом подпись', function() {
                var appendResult = this.signature.appendToBody('msgBody', 'plain', true, 'test@example.com');
                expect(appendResult).to.be.eql('msgBody\n\n-- \ntest\n');
                expect(this.signature.getSignatureMatch).to.have.callCount(1);
            });
        });
        describe('Выбор подписи для подстановки в композ, предварительная фильтрация подписей и сортировка', function() {
            describe('Режим письма html', function() {
                it('Есть отфильтрованные подписи для сортировки', function() {
                    this.sinon.spy(this.signature, 'getFilteredSigns');
                    this.sinon.stub(this.signature, 'getHtml').returns([
                        {
                            text: 'sign1',
                            emails: [ 'test@example.com' ],
                            convert: '<div>sign1</div>',
                            _idx: 0
                        },
                        {
                            text: 'sign2',
                            convert: '<div>sign2</div>',
                            emails: [ 'test@example.com' ],
                            _idx: 3
                        },
                        {
                            text: 'sign4',
                            convert: '<div>sign4</div>',
                            emails: [ 'user@example.com' ],
                            _idx: 1
                        },
                        {
                            text: 'sign111',
                            convert: '<div>sign111</div>',
                            emails: [],
                            _idx: 5
                        }
                    ]);

                    this.appendBody = this.signature.appendToBody('msgBody', 'html', false, 'test@example.com');

                    expect(this.appendBody).to.be.eql(
                        'msgBody<div><br/></div><div><br/></div><div>sign2</div><div><br/></div>'
                    );
                });

                it('Нет отфильтрованной подписи для сортировки, не выбрано ни одной подписи', function() {
                    this.sinon.spy(this.signature, 'getFilteredSigns');
                    this.sinon.spy(this.signature, 'setEmptySign');
                    this.sinon.stub(this.signature, 'getHtml').returns([
                        {
                            text: 'sign1',
                            emails: [ 'test@example.com' ],
                            convert: '<div>sign1</div>',
                            _idx: 0
                        },
                        {
                            text: 'sign4',
                            convert: '<div>sign4</div>',
                            emails: [ 'user@example.com' ],
                            _idx: 1
                        }
                    ]);

                    this.appendBody = this.signature.appendToBody('msgBody', 'html', false, 'test3@example.com');

                    expect(this.appendBody).to.be.eql(
                        'msgBody<div><br/></div><div><br/></div><div>--&nbsp;</div><div><br/></div>'
                    );
                    expect(this.signature.setEmptySign).to.have.callCount(1);
                });
            });
        });
    });
    describe('getFilteredSigns', function() {
        it('Емейл в верхнем регистре в процессе фильтрации приводится к нижнему и выфильтровывается в подписях',
            function() {
            var signs = [
                { text: '1', convert: '<div>1</div>', emails: [ 'User2@ya.ru' ] },
                { text: '11', convert: '<div>11</div>', emails: [ 'user1@ya.ru' ] }
            ];
            expect(this.signature.getFilteredSigns(signs, 'User2@ya.ru')).to.be.eql([
                { text: '1', convert: '<div>1</div>', emails: [ 'User2@ya.ru' ] }
            ]);
        });
        it('После фильтрации должны остаться непривязанные к какому-либо емейлу подписи', function() {
            var signs = [
                { text: '1', convert: '<div>1</div>', emails: [ 'user@ya.ru' ] },
                { text: '11', convert: '<div>11</div>', emails: [ 'user1@ya.ru' ] },
                { text: '2', convert: '<div>2</div>', emails: [ ] },
                { text: '3', convert: '<div>3</div>', emails: [ ] }
            ];
            expect(this.signature.getFilteredSigns(signs, 'user2@ya.ru')).to.be.eql([
                { text: '2', convert: '<div>2</div>', emails: [ ] },
                { text: '3', convert: '<div>3</div>', emails: [ ] }
            ]);
        });
        it('После фильтрации должны остаться привязанные к какому-либо емейлу подписи', function() {
            var signs = [
                { text: '1', convert: '<div>1</div>', emails: [ 'user@ya.ru' ] },
                { text: '33', convert: '<div>33</div>', emails: [ 'user@ya.ru' ] },
                { text: '11', convert: '<div>11</div>', emails: [ 'user1@ya.ru' ] }
            ];
            expect(this.signature.getFilteredSigns(signs, 'user@ya.ru')).to.be.eql([
                { text: '1', convert: '<div>1</div>', emails: [ 'user@ya.ru' ] },
                { text: '33', convert: '<div>33</div>', emails: [ 'user@ya.ru' ] },
            ]);
        });
        it('После фильтрации должны остаться либо непривязанные к емейлу подписи либо привязанные', function() {
            var signs = [
                { text: '1', convert: '<div>1</div>', emails: [ 'user@ya.ru' ] },
                { text: '33', convert: '<div>33</div>', emails: [ 'user@ya.ru' ] },
                { text: '11', convert: '<div>11</div>', emails: [ 'user1@ya.ru' ] },
                { text: '2', convert: '<div>2</div>', emails: [ ] },
                { text: '3', convert: '<div>3</div>', emails: [ ] }
            ];
            expect(this.signature.getFilteredSigns(signs, 'user@ya.ru')).to.be.eql([
                { text: '1', convert: '<div>1</div>', emails: [ 'user@ya.ru' ] },
                { text: '33', convert: '<div>33</div>', emails: [ 'user@ya.ru' ] },
                { text: '2', convert: '<div>2</div>', emails: [ ] },
                { text: '3', convert: '<div>3</div>', emails: [ ] }
            ]);
        });
    });
    describe('sort', function() {
        it('Дефолтная с параметром isDefault=true приоритетнее isDefault=false (нет других приоритетных)', function() {
            var signs = [
                { text: 'подпись с isDefault', isDefault: true, emails: [] },
                { text: 'подпись с isDefault=true', isDefault: false, emails: [] }
            ];
            expect(this.signature.sort('user@ya.ru')(signs[0], signs[1])).to.be.eql(-1);
            expect(signs.sort(this.signature.sort('user@ya.ru', 'ru'))).to.be.eql(signs);
        });
        it('Подпись с нужным языком приоритетнее чем подпись другого языка', function() {
            var signs = [
                { text: 'английская подпись', isDefault: false, emails: [], lang: 'en' },
                { text: 'русская подпись', isDefault: false, emails: [], lang: 'ru' }
            ];
            expect(this.signature.sort('user@ya.ru', 'ru')(signs[0], signs[1])).to.be.eql(1);
            expect(signs.sort(this.signature.sort('user@ya.ru', 'ru'))).to.be.eql([
                { text: 'русская подпись', isDefault: false, emails: [], lang: 'ru' },
                { text: 'английская подпись', isDefault: false, emails: [], lang: 'en' }
            ]);
        });
        it('Привязанная к емейлу подпись приоритетнее непривязанной', function() {
            var signs = [
                { text: 'русская подпись', isDefault: false, emails: [ ], lang: 'ru' },
                { text: 'английская подпись', isDefault: false, emails: [ 'user@ya.ru' ], lang: 'en' }
            ];
            expect(this.signature.sort('user@ya.ru', 'ru')(signs[0], signs[1])).to.be.eql(1);
            expect(signs.sort(this.signature.sort('user@ya.ru', 'ru'))).to.be.eql([
                { text: 'английская подпись', isDefault: false, emails: [ 'user@ya.ru' ], lang: 'en' },
                { text: 'русская подпись', isDefault: false, emails: [ ], lang: 'ru' }
            ]);
        });
        it('В подписях одинакового приоритета (непривязанные) выше будет та что создана позже', function() {
            var signs = [
                { text: 'русская подпись', isDefault: false, emails: [], lang: 'ru', _idx: 0  },
                { text: 'русская свежая подпись', isDefault: false, emails: [], lang: 'ru', _idx: 1 }
            ];
            expect(this.signature.sort('user@ya.ru', 'ru')(signs[0], signs[1])).to.be.eql(1);
            expect(signs.sort(this.signature.sort('user@ya.ru', 'ru'))).to.be.eql([
                { text: 'русская свежая подпись', isDefault: false, emails: [], lang: 'ru', _idx: 1 },
                { text: 'русская подпись', isDefault: false, emails: [], lang: 'ru', _idx: 0  }
            ]);
        });
        it('В подписях одинакового приоритета (привязанные) выше будет та что создана позже', function() {
            var signs = [
                { text: 'русская подпись', isDefault: false, emails: [ 'user@ya.ru' ], lang: 'ru', _idx: 0  },
                { text: 'русская свежая подпись', isDefault: false, emails: [ 'user@ya.ru' ], lang: 'ru', _idx: 1 }
            ];
            expect(this.signature.sort('user@ya.ru', 'ru')(signs[0], signs[1])).to.be.eql(1);
            expect(signs.sort(this.signature.sort('user@ya.ru', 'ru'))).to.be.eql([
                { text: 'русская свежая подпись', isDefault: false, emails: [ 'user@ya.ru' ], lang: 'ru', _idx: 1 },
                { text: 'русская подпись', isDefault: false, emails: [ 'user@ya.ru' ], lang: 'ru', _idx: 0  }
            ]);
        });
    });
    describe('setEmptySign', function() {
        it('если режим письма html, то вернет подпись <div>--&nbsp;</div>', function() {
            expect(this.signature.setEmptySign(true)).to.be.eql({text: '<div>-- </div>', convert: '<div>--&nbsp;</div>'});
        });
        it('если режим письма plain, то вернет подпись -- ', function() {
            expect(this.signature.setEmptySign(false)).to.be.eql({text: '<div>-- </div>', convert: '-- '});
        });
    });
});
