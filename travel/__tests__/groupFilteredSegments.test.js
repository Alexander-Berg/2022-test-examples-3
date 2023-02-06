jest.disableAutomock();

import groupFilteredSegments from '../groupFilteredSegments';

const baseTestManagerFilter = {
    type: 'test',

    isAvailableForContext(context) {
        return context.type === this.type;
    },

    getFilteredSegmentIndices(value, segments) {
        return segments.map(segment => value === segment.subtypeCode);
    },
};

const testManagerFilterOptionsAsObject = {
    ...baseTestManagerFilter,

    getOptions(segments) {
        const result = [];
        const optionsObject = segments.reduce((options, segment) => {
            if (segment.subtypeCode && !options[segment.subtypeCode]) {
                options[segment.subtypeCode] = segment.subtypeTitle;
            }

            return options;
        }, {});

        for (const code in optionsObject) {
            if (optionsObject.hasOwnProperty(code)) {
                result.push({value: code, text: optionsObject[code]});
            }
        }

        return result;
    },
};

const testCodeToTitle = {
    last: 'Ласточка',
    sapsan: 'Сапсан',
};

const testManagerFilterOptionsAsString = {
    ...baseTestManagerFilter,

    getOptions(segments) {
        const result = [];
        const optionsObject = segments.reduce((options, segment) => {
            if (segment.subtypeCode && !options[segment.subtypeCode]) {
                options[segment.subtypeCode] = segment.subtypeName;
            }

            return options;
        }, {});

        for (const code in optionsObject) {
            if (optionsObject.hasOwnProperty(code)) {
                result.push(code);
            }
        }

        return result;
    },

    getOptionText(option) {
        return testCodeToTitle[option];
    },
};

const testContext = {
    type: 'test',
};

const badContext = {};

const lastochkaSegment = {
    name: 'Москва - Екб',
    subtypeCode: 'last',
    subtypeTitle: 'Ласточка',
};
const sapsanSegment = {
    name: 'Москва - Екб',
    subtypeCode: 'sapsan',
    subtypeTitle: 'Сапсан',
};
const normalSegment = {name: 'Москва - Екб'};

const groupedFilteredSegments = [
    {
        title: 'Ласточка',
        segments: [lastochkaSegment],
        key: 'last',
    },
    {
        title: 'Сапсан',
        segments: [sapsanSegment],
        key: 'sapsan',
    },
];

describe('groupFilteredSegments', () => {
    it('Если filterManager не подходит для контекста, возвращает пустой массив', () => {
        expect(
            groupFilteredSegments({
                filterManager: baseTestManagerFilter,
                context: badContext,
            }),
        ).toEqual([]);
    });

    it('Если filterManager возвращает опции как строки: вернет массив объектов подтипов.', () => {
        expect(
            groupFilteredSegments({
                filterManager: testManagerFilterOptionsAsString,
                segments: [lastochkaSegment, sapsanSegment, normalSegment],
                context: testContext,
            }),
        ).toEqual(groupedFilteredSegments);
    });

    it('Если filterManager возвращает опции как объекты: вернет массив объектов подтипов.', () => {
        expect(
            groupFilteredSegments({
                filterManager: testManagerFilterOptionsAsObject,
                segments: [lastochkaSegment, sapsanSegment, normalSegment],
                context: testContext,
            }),
        ).toEqual(groupedFilteredSegments);
    });
});
