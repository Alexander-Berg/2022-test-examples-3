describe('b-campaign-edit-panel', function() {
    var ctx = {
        block: 'b-campaign-edit-panel',
        js: {
            modelParams: {
                id:"13902219",
                name:"m-campaign"
            }
        },
        content: [
            {
                block: 'b-campaign-edit-panel',
                elem: 'save',
                elemMods: { active: 'yes' }
            }
        ]
    },
        block,
        phrase,
        phraseHelper,
        groupsList,
        sandbox;

    u.stubCurrencies();

    function changeValues(value, noRespond) {
        phrase.set('price', value);
        phraseHelper.eachModel(function(model) {
            model.set('adgroup_id', 1);
            model.set('price', value);
        });

        sandbox.clock.tick(100);

        block.saveBtn.trigger('click');

        sandbox.clock.tick(5000);

        // в некоторых тестах нужно состояние "ожидания" ответа, для этого не отвечаем фейковым сервером
        noRespond || sandbox.server.respond();
    }

    beforeEach(function() {
        var campaign = BEM.MODEL.create({
            id:"13902219",
            name:"m-campaign"
        }),
        campaignData = {
            "cid": "13902219",
            "mediaType": "text",
            "minus_words": [
                "!Гена",
                "!Железная"
            ]
        },
        group = BEM.MODEL.create({
            id: '1',
            name: 'm-group'
        });

        group.set('banners', [{
            "modelId": "1313242853",
            "bid": 1313242853
        }]);
        campaign.update(campaignData);
        
        phraseHelper = BEM.blocks['i-phrases-prices-helper'].getInstance('all', campaign);
        groupsList = u.createBlock({ block: 'b-groups-list', js: {
            campaign: campaignData,
            groupIds: ['1']
        }});
        sandbox = sinon.sandbox.create({ useFakeServer: true, useFakeTimers: true });

        if (!block) {
            block = u.createBlock(ctx);
        }
    });

    afterEach(function() {
        phraseHelper.destruct();
        groupsList.destruct();
        sandbox.restore();
    });

    describe('Изначальные состояния', function() {
        it('Кнопка Сохранить задизейблена', function () {
             expect(block.saveBtn).to.haveMod('disabled', 'yes');
        });

        it('На кнопке сохранения текст "Сохранить"', function() {
            expect(block.saveBtn.getText()).to.equal('Сохранить')
        });

        it('Кнопка сохранения c темой "Normal"', function() {
            expect(block.saveBtn).to.haveMod('theme', 'normal');
        });

        it('На window привязано событие onbeforeunload', function() {
            var onBeforeUnloadStub = sinon.stub(block, '_onBeforeUnload');

            $(window).trigger('beforeunload');

            expect(onBeforeUnloadStub.called).not.to.be.true;

            onBeforeUnloadStub.restore();
        });

        it('isPricesChanged возвращает false', function() {
            expect(block.isPricesChanged()).to.be.false
        });

        it('unbindLeave отвязывает подписку на onbeforeunload', function() {
            block.unbindLeave();

            expect(window.onbeforeunload).to.be.null;
        });
    });

    describe('События смены фраз', function() {
        beforeEach(function() {
            phrase = BEM.MODEL.create(block.getModelParams('phrase'));
        });

        afterEach(function() {
            phrase.destruct();
        });

        describe('Состояния кнопки сохранения', function() {
            it('При изменениях в фразе кнопка активна', function() {
                phrase.set('key_words', 'bla-bla');

                sandbox.clock.tick(100);

                expect(block.saveBtn).to.not.haveMod('disabled');
            });

            it('При изменениях в фразе текст кнопки - "Сохранить"', function() {
                phrase.set('key_words', 'bla-bla');

                sandbox.clock.tick(100);

                expect(block.saveBtn.getText()).to.equal('Сохранить')
            });

            it('При изменениях в фразе тема кнопки - "action"', function() {
                phrase.set('key_words', 'bla-bla');

                sandbox.clock.tick(100);

                expect(block.saveBtn).to.haveMod('theme', 'action');
            });
        });
    });

    describe('Сохранение', function() {
        beforeEach(function() {
            phrase = BEM.MODEL.create(block.getModelParams('phrase'));
            sandbox.server.respondWith("POST", '/registered/main.pl', [200,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, ok: true } )]);
        });

        afterEach(function() {
            phrase.destruct();
            sandbox.restore();
        });

        it('В начале сохранения кнопка не активна', function() {
            changeValues(55.00);
            expect(block.saveBtn).to.haveMod('disabled', 'yes');
        });

        it('В начале сохранения текст кнопки меняется на "Сохраняется"', function() {
            changeValues(34.88, true);
            expect(block.saveBtn.getText()).to.equal('Сохраняется...');
        });
    });

    describe('Неуспешное сохранение', function() {
        beforeEach(function() {
            phrase = BEM.MODEL.create(block.getModelParams('phrase'));
            sandbox.server.respondWith("POST", '/registered/main.pl', [200,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, ok: true } )]);
        });

        afterEach(function() {
            phrase.destruct();
            sandbox.restore();
        });

        it('При неуспешном сохранении алерт не всплывает', function() {
            BEM.blocks['b-confirm'].alert = sinon.spy();

            changeValues(34.00, true);

            sandbox.server.respondWith("POST", '/registered/main.pl', [404,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, ok: false } )]);

            sandbox.server.respond();

            expect(BEM.blocks['b-confirm'].alert.called).to.be.false;
        });

        it('При неуспешном сохранении кнопка активна', function() {
            changeValues(28.00, true);

            sandbox.server.respondWith("POST", '/registered/main.pl', [404,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, ok: false } )]);

            sandbox.server.respond();

            expect(block.saveBtn).to.not.haveMod('disabled');
        });

        it('При неуспешном сохранении текст кнопки "Сохранить"', function() {
            changeValues(28.00, true);

            sandbox.server.respondWith("POST", '/registered/main.pl', [404,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, ok: false } )]);

            sandbox.server.respond();

            expect(block.saveBtn.getText()).to.equal('Сохранить');
        });

        it('При неуспешном сохранении тема кнопки "action"', function() {
            changeValues(28.00, true);

            sandbox.server.respondWith("POST", '/registered/main.pl', [404,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, ok: false } )]);

            sandbox.server.respond();

            expect(block.saveBtn).to.haveMod('theme', 'action');
        })
    });

    describe('Успешное сохранение', function() {
        beforeEach(function() {
            phrase = BEM.MODEL.create(block.getModelParams('phrase'));
            sandbox.server.respondWith("POST", '/registered/main.pl', [200,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, ok: true } )]);
        });

        afterEach(function() {
            phrase.destruct();
            sandbox.restore();
        });

        it('После успешного сохранения всплывает confirm', function() {
            BEM.blocks['b-confirm'].alert = sinon.spy();

            changeValues(54.00);

            sandbox.server.respond();
            sandbox.clock.tick(1000);

            expect(BEM.blocks['b-confirm'].alert.called).to.be.true;
        });

        it('После успешного сохранения кнопка не активна', function() {

            changeValues(34.00);

            sandbox.server.respond();
            sandbox.clock.tick(1000);

            expect(block.saveBtn).to.haveMod('disabled', 'yes');
        });

        it('После успешного сохранения текст кнопки меняется на "Сохранено"', function() {
            changeValues(23.00);

            sandbox.server.respond();
            sandbox.clock.tick(1000);

            expect(block.saveBtn.getText()).to.equal('Сохранено');
        });

        it('После успешного сохранения тема меняется на normal', function() {
            changeValues(59.00);

            sandbox.server.respond();
            sandbox.clock.tick(1000);

            expect(block.saveBtn).to.haveMod('theme', 'normal');
        });
    });

    describe('Изменения ставок', function() {
        beforeEach(function() {
            phrase = BEM.MODEL.create(block.getModelParams('phrase'));
            sandbox.server.respondWith("POST", '/registered/main.pl', [200,
                {"Content-Type":"application/json"}, JSON.stringify({ "requestId": 1, ok: true } )]);
        });

        afterEach(function() {
            phrase.destruct();
            sandbox.restore();
        });

        it('При изменении ставок isPricesChanged возвращает true', function() {
            changeValues(10);

            expect(block.isPricesChanged()).to.be.true;
        });

        it('При отсутствии изменении ставок isPricesChanged возвращает false', function() {
            changeValues(10);

            sandbox.server.respond();

            changeValues(10);

            expect(block.isPricesChanged()).to.be.false;
        })

    });
});
