describe('Daria.url', function() {
    beforeEach(function() {
        this.originalUid = Daria.uid;
        Daria.uid = '123';
    });

    afterEach(function() {
        Daria.uid = this.originalUid;
    });

    describe('attachment-related', function() {
        var testAttachmentContract = function(_context, _method, otherArguments) {
            var context, method;

            beforeEach(function() {
                context = _.get(window, _context);
                method = context[_method];
            });

            it('должен кинуть эксцепшен, если в аттачменте hid нулевой длинны', function() {
                var attachment = {
                    'name-uri-encoded': 'whatsoever',
                    'name': 'whatever',
                    'hid': ''
                };

                expect(function() {
                    method.apply(context, [attachment].concat(otherArguments));
                }).to.throw('Attachment should have a hid');
            });

            it('должен подставлять name-uri-encoded аттача в параметр name, если есть', function() {
                var attachment = {
                    'name-uri-encoded': 'whatsoever',
                    'name': 'whatever',
                    'hid': '1.2.3'
                };
                expect(getParam(method.apply(context, [attachment].concat(otherArguments)), 'name'))
                    .to.be.equal(attachment['name-uri-encoded']);
            });

            it('должен подставлять name аттача в параметр name, если нет name-uri-encoded', function() {
                var attachment = {
                    'name': 'whatever',
                    'hid': '1.2.3'
                };
                expect(getParam(method.apply(context, [attachment].concat(otherArguments)), 'name'))
                    .to.be.equal(attachment.name);
            });

            it('должен подставлять пустую строку в параметр name, если в аттаче нет name-uri-encoded и name', function() {
                var attachment = {
                    'hid': '1.2.3'
                };
                expect(getParam(method.apply(context, [attachment].concat(otherArguments)), 'name')).to.be.equal("");
            });
        };

        var getParam = function(url, name) {
            var result = null;
            url.substr(url.indexOf('?'))
                .substr(1)
                .split('&')
                .forEach(function(param) {
                    var split = param.split('=');
                    if (split[0] === name) {
                        result = split[1];
                    }
                });

            return result;
        };

        beforeEach(function() {
            this.attachment = {
                'name-uri-encoded': 'whatsoever',
                'name': 'whatever',
                'hid': '1.12.23'
            };

            this.mid = 'wellImAMid';

            this.DariaXSLConfigBackup = Daria.Config;
            Daria.Config = {
                'docviewer-frontend-host': 'docFront'
            };
        });
        afterEach(function() {
            Daria.Config = this.DariaXSLConfigBackup;
        });

        describe('Daria.url.docviewer(attachment, mid)', function() {
            testAttachmentContract('Daria.url', 'docviewer', ['wellImAMid']);

            it('должен кинуть эксцепшен, если mid нулевой длинны', function() {
                var that = this;
                expect(function() {
                    Daria.url.docviewer(that.attachment, '');
                }).to.throw('Invalid message id passed');
            });

            it('должен откусывать "t" от mid\'а', function() {
                expect(getParam(Daria.url.docviewer(this.attachment, 't165507286305872714'), 'url'))
                    .to.be.equal(encodeURIComponent('ya-mail://165507286305872714/' + this.attachment.hid));
            });

            it('Урл должен начинаться с docviewer-frontend-host', function() {
                var config = Daria.Config;
                var url = Daria.url.docviewer(this.attachment, this.mid);
                expect(url.split('?')[0]).to.be.equal(config['docviewer-frontend-host'] + '/');
            });

            it('должен использовать Daria.url._getAttachmentName для определения имени аттача', function() {
                var mock = this.sinon.mock(Daria.url)
                    .expects('_getAttachmentName')
                    .twice()
                    .withExactArgs(this.attachment)
                    .returns('whatever');

                expect(getParam(Daria.url.docviewer(this.attachment, this.mid), 'name'))
                    .to.be.equal(Daria.url._getAttachmentName(this.attachment));

                mock.verify();
                Daria.url._getAttachmentName.restore();
            });

            it('Урл для доквьювера должен выглядеть, как urlEncoded строка ya-mail://mid/attachment.hid', function() {
                expect(getParam(Daria.url.docviewer(this.attachment, this.mid), 'url'))
                    .to.be.equal(encodeURIComponent('ya-mail://' + this.mid + '/' + this.attachment.hid));
            });

        });

        describe('Daria.url.attachment(attachment, mid, options)', function() {

            it('должен вернуть undefined если поле mid не строка', function() {
                expect(Daria.url.attachment(this.attachment, [])).to.be.equal(undefined);
            });

            describe('если поле url аттача не пустое', function() {
                beforeEach(function() {
                    this.attachment.url = 'http://yadi.sk/111/sdlfkjsldfkj?uid=123';
                });

                it('должен вернуть поле url аттача', function() {
                    expect(Daria.url.attachment(this.attachment, this.mid)).to.be.equal(this.attachment.url);
                });

                it('не должен проверять мид', function() {
                    expect(Daria.url.attachment(this.attachment, "")).to.be.equal(this.attachment.url);
                });

                it('не должен проверять наличие имени у аттача', function() {
                    this.attachment.name = '';
                    this.attachment['name-uri-encoded'] = '';
                    expect(Daria.url.attachment(this.attachment, "")).to.be.equal(this.attachment.url);
                });
            });

            describe('у аттача нет или пустое поле url', function() {
                testAttachmentContract('Daria.url', 'attachment', ['wellImAMid']);

                it('должен кинуть эксцепшен, если mid нулевой длинны', function() {
                    var that = this;
                    expect(function() {
                        Daria.url.attachment(that.attachment, '');
                    }).to.throw('Invalid message id passed');
                });

                it('должен обращатсья к хоструту по пути {host}/message_part/ + name-uri-encoded аттача, если есть', function() {
                    var url = Daria.url.attachment(this.attachment, this.mid);
                    expect(url.split('?')[0]).to.be.equal('https://' + Daria.Config.host + '/message_part/' + this.attachment['name-uri-encoded']);
                });

                it('должен обращатсья к хоструту по пути {host}/message_part/ + name аттача, если нет name-uri-encoded', function() {
                    this.attachment['name-uri-encoded'] = null;
                    var url = Daria.url.attachment(this.attachment, this.mid);
                    expect(url.split('?')[0]).to.be.equal('https://' + Daria.Config.host + '/message_part/' + this.attachment.name);
                });

                it('должен подставлять hid аттача в параметр hid', function() {
                    expect(getParam(Daria.url.attachment(this.attachment, this.mid), 'hid'))
                        .to.be.equal(this.attachment.hid);
                });

                it('должен подставлять mid в параметр ids', function() {
                    expect(getParam(Daria.url.attachment(this.attachment, this.mid), 'ids'))
                        .to.be.equal(this.mid);
                });

                describe('options', function() {
                    ['no_disposition', 'exif_rotate', 'thumb'].forEach(function(option) {
                        it('должен подставлять опцию ' + option + ' как &' + option + '=y', function() {
                            var url = Daria.url.attachment(this.attachment, this.mid, {
                                'no_disposition': true,
                                'exif_rotate': 'yes',
                                'thumb': 1
                            });

                            expect(getParam(url, option)).to.be.equal('y');
                        });

                        it('должен игнорировать опцию ' + option + ', если она приводятся к false', function() {
                            var url = Daria.url.attachment(this.attachment, this.mid, {
                                'no_disposition': '',
                                'exif_rotate': 0,
                                'thumb': false
                            });

                            expect(getParam(url, option)).to.be.equal(null);
                        });
                    });

                    it('должен игнорировать незнакомые опции', function() {
                        var url = Daria.url.attachment(this.attachment, this.mid, {
                            'put_me_in_a_link': true,
                            'exif_rotate': true
                        });

                        expect(getParam(url, 'put_me_in_a_link')).to.be.equal(null);
                    });
                });
            });
        });
    });

    describe('#upload', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'ckey').value('test');
            this.sinon.stub(Daria.Config, 'product').value('RUS');
        });

        it('Должен вернуть url для загрузки документов', function() {
            expect(Daria.url.upload()).to.be.contain(Daria.api['upload-attachment']);
        });

        it('Должен вернуть url содержащий ckey', function() {
            expect(Daria.url.upload()).to.be.contain(Daria.supplant('ckey={ckey}', {
                ckey: Daria.ckey
            }));
        });

        it('Не должен ломать url который содержит query параметры', function() {
            var now = Date.now();
            this.sinon.stub(Date, 'now').returns(now);
            this.sinon.stub(Daria.api, 'upload-attachment').value('/web-api/upload/liza?bar=foo');
            this.sinon.stub(Daria, 'commonRequestParams').returns({
                _ckey: '123',
                _uid: '321'
            });

            var url = $.url(Daria.url.upload());

            expect(Daria.parseQuery(url.query())).to.be.eql({
                bar: 'foo',
                _ckey: '123',
                _uid: '321'
            });
        });

        it('Должен вернуть url для загрузки картинок в подпись', function() {
            expect(Daria.url.upload(null, 'signature')).have.been.contain(Daria.api['upload-signature-images']);
        });
    });

    describe('#isEMLSuffixMissing', function() {
        it('должен возращать true, если Content-Type eml, и у его другое расширения', function() {
            expect(Daria.url.isEMLSuffixMissing('message', 'rfc822', 'filename.txt')).to.be.equal(true);
        });

        it('должен возращать true, если Content-Type eml, и у его другое расширения(кириллица)', function() {
            var filename = encodeURIComponent('кирилица.лял.txt');
            expect(Daria.url.isEMLSuffixMissing('message', 'rfc822', filename)).to.be.equal(true);
        });

        it('должен возращать true, если Content-Type eml, и у нет нет расширения', function() {
            expect(Daria.url.isEMLSuffixMissing('message', 'rfc822', 'filename')).to.be.equal(true);
        });

        it('должен возращать true, если Content-Type eml, и у нет нет расширения(кириллица)', function() {
            var filename = encodeURIComponent('кирилица.лял');
            expect(Daria.url.isEMLSuffixMissing('message', 'rfc822', filename)).to.be.equal(true);
        });

        it('должен возращать false, если Content-Type eml, и у него правильное расширение eml', function() {
            expect(Daria.url.isEMLSuffixMissing('message', 'rfc822', 'filename.eml')).to.be.equal(false);
        });

        it('должен возращать false, если Content-Type eml, и у него правильное расширение eml(кириллица)', function() {
            var filename = encodeURIComponent('кирилица.лял.eml');
            expect(Daria.url.isEMLSuffixMissing('message', 'rfc822', filename)).to.be.equal(false);
        });

        it('должен возращать false, если Content-Type не eml, но у него расширение eml', function() {
            expect(Daria.url.isEMLSuffixMissing('text', 'html', 'filename.eml')).to.be.equal(false);
        });

        it('должен возращать false, если Content-Type не eml, но у него расширение eml(кириллица)', function() {
            var filename = encodeURIComponent('кирилица.лял.eml');
            expect(Daria.url.isEMLSuffixMissing('text', 'html', filename)).to.be.equal(false);
        });

        it('должен возращать false, если Content-Type не eml и расширение не eml', function() {
            expect(Daria.url.isEMLSuffixMissing('text', 'html', 'filename.html')).to.be.equal(false);
        });

        it('должен возращать false, если Content-Type не eml и расширение не eml(кириллица)', function() {
            var filename = encodeURIComponent('кирилица.лял.html');
            expect(Daria.url.isEMLSuffixMissing('text', 'html', filename)).to.be.equal(false);
        });

        it('должен возращать false, если Content-Type не eml, и у него нет расширения', function() {
            expect(Daria.url.isEMLSuffixMissing('text', 'html', 'filename')).to.be.equal(false);
        });

        it('должен возращать false, если Content-Type не eml, и у него нет расширения(кириллица)', function() {
            var filename = encodeURIComponent('кирилица-ляля');
            expect(Daria.url.isEMLSuffixMissing('text', 'html', filename)).to.be.equal(false);
        });

        it('должен возращать false, если имя файла пустое', function() {
            expect(Daria.url.isEMLSuffixMissing('text', 'html', '')).to.be.equal(false);
            expect(Daria.url.isEMLSuffixMissing('message', 'rfc822', '')).to.be.equal(false);
        });
    });

    describe('#archive', function() {
        beforeEach(function() {
            this.messageData = {
                attachment: [
                    {
                        'name': 'whatever',
                        'hid': '0.0.1'
                    }
                ],
                info: {
                    date: {
                        timestamp: '1441276696000'
                    }
                }
            };
        });
        it('должен возвращать ссылку без параметра "archive", если в письме одно вложение', function() {
            var archiveUrl = Daria.url.archive(this.messageData, '111111');
            expect($.url(archiveUrl).Query).to.not.have.property('archive');
        });
    });

    describe('#_getAttachmentName', function() {
        beforeEach(function() {
            this.attachment = {
                disposition_filename: 'Велопарковки2019.jpg',
                disposition_value: 'attachment',
                fileext: '.jpg',
                filename: 'Велопарковки2019',
                name: 'Велопарковки2019.jpg',
                'name-uri-encoded': '%D0%92%D0%B5%D0%BB%D0%BE%D0%BF%D0%B0%D1%80%D0%BA%D0%BE%D0%B2%D0%BA%D0%B82019.jpg'
            };
        });

        it('должен вернуть `name-uri-encoded`', function() {
            expect(Daria.url._getAttachmentName(this.attachment))
                .to.be.equal('%D0%92%D0%B5%D0%BB%D0%BE%D0%BF%D0%B0%D1%80%D0%BA%D0%BE%D0%B2%D0%BA%D0%B82019.jpg');
        });

        it('должен вернуть `name`', function() {
            delete this.attachment['name-uri-encoded'];
            expect(Daria.url._getAttachmentName(this.attachment)).to.be.equal('Велопарковки2019.jpg');
        });

        it('должен вернуть `filename`', function() {
            delete this.attachment['name-uri-encoded'];
            delete this.attachment.name;
            expect(Daria.url._getAttachmentName(this.attachment)).to.be.equal('Велопарковки2019');
        });
    });
});
