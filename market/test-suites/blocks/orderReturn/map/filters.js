import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import FilterMenuButton from '@self/root/src/components/Checkout/FilterMenuButton/__pageObject';
import PickupFiltersPopup from '@self/root/src/components/PickupFiltersPopup/__pageObject';

import {FILTERS_IDS} from '@self/root/src/entities/mapState/filter';

export default makeSuite('Фильтры', {
    issue: 'MARKETFRONT-47969',
    id: 'marketfront-4672',
    params: {
        outletCount: 'Первоначальное количество точек на карте',
    },
    story: {
        async beforeEach() {
            assert(this.params.outletCount, 'Param outletCount must be defined');

            await this.setPageObjects({
                returnMapFilterMenuButton: () => this.createPageObject(FilterMenuButton, {parent: this.returnsForm}),
                returnMapFilterPopup: () => this.createPageObject(PickupFiltersPopup),
            });

            await this.returnMapFilterMenuButton.isVisible()
                .should.eventually.be.equal(
                    true,
                    'Кнопка меню для фильтров должна быть видна'
                );

            await this.returnMapFilterMenuButton.click();
            await this.returnMapFilterPopup.waitForFiltersVisible()
                .should.eventually.be.equal(
                    true,
                    'Фильтры должны быть видны'
                );
        },

        'При применении фильтра, для которого есть подходящие точки,': {
            'количество точек на карте меняется': makeCase({
                async test() {
                    await this.returnMapFilterPopup.getApplyFiltersButtonText()
                        .should.eventually.include(
                            this.params.outletCount,
                            'Текст кнопки должен содержать корректное количество точек возврата'
                        );

                    const initialPlacemarkCount = await this.returnMap.getPlacemarkCount();

                    await this.returnMapFilterPopup.clickFilterById(FILTERS_IDS.FREE);
                    await this.returnMapFilterPopup.applyFilters();

                    const placemarkCountAfterFilter = await this.returnMap.getPlacemarkCount();

                    return this.expect(placemarkCountAfterFilter)
                        .to.not.be.equal(
                            initialPlacemarkCount,
                            'Количество точек возврата на карте должно отличаться от первоначального'
                        );
                },
            }),
        },

        'При выборе фильтра, для которого нет подходящих точек,': {
            'кнопка применения фильтров становится неактивной и содержит корректный текст': makeCase({
                async test() {
                    await this.returnMapFilterPopup.clickFilterById(FILTERS_IDS.AROUND_THE_CLOCK);
                    await this.returnMapFilterPopup.isApplyFiltersButtonClickable()
                        .should.eventually.be.equal(
                            false,
                            'Кнопка применения фильтров должна быть некликабельна'
                        );

                    return this.returnMapFilterPopup.getApplyFiltersButtonText()
                        .should.eventually.be.equal(
                            'Пунктов не найдено',
                            'Текст кнопки должен быть корректным'
                        );
                },
            }),
        },
    },
});
