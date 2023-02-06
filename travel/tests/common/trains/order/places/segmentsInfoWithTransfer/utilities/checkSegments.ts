import {Moment} from 'moment';
import {assert} from 'chai';

import {TestTrainsOrderPlacesStepPage} from 'helpers/project/trains/pages/TestTrainsOrderPlacesStepPage/TestTrainsOrderPlacesStepPage';

export default async function checkSegments({
    fromName,
    toName,
    orderPlacesStepPage,
    firstSegmentDepartureMoment,
}: {
    fromName: string;
    toName: string;
    orderPlacesStepPage: TestTrainsOrderPlacesStepPage;
    firstSegmentDepartureMoment: Moment;
}): Promise<void> {
    const {orderSegments} = orderPlacesStepPage;

    const [departure, arrival] =
        await orderSegments.title.getDepartureAndArrival();

    assert.equal(
        departure,
        fromName,
        'Должен быть указан верный начальный пункт отправления всего маршрута',
    );
    assert.equal(
        arrival,
        toName,
        'Должен быть указан верный конечный пункт прибытия всего маршрута',
    );

    const date = await orderSegments.title.departure.getText();

    assert.equal(
        date,
        firstSegmentDepartureMoment.format('D MMMM, dddd'),
        'Должная быть указана верная дата отправления',
    );

    const {segments} = orderSegments;

    assert.equal((await segments.items).length, 2, 'Должно быть 2 сегмента');

    await segments.every(async (segment, index) => {
        const info = await segment.getInfo();

        if (!orderPlacesStepPage.isTouch) {
            assert.isNotEmpty(
                info.company,
                `У ${index} сегмента должен быть указан перевозчик`,
            );
        }

        assert.isNotEmpty(
            info.number,
            `У ${index} сегмента должен быть указан номер поезда`,
        );
        assert.isNotEmpty(
            info.departureDate,
            `У ${index} сегмента должна быть указана дата отправления`,
        );
        assert.isNotEmpty(
            info.departureTime,
            `У ${index} сегмента должно быть указано время отправления`,
        );
        assert.isNotEmpty(
            info.arrivalDate,
            `У ${index} сегмента должна быть указана дата прибытия`,
        );
        assert.isNotEmpty(
            info.arrivalTime,
            `У ${index} сегмента должно быть указано время прибытия`,
        );
        assert.isNotEmpty(
            info.duration,
            `У ${index} сегмента должна быть указана длительность поездки`,
        );
        assert.equal(
            info.timeMessage,
            'Время местное',
            `У ${index} сегмента должна быть приписка "Время местное"`,
        );

        if (index === 0) {
            assert.equal(
                info.departureCity,
                fromName,
                'Пункт отправления первого сегмента должен совпадать с пунктом отправления всего маршрута',
            );
            assert.isNotEmpty(
                info.arrivalCity,
                `У ${index} сегмента должен быть указан пункт прибытия`,
            );
        } else {
            assert.isNotEmpty(
                info.departureCity,
                `У ${index} сегмента должен быть указан пункт отправления`,
            );
            assert.equal(
                info.arrivalCity,
                toName,
                'Пункт прибытия второго сегмента должен совпадать с пунктом прибытия всего маршрута',
            );
        }

        return true;
    });

    assert.isTrue(
        await orderSegments.transfer.isDisplayed(),
        'Должна отображаться информация о пересадке',
    );
}
