specs({
    feature: 'LcColumnsGrid',
}, () => {
    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Одна колонка', function() {
        return this.browser
            .url('/turbo?stub=lccolumnsgrid/withOneColumn.json')
            .yaWaitForVisible(PO.lcColumnsGrid(), 'Блок не загрузился')
            .assertView('plain', PO.lcColumnsGrid())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Две колонки', function() {
        return this.browser
            .url('/turbo?stub=lccolumnsgrid/withTwoColumns.json')
            .yaWaitForVisible(PO.lcColumnsGrid(), 'Блок не загрузился')
            .assertView('plain', PO.lcColumnsGrid())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Три колонки', function() {
        return this.browser
            .url('/turbo?stub=lccolumnsgrid/wthThreeColumns.json')
            .yaWaitForVisible(PO.lcColumnsGrid(), 'Блок не загрузился')
            .assertView('plain', PO.lcColumnsGrid())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Четыре колонки', function() {
        return this.browser
            .url('/turbo?stub=lccolumnsgrid/withFourColumns.json')
            .yaWaitForVisible(PO.lcColumnsGrid(), 'Блок не загрузился')
            .assertView('plain', PO.lcColumnsGrid())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Один товар', function() {
        return this.browser
            .url('/turbo?stub=lccolumnsgrid/withOneGood.json')
            .yaWaitForVisible(PO.lcColumnsGrid(), 'Блок не загрузился')
            .assertView('plain', PO.lcColumnsGrid())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Одна висящая карточка', function() {
        return this.browser
            .url('/turbo?stub=lccolumnsgrid/withOneHangCard.json')
            .yaWaitForVisible(PO.lcColumnsGrid(), 'Блок не загрузился')
            .assertView('plain', PO.lcColumnsGrid())
            .yaCheckClientErrors();
    });
});
