import {assert} from 'chai';
import moment from 'moment';
import {serp} from 'suites/avia';

import dateFormats from 'helpers/utilities/date/formats';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';

describe(serp.name, () => {
    it('Проверка фильтра Авиакомпаний', async function () {
        const testApp = new TestAviaApp(this.browser);

        await testApp.goToSearchPage({
            from: {name: 'Екатеринбург', id: 'c54'},
            to: {name: 'Санкт-Петербург', id: 'c2'},
            startDate: moment().add(1, 'months').format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
        });

        const searchPage = testApp.searchDesktopPage;

        await searchPage.waitForSearchComplete();

        assert(
            await searchPage.filters.companies.isDisplayed(),
            'Должен отображаться фильтр "Авиакомпании"',
        );

        assert(
            !(await searchPage.filters.reset.isDisplayed()),
            'Должна отсутствовать кнопка Сбросить',
        );

        await searchPage.filters.companies.click();

        assert(
            await searchPage.filters.companies.allAlliences.isVisible(),
            'Должен отображаться чекбокс "Комбинации авиакомпаний"',
        );

        assert(
            await searchPage.filters.companies.allAlliences.isVisible(),
            'Должен отображаться блок выбора альянсов',
        );

        assert.isTrue(
            await searchPage.filters.companies.allAlliences.isChecked(),
            'Должно быть выбрано выбрано "Все альянсы"',
        );

        const aviaCompanyOne =
            await searchPage.filters.companies.selectCompany.find(
                async company => {
                    return (
                        !(await company.isDisabled()) &&
                        !(await company.isChecked())
                    );
                },
            );

        if (!aviaCompanyOne) {
            throw new Error(
                'Нет первой доступной для выбора авиакомпании в фильтре авиакомпаний',
            );
        }

        await aviaCompanyOne.click();

        assert.isTrue(
            await aviaCompanyOne.isChecked(),
            'Должна быть выбрана авиакомпания в фильтре',
        );

        await searchPage.fog.waitUntilProcessed();

        assert(
            await searchPage.filters.reset.isVisible(),
            'Должна быть кнопка сброса фильтров',
        );

        let variants = await searchPage.variants.items;

        for (const variant of variants) {
            assert.isTrue(
                await variant.checkAviacompanyInForwardFlight([
                    await aviaCompanyOne.title.getText(),
                ]),
                'Название выбранной авиакомпании в фильтре должно соответствовать той, что изображена на снипете',
            );
        }

        const aviaCompanyTwo =
            await searchPage.filters.companies.selectCompany.find(
                async company => {
                    return (
                        !(await company.isDisabled()) &&
                        !(await company.isChecked())
                    );
                },
            );

        if (!aviaCompanyTwo) {
            throw new Error(
                'Нет второй доступной для выбора авиакомпании в фильтре авиакомпаний',
            );
        }

        await aviaCompanyTwo.click();

        assert.isTrue(
            await aviaCompanyTwo.isChecked(),
            'Должно быть выбрано две авиакомпании в фильтре',
        );

        await searchPage.fog.waitUntilProcessed();

        assert(
            await searchPage.filters.reset.isVisible(),
            'Должна присутствовать кнопка сброса фильтров',
        );

        const aviaCompanies = [
            await aviaCompanyOne.title.getText(),
            await aviaCompanyTwo.title.getText(),
        ];

        variants = await searchPage.variants.items;

        for (const variant of variants) {
            assert.isTrue(
                await variant.checkAviacompanyInForwardFlight(aviaCompanies),
                'Авиакомпании в сниппете должны совпадать с теми, что были выбраны в фильтре',
            );
        }
    });
});
