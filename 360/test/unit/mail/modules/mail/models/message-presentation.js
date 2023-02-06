describe('Daria.mMessagePresentation', function() {

    describe('params', function() {

        it('параметры Daria.mMessagePresentation должны быть полностью как у Daria.mMessage + fid - force', function() {
            var mMessageInfoParams = _.omit(ns.Model.info('message').params, 'force');
            var mMessagePresentationInfoParams = ns.Model.info('message-presentation').params;

            // TODO у нас сейчас lodash 3.10.1, а в 4.0.0 добавился _.differenceWith с котором можно сделать одну проверкой.
            expect(_.difference(_.keys(mMessagePresentationInfoParams), _.keys(mMessageInfoParams))).to.be.eql([ 'fid' ]);
            expect(_.difference(_.keys(mMessageInfoParams), _.keys(mMessagePresentationInfoParams))).to.be.eql([]);
        });

    });

    describe('#onInit', function() {

        beforeEach(function() {
            this.mMessagePresentation = ns.Model.get('message-presentation', { ids: '123', fid: '1' });
        });

        it('сразу после создания модель инициализируется', function() {
            expect(this.mMessagePresentation.status).to.be.eql(ns.M.STATUS.INITED);
        });

        it('сразу после создания модель не валидна', function() {
            expect(this.mMessagePresentation.isValid()).to.be.eql(false);
        });

        it('сразу после создания у модели пустые данные (null)', function() {
            expect(this.mMessagePresentation.getData()).to.be.eql(null);
        });

    });

    describe('#fill', function() {
        var copiedFields = [ 'flags', 'field', 'firstline' ];

        beforeEach(function() {
            this.mMessage = ns.Model.get('message', { ids: '123' });
            this.mMessagePresentation = ns.Model.get('message-presentation', _.extend({}, this.mMessage.params, { fid: '1' }));

            this.mMessage.setData({
                firstline: 'Hi, I am the firstline',
                field: [
                    { email: 'chestozo@gmail.com', name: 'Roman Kartsev', type: 'from' },
                    { email: 'chestozo@ya.ru', name: 'Карцев Роман', type: 'to' },
                    { email: 'chestozo@gmail.com', name: 'chestozo@gmail.com', type: 'reply-to' }
                ]
            });

            this.mMessagePresentation.fill(this.mMessage);
        });

        it('копируется ограниченный список полей', function() {
            var fields = Object.keys(this.mMessagePresentation.getData());

            // TODO у нас сейчас lodash 3.10.1, а в 4.0.0 добавился _.differenceWith с котором можно сделать одну проверкой.
            expect(_.difference(fields, copiedFields).length).to.be.eql(0);
            expect(_.difference(copiedFields, fields).length).to.be.eql(0);
        });

        copiedFields.forEach(function(fieldName) {
            it('копируется поле ' + fieldName, function() {
                expect(this.mMessagePresentation.get('.' + fieldName)).to.be.eql(this.mMessage.get('.' + fieldName));
            });
        });

    });

    describe('#ensureOnDestroyBinded', function() {

        beforeEach(function() {
            this.mThreadMessage1 = ns.Model.get('message', { ids: '123' });
            this.mThreadMessage2 = ns.Model.get('message', { ids: '123', hid: 'x' });

            this.mMessagePresentation11 = ns.Model.get('message-presentation', { ids: '123', fid: '1' });
            this.mMessagePresentation12 = ns.Model.get('message-presentation', { ids: '123', fid: '4' });

            this.sinon.spy(this.mMessagePresentation11, 'destroy');
            this.sinon.spy(this.mMessagePresentation12, 'destroy');
            this.sinon.spy(this.mMessagePresentation11, 'destroyWith');
            this.sinon.stub(Jane.ErrorLog, 'send');
        });

        describe('должна быть выполнена подписка на событие ns-model-destroyed', function() {

            it('должны подписаться на событие модели Daria.mMessage', function() {
                this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage1);
                expect(this.mMessagePresentation11.destroyWith).to.have.callCount(1);
            });

            it('должны подписаться на событие ns-model-destroyed модели Daria.mMessage', function() {
                var callInfo;
                this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage1);
                callInfo = this.mMessagePresentation11.destroyWith.getCall(0);
                expect(callInfo.args[0]).to.be.eql([ this.mThreadMessage1 ]);
            });

        });

        it('повторная подписка на событие ns-model-destroyed от той же модели Daria.mMessage не должна выполняться', function() {
            this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage1);
            this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage1);
            expect(this.mMessagePresentation11.destroyWith).to.have.callCount(1);
        });

        it('повторная подписка на событие ns-model-destroyed от другой модели Daria.mMessage не должна выполняться', function() {
            this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage1);
            this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage2);
            expect(this.mMessagePresentation11.destroyWith).to.have.callCount(1);
        });

        describe('повторная подписка на событие ns-model-destroyed от другой модели Daria.mMessage должна логироваться как ошибка', function() {

            beforeEach(function() {
                this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage1);
                this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage2);
            });

            it('должна логироваться ошибка', function() {
                expect(Jane.ErrorLog.send).to.have.callCount(1);
            });

            it('должно логироваться много полезной информации', function() {
                var callInfo = Jane.ErrorLog.send.getCall(0);

                expect(callInfo.args[0]).to.be.eql({
                    'errorType': 'message-presentation.error',
                    'params': '{"ids":"123","fid":"1"}',
                    'errorMessage': 'ensureOnDestroyBinded called more then once: was binded to model=message&ids=123, now binding to model=message&ids=123&hid=x'
                });
            });

        });

        describe('при уничтожении модели Daria.mMessage уничтожаются все связанные модели Daria.mMessagePresentation', function() {

            beforeEach(function() {
                this.mMessagePresentation11.ensureOnDestroyBinded(this.mThreadMessage1);
                this.mMessagePresentation12.ensureOnDestroyBinded(this.mThreadMessage1);
                this.mThreadMessage1.destroy();
            });

            it('первая модель Daria.mMessagePresentation должна себя уничтожить при уничтожении родительской модели', function() {
                expect(this.mMessagePresentation11.destroy).to.have.callCount(1);
            });

            it('вторая модель Daria.mMessagePresentation должна себя уничтожить при уничтожении родительской модели', function() {
                expect(this.mMessagePresentation12.destroy).to.have.callCount(1);
            });

        });

        describe('вызывается вручную', function() {

            it('должен вызываться по время split-а модели Daria.mMessages', function() {
                var mMessages = ns.Model.get('messages', { current_folder: '3' });
                var mMessagePresentation = ns.Model.get('message-presentation', { ids: '1', fid: '3' });

                this.sinon.spy(mMessagePresentation, 'ensureOnDestroyBinded');

                mMessages.setData({
                    message: [
                        { mid: '1', lid: ['1'] }
                    ]
                });

                expect(mMessagePresentation.ensureOnDestroyBinded).to.have.callCount(1);
            });

            describe('должен вызываться во время обновления информции о тредном письме в Daria.mMessage.updateThreadInfo', function() {

                beforeEach(function() {

                    this.mThreadMessage = ns.Model.get('message', { ids: 't159033361841522075' })
                        .setData({
                            'mid': 't159033361841522075',
                            'fid': '1',
                            'firstline': '3 the new one',
                            'flags': {},
                            'lid': [],
                            'field': []
                        });

                    var newMessage = ns.Model.get('message', { ids: '159033361841522137' })
                        .setData({
                            'mid': '159033361841522137',
                            'fid': '1',
                            'firstline': '3 the new one',
                            'flags': {},
                            'field': []
                        });

                    this.mMessagePresentation = ns.Model.get('message-presentation', _.extend({}, this.mThreadMessage.params, { fid: '1' }));

                    this.sinon.spy(this.mMessagePresentation, 'ensureOnDestroyBinded');

                    this.mThreadMessage.updateThreadInfo(newMessage);

                });

                it('метод вызывается', function() {
                    expect(this.mMessagePresentation.ensureOnDestroyBinded).to.have.callCount(1);
                });

                it('в метод передаётся тредное письмо', function() {
                    expect(this.mMessagePresentation.ensureOnDestroyBinded.calledWith(this.mThreadMessage)).to.be.eql(true);
                });

            });



        });

    });

});
