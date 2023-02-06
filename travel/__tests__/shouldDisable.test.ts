import {shouldDisable} from '../updateViewData';
import {fromRoutePic} from '../__testUtils__/fromRoutePic';
import {fromFilterPic} from '../__testUtils__/fromFilterPic';

const DEPARTURE = 'departure';
const ARRIVAL = 'arrival';
const TRANSFERS = 'transfers';

describe('shouldDisable', () => {
    test.each`
        segmentIdx | direction    | routePic                               | filterPic             | expected
        ${0}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>||200'}          | ${true}
        ${0}       | ${ARRIVAL}   | ${'100->300, 300->100'}                | ${'|>||200'}          | ${false}
        ${0}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>100||'}          | ${false}
        ${0}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>200||'}          | ${false}
        ${0}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>|| |>300||'}     | ${false}
        ${0}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>|| |>200||'}     | ${true}
        ${1}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>|| |>200||'}     | ${false}
        ${1}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>|| |>||200'}     | ${true}
        ${0}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>||'}             | ${false}
        ${0}       | ${DEPARTURE} | ${'100->300, 300->100'}                | ${'|>|200|'}          | ${false}
        ${0}       | ${DEPARTURE} | ${'100->400->300, 300->100'}           | ${'|>|200|'}          | ${true}
        ${0}       | ${DEPARTURE} | ${'100->400->500->300, 300->100'}      | ${'|>|200|'}          | ${true}
        ${0}       | ${DEPARTURE} | ${'100->400->500->300, 300->100'}      | ${'|>|200,500|'}      | ${true}
        ${1}       | ${DEPARTURE} | ${'100->400->500->300, 300->100'}      | ${'|>|| |>|200,500|'} | ${false}
        ${1}       | ${DEPARTURE} | ${'100->400->500->300, 300->500->100'} | ${'|>|| |>|200,500|'} | ${false}
        ${1}       | ${DEPARTURE} | ${'100->400->500->300, 300->400->100'} | ${'|>|| |>|200,500|'} | ${true}
        ${0}       | ${TRANSFERS} | ${'100->400->300, 300->100'}           | ${'|>|200|'}          | ${false}
    `(
        'shouldDisable returns $expected on [$routePic] with $segmentIdx-$direction and filter [$filterPic]',
        ({segmentIdx, direction, routePic, filterPic, expected}) => {
            expect(
                shouldDisable(
                    segmentIdx,
                    direction,
                    fromRoutePic(routePic),
                    fromFilterPic(filterPic),
                ),
            ).toBe(expected);
        },
    );
});
