import {cloneDeep, last} from 'lodash';

import {aviaUpdateAirportsViewData} from 'selectors/avia/search/filters/airportsFilter/updateViewData';
import {getInitialViewData} from 'selectors/avia/search/filters/airportsFilter/getInitialViewData';
import {IResultAviaVariant} from 'selectors/avia/utils/denormalization/variant';
import {AirportsFilterViewData} from 'selectors/avia/search/filters/airportsFilter';

import {aviaFilterBy} from 'projects/avia/lib/search/filters/filterVariants';

import {fromRoutePic} from '../__testUtils__/fromRoutePic';
import {fromFilterPic} from '../__testUtils__/fromFilterPic';
import {getStubVariant} from '../__testUtils__/getStubVariant';

function prepareViewData(
    filterPic: string,
    variantPics: string[],
    filteredByOthers: string[] = [],
): AirportsFilterViewData {
    const filterValue = fromFilterPic(filterPic);
    const variants = variantPics.map(buildVariant);
    const filtered = aviaFilterBy.airports(filterValue, variants);
    const filteredByOthersMap = filteredByOthers.reduce(
        (result, tag) => ({
            ...result,
            [tag]: true,
        }),
        {},
    );
    const viewData = getInitialViewData(variants);

    return aviaUpdateAirportsViewData(
        variants,
        filtered,
        filteredByOthersMap,
        filterValue,
        viewData,
    );
}

test('enreachViewData returns no disabled stations on empty filter', () => {
    const viewData = prepareViewData('|>||', [
        '100->200',
        '100->250',
        '150->250',
    ]);

    expect(viewData.disabledStations).toEqual([
        {
            arrival: {200: false, 250: false},
            departure: {100: false, 150: false},
            transfers: {},
        },
    ]);
});

test('enreachViewData returns one disabled departure station', () => {
    const viewData = prepareViewData('|>||200', [
        '100->200',
        '100->250',
        '150->250',
    ]);

    expect(viewData.disabledStations).toEqual([
        {
            arrival: {200: false, 250: false},
            departure: {100: false, 150: true},
            transfers: {},
        },
    ]);
});

test('enreachViewData returns one disabled arrival station', () => {
    const viewData = prepareViewData('|>100||', [
        '100->200',
        '100->250',
        '150->350',
    ]);

    expect(viewData.disabledStations).toEqual([
        {
            arrival: {200: false, 250: false, 350: true},
            departure: {100: false, 150: false},
            transfers: {},
        },
    ]);
});

test('enreachViewData returns one disabled transfer station', () => {
    const viewData = prepareViewData('|>100||', [
        '100->200',
        '100->250',
        '150->300->250',
    ]);

    expect(viewData.disabledStations).toEqual([
        {
            arrival: {200: false, 250: false},
            departure: {100: false, 150: false},
            transfers: {300: true},
        },
    ]);
});

test('enreachViewData returns one disabled backward departure station', () => {
    const viewData = prepareViewData('|>100|| |>||', [
        '100->200, 200->100',
        '100->250, 250->100',
        '150->250, 350->100',
    ]);

    expect(viewData.disabledStations).toEqual([
        {
            arrival: {200: false, 250: false},
            departure: {100: false, 150: false},
            transfers: {},
        },
        {
            arrival: {100: false},
            departure: {200: false, 250: false, 350: true},
            transfers: {},
        },
    ]);
});

test('enreachViewData returns one disabled backward departure station with forward filter only', () => {
    const viewData = prepareViewData('|>100||', [
        '100->200, 200->100',
        '100->250, 250->100',
        '150->250, 350->100',
    ]);

    expect(viewData.disabledStations).toEqual([
        {
            arrival: {200: false, 250: false},
            departure: {100: false, 150: false},
            transfers: {},
        },
        {
            arrival: {100: false},
            departure: {200: false, 250: false, 350: true},
            transfers: {},
        },
    ]);
});

test('enreachViewData case with transfers and multisegments', () => {
    const viewData = prepareViewData('|>|| |>200||', [
        '100->200, 200->50->100',
        '100->250, 250->100',
        '150->250, 350->50->100',
    ]);

    expect(viewData.disabledStations).toEqual([
        {
            arrival: {200: false, 250: true},
            departure: {100: false, 150: true},
            transfers: {},
        },
        {
            arrival: {100: false},
            departure: {200: false, 250: false, 350: false},
            transfers: {50: false},
        },
    ]);
});

test('enreachViewData disables variants filtered by other filters', () => {
    const viewData = prepareViewData(
        '|>|| |>||',
        [
            '100->200, 200->50->100',
            '100->250, 250->100',
            '150->250, 350->50->100',
        ],
        ['100,200;200,50,100'],
    );

    expect(viewData.disabledStations).toEqual([
        {
            departure: {100: false, 150: false},
            arrival: {200: true, 250: false},
            transfers: {},
        },
        {
            departure: {200: true, 250: false, 350: false},
            arrival: {100: false},
            transfers: {50: false},
        },
    ]);
});

test('enreachViewData does not disables variants filtered by other filters if stations still available', () => {
    const viewData = prepareViewData(
        '|>|| |>||',
        [
            '100->200, 200->50->100', // x
            '100->200, 200->55->100', // o
            '100->250, 250->100', // x
        ],
        ['100,200;200,50,100', '100,250;250,100'],
    );

    expect(viewData.disabledStations).toEqual([
        {
            departure: {100: false},
            arrival: {200: false, 250: true},
            transfers: {},
        },
        {
            departure: {200: false, 250: true},
            arrival: {100: false},
            transfers: {50: true, 55: false},
        },
    ]);
});

function buildVariant(routePic: string): IResultAviaVariant {
    const stubVariant = getStubVariant();
    const stubFlight = stubVariant.route[0][0];
    const result = cloneDeep(stubVariant);
    const segments = fromRoutePic(routePic);

    result.tag = segments
        .map(segment =>
            [segment.departure, ...segment.transfers, segment.arrival].join(
                ',',
            ),
        )
        .join(';');
    result.key = result.tag;
    result.route = [];
    segments.forEach((segment, segmentIdx) => {
        if (segment.transfers.length === 0) {
            result.route[segmentIdx] = [cloneDeep(stubFlight)];
            result.route[segmentIdx][0].from.id = segment.departure;
            result.route[segmentIdx][0].to.id = segment.arrival;
        } else {
            result.route[segmentIdx] = [];

            for (let i = 0; i < segment.transfers.length; i++) {
                const from =
                    i === 0 ? segment.departure : segment.transfers[i - 1];
                const to = segment.transfers[i];

                result.route[segmentIdx][i] = cloneDeep(stubFlight);
                result.route[segmentIdx][i].from.id = from;
                result.route[segmentIdx][i].to.id = to;
            }

            result.route[segmentIdx][segment.transfers.length] =
                cloneDeep(stubFlight);
            result.route[segmentIdx][segment.transfers.length].from.id = last(
                segment.transfers,
            )!;
            result.route[segmentIdx][segment.transfers.length].to.id =
                segment.arrival;
        }
    });

    return result;
}
