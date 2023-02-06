import {assert} from 'chai';
import {trip} from 'suites/trips';
import {uniq} from 'lodash';

import ETripActivityType from 'helpers/project/account/types/ETripActivityType';

import TestApp from 'helpers/project/TestApp';

describe(trip.name, () => {
    it('Работа фильтра по типу в блоке развлечений', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {
            accountApp,
            accountApp: {tripPage},
        } = app;

        await accountApp.useTripsApiMock();

        await tripPage.goToTrip();

        const {activitiesBlock} = tripPage;

        await activitiesBlock.scrollIntoView();

        const activeFilter =
            await activitiesBlock.activityTypeFilter.getActive();

        assert.equal(
            activeFilter && (await activeFilter.getText()),
            'Все',
            'По умолчанию должен быть выбран фильтр "Все"',
        );

        const visibleActivityTypes = uniq(
            await activitiesBlock.activities.map(activity =>
                activity.label.getText(),
            ),
        );

        assert.isTrue(
            visibleActivityTypes.length > 1,
            'Должны отображаться все виды активностей',
        );

        const filters =
            await activitiesBlock.activityTypeFilter.activityTypes.filter(
                async activityType => (await activityType.getText()) !== 'Все',
            );

        for (const filter of filters) {
            const filterName = await filter.getText();

            await filter.click();

            assert.isTrue(
                await filter.isChecked(),
                `Фильтр "${filterName}" должен быть активным`,
            );

            assert.isTrue(
                await activitiesBlock.activities.every(
                    async activity =>
                        (await activity.label.getText()) === filterName,
                ),
                `Все активности должны быть только типа "${filterName}"`,
            );
        }
    });

    it('Фильтр по типу в блоке развлечений, когда есть только один тип развлечений', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {
            accountApp,
            accountApp: {tripPage},
        } = app;

        await accountApp.useTripsApiMock({
            filterActivityTypes: [ETripActivityType.AFISHA],
        });

        await tripPage.goToTrip();

        const {activitiesBlock} = tripPage;

        await activitiesBlock.scrollIntoView();

        assert.isFalse(
            await activitiesBlock.activityTypeFilter.isDisplayed(),
            'Фильтры должны отсутствовать в блоке',
        );
    });
});
