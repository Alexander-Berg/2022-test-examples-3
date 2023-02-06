import {assert} from 'chai';
import {order} from 'suites/trains';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

const {steps} = order;

describe(steps.places, () => {
    it('Отображение информации в блоке "Выберите тип вагона и место"', async function () {
        const app = new TestTrainsApp(this.browser);
        const {orderPlacesStepPage} = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        const coachTypeTabButtons = await app.orderPlacesStepPage
            .coachTypeTabsSelector.tabButtons;

        assert.isAbove(
            await coachTypeTabButtons.count(),
            1,
            'Должно быть более 1 табика с типом вагона',
        );

        await coachTypeTabButtons.forEach(async coachTypeTabButton => {
            await coachTypeTabButton.setActive();

            const title = await coachTypeTabButton.title.getText();

            assert.isNotEmpty(
                title,
                'Заголовок у табика с типом вагона должен быть не пуст',
            );

            const firstCoach =
                await app.orderPlacesStepPage.trainCoaches.coaches.first();

            if (!firstCoach) {
                throw new Error(`Не найден вагон с типом: ${title}`);
            }

            await app.orderPlacesStepPage.trainCoaches.coaches.forEach(
                async coach => {
                    const coachNumber =
                        await coach.coachHeader.getCoachNumber();

                    assert.isNumber(
                        coachNumber,
                        'Должен отображаться номер вагона',
                    );

                    const availablePlaces =
                        await coach.transportSchema.getAvailablePlaces();
                    const countAvailablePlaces = availablePlaces.length;

                    if (!countAvailablePlaces) {
                        assert.isTrue(
                            await coach.autoSeat.label.isDisplayed(),
                            'Должен отображаться лейбл об автоматическом выборе мест для вагона без схемы',
                        );

                        assert.isTrue(
                            await coach.autoSeat.button.isDisplayed(),
                            'Должна отображаться кнопка об автоматическом выборе мест для вагона без схемы',
                        );
                    }
                },
            );
        });
    });
});
