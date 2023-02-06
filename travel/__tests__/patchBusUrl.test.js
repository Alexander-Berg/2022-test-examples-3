jest.disableAutomock();

import patchBusUrl from '../patchBusUrl';

const orderUrl = '//yandex.ru/bus/ride/?fromId=c213&toId=c2';
const parsedUrl = {
    pathname: '//yandex.ru/bus/ride/',
    query: {
        fromId: 'c213',
        toId: 'c2',
    },
};

describe('patchBusUrl', () => {
    it('Если тарифы не определены - ничего не вернем', () => {
        expect(patchBusUrl({})).toBeUndefined();
    });

    it('Если url не был распаршен, то не меняем его', () => {
        const tariffs = {
            classes: {
                bus: {
                    orderUrl,
                },
            },
        };

        expect(
            patchBusUrl({
                tariffs,
            }),
        ).toBe(tariffs);
    });

    it('Если урл был распаршен, то добавляем необходимые параметры', () => {
        expect(
            patchBusUrl({
                tariffs: {
                    classes: {
                        bus: {
                            orderUrl,
                            parsedUrl,
                        },
                    },
                },
            }),
        ).toEqual({
            classes: {
                bus: {
                    orderUrl:
                        '//yandex.ru/bus/ride/?fromId=c213&hide_station=1&toId=c2',
                    parsedUrl,
                },
            },
        });
    });
});
