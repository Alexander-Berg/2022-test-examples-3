import {buildSpecial, buildWeekday, buildDate} from '../build';
import {updateTime} from '../update';
import {updateSpecialTime} from '../updateByType';

jest.mock('../update');
jest.mock('../updateByType');

const timezone = 'Asia/Yekaterinburg';
const language = 'uk';

const time = {
    now: '2016-02-17T03:45:00+05:00',
    timezone,
};

const expectedValue = {
    date: '2016-02-16',
};

// В тестах предполагаем, что text, hint введены на русском языке, но текущий язык интерфейса - украинский.
describe('build', () => {
    describe('buildSpecial', () => {
        it('build by text', () => {
            const text = 'вче';
            const hint = 'вчера';

            updateSpecialTime.mockReturnValue(expectedValue);

            expect(
                buildSpecial('yesterday', {
                    text,
                    hint,
                    time,
                    language,
                }),
            ).toBe(expectedValue);

            expect(updateSpecialTime).toBeCalledWith(
                {
                    text,
                    hint,
                    special: 'yesterday',
                    formatted: 'вчора',
                },
                time,
            );
        });

        it('build without text', () => {
            updateSpecialTime.mockReturnValue(expectedValue);

            expect(
                buildSpecial('yesterday', {
                    time,
                    language,
                }),
            ).toBe(expectedValue);

            expect(updateSpecialTime).toBeCalledWith(
                {
                    text: 'вчора',
                    hint: 'вчора',
                    special: 'yesterday',
                    formatted: 'вчора',
                },
                time,
            );
        });
    });

    describe('buildWeekday', () => {
        const tuesdayIndex = 1;

        it('build by text', () => {
            const text = 'вто';
            const hint = 'вторник';

            updateTime.mockReturnValue(expectedValue);

            expect(
                buildWeekday(tuesdayIndex, {
                    text,
                    hint,
                    time,
                    language,
                }),
            ).toBe(expectedValue);

            expect(updateTime).toBeCalledWith(
                {
                    text,
                    hint,
                    weekday: tuesdayIndex,
                    formatted: 'вівторок',
                },
                time,
            );
        });

        it('build without text', () => {
            updateTime.mockReturnValue(expectedValue);

            expect(
                buildWeekday(tuesdayIndex, {
                    time,
                    language,
                }),
            ).toBe(expectedValue);

            expect(updateTime).toBeCalledWith(
                {
                    text: 'вівторок',
                    hint: 'вівторок',
                    weekday: tuesdayIndex,
                    formatted: 'вівторок',
                },
                time,
            );
        });
    });

    describe('buildDate', () => {
        const dateString = '2016-02-16';

        it('build by text', () => {
            const text = '16 фев';
            const hint = '16 февраля';

            expect(
                buildDate(dateString, {
                    text,
                    hint,
                    language,
                }),
            ).toEqual({
                text,
                hint,
                formatted: '16 лютого',
                shortFormatted: '16 лют',
                date: dateString,
            });
        });

        it('build without text', () => {
            expect(
                buildDate(dateString, {
                    language,
                }),
            ).toEqual({
                text: '16 лютого',
                hint: '16 лютого',
                date: dateString,
                formatted: '16 лютого',
                shortFormatted: '16 лют',
            });
        });
    });
});
