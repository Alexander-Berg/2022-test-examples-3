import { renderHook } from '@testing-library/react-hooks';

import { Condition } from '../../../types';
import useIsPinIntersected, { zoomPointsEqualFuzz } from './useIsPinIntersected';

const describeWrap = (run: jest.EmptyFunction) =>
    describe('components', () => {
        describe('Map', () => {
            describe('hooks', () => {
                describe('useIsPinIntersected', run);
            });
        });
    });

describeWrap(() => {
    it('false: Без репорта', async() => {
        const { result } = renderHook(() => useIsPinIntersected({
            coords: [1, 2],
            zoom: 2
        }));

        expect(result.current).toBeFalsy();
    });

    it('false: Репорт скрыт', async() => {
        const { result } = renderHook(() => useIsPinIntersected({
            pin: { coords: [1, 2], isShown: false, isNight: false, condition: Condition.Cloudy },
            coords: [1, 2],
            zoom: 2
        }));

        expect(result.current).toBeFalsy();
    });

    it('false: Репорт показан далеко от точки', async() => {
        const { result } = renderHook(() => useIsPinIntersected({
            pin: { coords: [10, 20], isShown: true, isNight: false, condition: Condition.Cloudy },
            coords: [1, 2],
            zoom: 2
        }));

        expect(result.current).toBeFalsy();
    });

    [
        {
            zoom: 2,
            diff: zoomPointsEqualFuzz(2)
        },
        {
            zoom: 5,
            diff: zoomPointsEqualFuzz(5)
        },
        {
            zoom: 15,
            diff: zoomPointsEqualFuzz(15)
        }
    ].map(({ zoom, diff }) => {
        it(`true: Репорт показан недалеко от точки, z=${zoom}`, async() => {
            const coords: [number, number] = [1, 2];
            // координаты в радиусе +-0.9 от размера фаззинга
            const pinCoords = coords.map(val => val + diff * (Math.random() > .5 ? -.9 : .9)) as [number, number];

            const { result } = renderHook(() => useIsPinIntersected({
                pin: { coords: pinCoords, isShown: true, isNight: false, condition: Condition.Cloudy },
                coords,
                zoom
            }));

            expect(result.current).toBeTruthy();
        });
    });
});
