describe('b-campaign-email-notifications-adapter', function() {
    var block,
        notificationsData = {
            "campaign": {
                "cid": "13647396",
                "isCampaign": true,
                "mediaType": "text"
            },
            "user": {
                "login": "shurupkirov",
                "script": "https://8208.beta1.direct.yandex.ru/registered/main.zQ7ppWga1IOa5i3p.pl"
            },
            "client": { "can_use_day_budget": 1 },
            "wallet": { "cid": "0" },
            "warning": {
                "value": "45",
                "sendWarn": 1,
                "interval": "60",
                "isPaused": true
            },
            "servicing": {
                "ManagerUID": "287450795",
                "request": "",
                "sendAccNews": 1,
                "offlineStatNotice": true
            },
            "validEmails": [
                {
                    "email": "shurupkirov@yandex.ru",
                    "select": ""
                },
                {
                    "select": "",
                    "email": "marketrussia@mail.ru"
                },
                {
                    "select": "",
                    "email": "spm.2015@ya.ru"
                },
                {
                    "select": "",
                    "email": "kislicin@lotus.kirov.ru"
                },
                {
                    "select": "",
                    "email": "jan43go@yandex.ru"
                }
            ],
            "email": "marketrussia@mail.ru",
            "dmParams": {
                "name": "m-campaign",
                "id": "1"
            }
        },
        ctx = {
            block: 'b-campaign-email-notifications-adapter',
            notificationData: notificationsData
        },
        clock;

    beforeEach(function() {
        clock = sinon.useFakeTimers();
        BEM.MODEL.create({ name: 'm-campaign', id: 1 });
        block = u.createBlock(ctx);

     });

    afterEach(function() {
        // должны отработать afterCurrentEvent
        clock.tick(1);
        block.destruct();
        clock.restore();
    });

    describe('Обновления на сохранении', function() {
        beforeEach(function() {
            block.findBlockInside('button').trigger('click');
        });

        it ('При сохранении обновляется хинт', function() {
            block._notificationBlock.trigger('save', {
                email: "spm.2015@ya.ru",
                sendWarn: 1,
                sendAccNews: 1,
                money_warning_value: 45,
                warnPlaceInterval: "60",
                offlineStatNotice: true,
                email_notify_paused_by_day_budget: true
            });

            expect(block.findBlockInside('hint-row').domElem.html().toString()).to.equal('spm.2015@ya.ru');
        });

        it ('При сохранении обновляется кнопка', function() {
            block._notificationBlock.trigger(
                'save', {
                    email: "",
                    sendWarn: 1,
                    sendAccNews: 1,
                    money_warning_value: 45,
                    warnPlaceInterval: "60",
                    offlineStatNotice: true,
                    email_notify_paused_by_day_budget: true
                });

            expect(block.findBlockInside('button').getText()).to.equal('Добавить');
        });

        it ('При сохранении обновляются скрытые поля', function() {
            block._notificationBlock.trigger('save', {
                email: "spm.2015@ya.ru",
                sendWarn: 0,
                sendAccNews: 1,
                money_warning_value: 25,
                warnPlaceInterval: "30",
                offlineStatNotice: 1,
                email_notify_paused_by_day_budget: 1
            });

            expect(block.elem('hidden-value').val()).to.equal('25');
            expect(block.elem('hidden-email').val()).to.equal('spm.2015@ya.ru');
            expect(block.elem('hidden-is-paused').val()).to.equal('1');
            expect(block.elem('hidden-send-news').val()).to.equal('1');
            expect(block.elem('hidden-offline-stat').val()).to.equal('1');
            expect(block.elem('hidden-send-warn').val()).to.equal('0');
            expect(block.elem('hidden-interval').val()).to.equal('30');

        });

        it ('При сохранении длинного email он отображается с троеточием', function() {
            block._notificationBlock.trigger('save', {
                email: 'veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryLongEmail@ya.ru',
                sendWarn: 1,
                sendAccNews: 1,
                money_warning_value: 45,
                warnPlaceInterval: '60',
                offlineStatNotice: true,
                email_notify_paused_by_day_budget: 1
            });

            expect(block.findBlockInside('hint-row').domElem.html().toString()).to.equal('veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryvery…');
        });

        it ('При сохранении длинного email у элемента появляется title с полным email', function() {
            block._notificationBlock.trigger('save', {
                email: 'veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryLongEmail@ya.ru',
                sendWarn: 1,
                sendAccNews: 1,
                money_warning_value: 45,
                warnPlaceInterval: '60',
                offlineStatNotice: true,
                email_notify_paused_by_day_budget: 1
            });

            expect(block.findBlockInside('hint-row').domElem.attr('title')).to.equal('veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryLongEmail@ya.ru');
        });
    });
});
