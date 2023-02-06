const _ = require('lodash/fp');
const getHorizontal = _.pipe(_.filter({ blockId: 'R-A-0001-01' }), _.get('0'));
const getVertical = _.pipe(_.filter({ blockId: 'R-A-0001-02' }), _.get('0'));

specs({
    feature: 'advert-react',
}, () => {
    describe('partner', function() {
        it('Для горизонтальной рекламы должен добавляться параметр выравнивания', function() {
            return this.browser
                .url('?stub=advert/partner-params.json&hermione_advert=stub&exp_flags=adv-disabled=0&exp_flags=force-react-advert=1&load-react-advert-script')
                .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
                .yaWaitForVisible(PO.reactAdvertItemRendered(), 'Реклама не загрузилась')
                .execute(() => window.AdvertCalls)
                .then(_.get('value.partners'))
                .then(calls => {
                    assert.notEqual(getHorizontal(calls), undefined, 'Не найден вызов рекламного блока R-A-0001-01');
                    assert.equal(getHorizontal(calls).horizontalAlign, 1, 'Для вызова блока не добавлен параметр horizontalAlign');

                    assert.notEqual(getVertical(calls), undefined, 'Не найден вызов рекламного блока R-A-0001-02');
                    assert.equal(getVertical(calls).horizontalAlign, undefined, 'Для вызова блока добавлен параметр horizontalAlign');
                })
                .yaCheckClientErrors();
        });
    });
});
