const patchedTariffs = {
    classes: {
        bus: {
            price: {
                value: 2100,
                currency: 'RUB',
            },
        },
    },
};

const patchTariffs = jest.fn(() => patchedTariffs);

jest.setMock('../../tariffs/patchTariffs', patchTariffs);

const patchBusUrl = jest.fn(tariffs => tariffs);

jest.setMock('../patchBusUrl', patchBusUrl);

const updateYBusSegment = require.requireActual('../updateYBusSegment').default;

const baseSegment = {
    number: '212',
    stationTo: {
        id: 919919,
    },
};

const updateSegment = {
    source: 'ybus',
    yBusId: '007',
    number: '313',
    stationTo: {
        id: 919919,
    },
    tariffs: {},
    useCityInsteadStationTo: true,
};

const meta = {
    context: {
        to: {
            key: 'c213',
        },
    },
};

describe('updateYBusSegment', () => {
    it('вернёт сегмент с дополнительной информацией из автобусного сегмента (станции не различаются)', () => {
        expect(updateYBusSegment(baseSegment, updateSegment, meta)).toEqual({
            ...baseSegment,
            source: 'ybus',
            yBusId: '007',
            number: '313',
            tariffs: patchedTariffs,
            useCityInsteadStationTo: true,
            stationsAreDifferent: false,
        });

        // Если станции прибытия не различаются - не патчим урл
        expect(patchBusUrl).not.toHaveBeenCalled();
    });

    it('вернёт сегмент с дополнительной информацией из автобусного сегмента (станции различаются)', () => {
        const busUpdateSegment = {
            ...updateSegment,
            stationTo: {
                id: 818818,
            },
        };

        expect(updateYBusSegment(baseSegment, busUpdateSegment, meta)).toEqual({
            ...baseSegment,
            source: 'ybus',
            yBusId: '007',
            number: '313',
            tariffs: patchedTariffs,
            useCityInsteadStationTo: true,
            stationsAreDifferent: true,
        });

        // Если станции прибытия различаются - патчим урл
        expect(patchBusUrl).toHaveBeenCalledWith(busUpdateSegment);
    });

    it('если в данных от автобусов не опередён номер - используем номер из нашей базы', () => {
        expect(
            updateYBusSegment(
                baseSegment,
                {
                    ...updateSegment,
                    number: null,
                },
                meta,
            ).number,
        ).toBe('212');
    });

    it('если станции различаются и точка прибытия - город, выставляем будем писать город вместо станции', () => {
        expect(
            updateYBusSegment(
                baseSegment,
                {
                    ...updateSegment,
                    stationTo: {
                        id: 818818,
                    },
                    useCityInsteadStationTo: false,
                },
                meta,
            ).useCityInsteadStationTo,
        ).toBe(true);
    });
});
