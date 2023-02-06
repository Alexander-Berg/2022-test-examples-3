specs({
    feature: 'CustomBlocks',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=customblock/default.json')
            .yaWaitForVisible(PO.customBlocks(), 'Кастомный блок не появился на странице')
            .assertView('plain', PO.hermioneContainer());
    });

    hermione.only.notIn('safari13');
    it('Проверка наличия отступов вокруг блока', function() {
        return this.browser
            .url('/turbo?stub=customblock/between-paragraphs.json')
            .yaWaitForVisible(PO.customBlocks(), 'Кастомный блок не появился на странице')
            .assertView('plain', PO.hermioneContainer());
    });
});
