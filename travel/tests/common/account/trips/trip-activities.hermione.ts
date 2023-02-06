import {assert} from 'chai';
import {trip} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';

describe(trip.name, () => {
    it('Блок развлечений на странице моей поездки', async function () {
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

        assert.isTrue(
            await activitiesBlock.title.isVisible(),
            'У блока есть заголовок "Чем заняться"',
        );

        await activitiesBlock.activities.forEach(
            async (activity, activityIndex) => {
                const row = Math.floor(activityIndex / 2) + 1;
                const column = (activityIndex % 2) + 1;
                const activityPrefix = `У ${row}й карточки в ${column}м столбце`;

                assert.isTrue(
                    await activity.title.isVisible(),
                    `${activityPrefix} есть заголовок`,
                );
                assert.isTrue(
                    await activity.image.isVisible(),
                    `${activityPrefix} есть картинка`,
                );
                assert.isTrue(
                    await activity.hasCorrectLink(),
                    `${activityPrefix} ссылка ведет на "https://izi.travel" или "https://afisha.tst.yandex.ru"`,
                );
            },
        );
    });
});
