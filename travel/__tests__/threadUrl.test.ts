import {momentTimezone as moment} from '../../../../reexports';

import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';
import DateRobot from '../../../interfaces/date/DateRobot';

import getThreadUrl from '../getThreadUrl';
import makeUrl from '../makeUrl';

jest.mock('../makeUrl');
(makeUrl as jest.Mock).mockImplementation(() => '');

const tld = Tld.ru;
const language = Lang.ru;
const departureFromMoment = moment.tz(
    '2017-12-13T03:20:00+05:00',
    'Asia/Yekaterinburg',
);
const baseParams = {
    tld,
    language,
    threadUid: '081I_4_2',
    stationFromId: 9607404,
    stationToId: 2000003,
    departureFromMoment,
};
const canonicalUid = 'T081I_4_2';
const baseMakeUrlParams = {
    station_from: 9607404,
    station_to: 2000003,
};
const departureFromString = '2017-12-13 03:20:00';
const basePath = `/thread/${baseParams.threadUid}`;
const canonicalPath = `/thread/${canonicalUid}`;

describe('threadUrl', () => {
    it('Должна вернуться ссылка с departure_from', () => {
        getThreadUrl(baseParams);
        expect(makeUrl).toBeCalledWith(basePath, tld, language, {
            ...baseMakeUrlParams,
            departure_from: departureFromString,
        });
    });

    it('Если не указана станция отправления, то не нужно добавлять departure_from', () => {
        getThreadUrl({
            ...baseParams,
            stationFromId: undefined,
        });
        expect(makeUrl).toBeCalledWith(basePath, tld, language, {
            station_to: baseMakeUrlParams.station_to,
            station_from: undefined,
        });
    });

    it('Должна вернуться ссылка с canonicalUid', () => {
        getThreadUrl({
            ...baseParams,
            canonicalUid,
        });
        expect(makeUrl).toBeCalledWith(canonicalPath, tld, language, {
            ...baseMakeUrlParams,
            departure_from: departureFromString,
        });
    });

    it('Должна вернуться ссылка с canonicalUid (случай отсутствия threadUid)', () => {
        getThreadUrl({
            ...baseParams,
            canonicalUid,
            threadUid: undefined,
        });
        expect(makeUrl).toBeCalledWith(canonicalPath, tld, language, {
            ...baseMakeUrlParams,
            departure_from: departureFromString,
        });
    });

    it('Если не указан departureMoment в ссылке не должно быть параметра departure_from', () => {
        getThreadUrl({
            ...baseParams,
            departureFromMoment: undefined,
        });
        expect(makeUrl).toBeCalledWith(
            basePath,
            tld,
            language,
            baseMakeUrlParams,
        );
    });

    it('Если указана таймзона, то она должен быть параметр timezone', () => {
        const timezone = 'Europe/Moscow';

        getThreadUrl({
            ...baseParams,
            timezone,
        });

        expect(makeUrl).toBeCalledWith(basePath, tld, language, {
            ...baseMakeUrlParams,
            departure_from: departureFromString,
            timezone,
        });
    });

    it('Если указан параметр departureFromDateRobot, то он должен быть использован вместо departureFromMoment', () => {
        getThreadUrl({
            ...baseParams,
            departureFromDateRobot: '2020-05-29' as DateRobot,
        });

        expect(makeUrl).toBeCalledWith(basePath, tld, language, {
            ...baseMakeUrlParams,
            departure_from: '2020-05-29',
        });
    });
});
