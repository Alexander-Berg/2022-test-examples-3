const PO = require('./PO');

describe('Dispenser: Список заявок', function() {
    describe('Просмотр заявок', function() {
        it('1. Просмотр заявок', function() {
            return this.browser
                .openIntranetPage({
                    pathname: '/hardware',
                }, { user: 'robot-abc-002' })
                .waitForVisible(PO.list(), 5000)
                // список заявок
                .assertView('list', PO.list());
        });

        it('2. Просмотр заявок на английской версии', function() {
            return this.browser
                .openIntranetPage({
                    pathname: '/hardware',
                    query: { lang: 'en' },
                }, { user: 'robot-abc-002' })
                .waitForVisible(PO.list(), 5000)
                // список заявок
                .assertView('list-en', PO.list());
        });
    });

    describe('Изменение статуса', function() {
        it('1. Сменить статус первой заявки', function() {
            /**
             * Для того, чтобы переснять дампы этого теста, robot-abc-002 должен видеть заявку со статусом 'Черновик'
             * здесь https://abc.test.yandex-team.ru/hardware/?author=robot-abc-002&status=NEW
             * Создать такую заявку нужно от имени robot-abc-002 кнопкой 'Создать заявку'
             * здесь https://abc.test.yandex-team.ru/services/dispensertest2/hardware/
             * (чтобы иметь возможность создавать/отменять заявки нужно быть участником сервиса или участником
             * сервиса-родителя для сервиса, в котором есть необходимость работать с заявками)
             */
            return this.browser
                .openIntranetPage({
                    pathname: '/hardware',
                    query: {
                        // Сервис для автотестов
                        // у робота должна быть возможность менять статус
                        author: 'robot-abc-002',
                        status: 'NEW',
                    },
                }, { user: 'robot-abc-002' })
                .waitForVisible(PO.list(), 5000)
                .waitForVisible(PO.list.item1(), 5000)
                .getText(PO.list.item1.status()).then(
                    text => assert.strictEqual(text, 'Черновик'),
                )
                .click(PO.list.item1.actions.actionCancel())
                .waitUntil(() => (
                    this.browser.getText(PO.list.item1.status()).then(
                        text => (text === 'Отменена'),
                    )
                ));
        });
    });
});
