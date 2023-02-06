import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Boolean фильтра
 * @property {PageObject.FilterPanelExtend} this.filterPanel
 */

export default makeSuite('Открытые фильтры.', {
    feature: 'Открытые фильтры.',
    id: 'marketfront-1360',
    issue: 'MARKETVERSTKA-26105',
    params: {
        checkedFilterId: 'Id выбранного фильтра',
        checkedFilterValue: 'Значение выбранного фильтра',
        openedFilterIds: 'Id фильтров, которые должны быть открыты',
    },
    story: {
        'По умолчанию': {
            'открыты фильтры в соответствие с выбранными параметрами': makeCase({
                async test() {
                    const {checkedFilterId, checkedFilterValue, openedFilterIds} = this.params;
                    await this.filterPanel.isFilterValueChecked(checkedFilterId, checkedFilterValue)
                        .should.eventually.be.equal(true,
                            `У фильтра ${checkedFilterId} выбрано значение ${checkedFilterValue}`);

                    return Promise.all(
                        openedFilterIds.map(filterId =>
                            this.filterPanel.isFilterOpenedById(filterId)
                                .should.eventually.be.equal(true, `Фильтр ${filterId} должен быть открыт`)
                        )
                    );
                },
            }),
        },
    },
});
