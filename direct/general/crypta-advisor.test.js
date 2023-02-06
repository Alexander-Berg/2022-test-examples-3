describe('crypta-advisor', function() {
    var block,
        sandbox,
        reachResponses200 = {
            success: {
                "result": {
                    "requestId": "SuccessRequestId",
                    "banner_formats": [
                        { "format": "960x640", "increase_percent": 0 },
                        { "format": "640x960", "increase_percent": 0 },
                        { "format": "970x250", "increase_percent": 2.939154348573392918528704022000688 },
                        { "format": "728x90", "increase_percent": 6.686146442076314884840151254726710 },
                        { "format": "640x100", "increase_percent": 0 },
                        { "format": "640x200", "increase_percent": 0 },
                        { "format": "300x500", "increase_percent": 0.7390855964248882777586799587487109 },
                        { "format": "336x280", "increase_percent": 6.754898590580955654864214506703334 },
                        { "format": "300x600", "increase_percent": 1.151598487452732897903059470608457 },
                        { "format": "300x250", "increase_percent": 18.87246476452389137160536266758336 }]
                }, "success": true
            },
            empty: { "result": { "requestId": "EmptyRequestId", "banner_formats": [] }, "success": true }
        };

    function createBlock(data) {
        block = u.getInitedBlock({
            block: 'crypta-advisor',
            js: {
                data: data || {}
            }
        });
    }

    function destructBlock() {
        block.destruct();
    }

    describe('Инициализация', function() {
        var stubRequest;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });
            stubRequest = sandbox
                .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReachRecommendation')
                .resolves(reachResponses200.success);

            createBlock();
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        it('Не должно быть запросов', function() {
            expect(stubRequest.called).to.be.false;
        });

        it('Должен быть блок expander', function() {
            expect(block).to.haveBlock('b-expander');
        });

        it('Должен быть блок expander, без модификатора open', function() {
            expect(block.findBlockInside('b-expander')).to.not.haveMod('open', 'yes');
        });

    });

    describe('Поведение', function() {
        var stubRequest;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });
            stubRequest = sandbox
                .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReachRecommendation')
                .resolves(reachResponses200.success);

            createBlock();
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        it('При нажатии на expander выполняется запрос', function() {
            block.findBlockInside('b-expander').elem('header').click();

            expect(stubRequest.called).to.be.true;
        });

    });

    describe('Успешная рекомендация', function() {
        var stubRequest;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });

            stubRequest = sandbox
                .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReachRecommendation')
                .resolves(reachResponses200.success);

            createBlock();

            return new Promise(function (resolve, reject){
                block.findBlockInside('b-expander').elem('header').click();
                block.on('success', function() {
                    resolve();
                });
            });
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        it('Отображается результат', function() {
            expect(block).to.haveElem('recommendation');
        });

        it('Максимальное кол-во результатов 3', function() {
            expect(block.elem('banner').length).to.be.eq(3);
        });

        it('Самый первый результат имеет самый большой процент и отформатирован', function() {
            expect(block.elem('increase-percent').eq(0).text()).to.be.eq('+18,9%');
        });

    });

    describe('Пустая рекомендация', function() {
        var stubRequest;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });

            stubRequest = sandbox
                .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReachRecommendation')
                .resolves(reachResponses200.empty);

            createBlock();

            return new Promise(function (resolve, reject){
                block.findBlockInside('b-expander').elem('header').click();
                block.on('success', function() {
                    resolve();
                });
            });
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        it('Отображается результат', function() {
            expect(block).to.haveElem('recommendation');
        });

        it('В нем нет списка баннеров', function() {
            expect(block).to.not.haveElems('banner');
        });

        it('Но есть элемент с сообщением о пустом прогнозе', function() {
            expect(block).to.haveElem('proposal-empty');
        });

    });

    describe('Неудачный запрос', function() {
        var stubRequest;

        beforeEach(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });

            stubRequest = sandbox
                .stub(BEM.blocks['i-web-api-request'].mediareach, 'getReachRecommendation')
                .rejects();

            createBlock();

            return new Promise(function (resolve, reject){
                block.findBlockInside('b-expander').elem('header').click();
                block.on('error', function() {
                    resolve();
                });
            });
        });

        afterEach(function() {
            sandbox.restore();
            destructBlock();
        });

        it('Отображается элемент с текстом ошибки', function() {
            expect(block).to.haveElem('recommendation-error');
        });

    });


});
