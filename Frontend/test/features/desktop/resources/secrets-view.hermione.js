describe('Отображение секретов', () => {
    it('Внешний вид', function() {
        return this.browser
            // открываем попап ресурса "TVM приложение" на просмотр секретов которого есть доступ
            .openIntranetPage({
                pathname: '/services/robotinternal003service/resources/',
                query: { 'show-resource': 4251208 },
            })
            .disableAnimations('*')

            // ждем, пока отрисуются секреты
            .waitForVisible('.abc-resource-view__attributes', 10000)
            .waitForVisible('.spin2', 10000, true)

            // секреты отображаются [plain]
            .assertView('plain', '.abc-resource-view__attributes', {
                animationDisabled: true,
                redrawElements: ['.modal_has-animation_yes', '.modal_has-animation_yes .modal__content'],
            });
    });
});
