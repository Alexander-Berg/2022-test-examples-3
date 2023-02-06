const PO = require('./PO');

describe('Светофоры идеальности', () => {
    it('Внешний вид модалки идеальности', function() {
        return this.browser
            // открываем попап ресурса "TVM приложение" на просмотр секретов которого есть доступ
            .openIntranetPage({
                pathname: '/',
                query: { perfection_details: 3272 },
            }, { user: 'robot-abc-002' })
            .disableAnimations('*')

            // ждем, пока отрисуется контент модалки идеальности
            .waitForVisible(PO.perfectionModal.wikiText(), 5000)

            // внешний вид модалки идеальности
            .assertPopupView(PO.perfectionModal(), 'plain', PO.perfectionModal.wrapper(), {
                animationDisabled: true,
                redrawElements: ['.Modal_hasAnimation .Modal-Content'],
                redrawMode: 'hard',
            });
    });

    it('Внешний вид модалки идеальности на странице с компонентами Диспенсера', function() {
        return this.browser
            // открываем попап ресурса "TVM приложение" на просмотр секретов которого есть доступ
            .openIntranetPage({
                pathname: '/services/zanartestservice000/hardware/',
                query: { perfection_details: 3272 },
            }, { user: 'robot-abc-002' })
            .disableAnimations('*')

            // ждем, пока отрисуется контент модалки идеальности
            .waitForVisible(PO.perfectionModal.wikiText(), 5000)

            // внешний вид модалки идеальности на странице с потенциальными конфликтами стилей
            .assertPopupView(PO.perfectionModal(), 'plain_dispenser', PO.perfectionModal.wrapper(), {
                animationDisabled: true,
                redrawElements: ['.Modal_hasAnimation .Modal-Content'],
                redrawMode: 'hard',
            });
    });
});
