import Tld from '../../interfaces/Tld';
import Lang from '../../interfaces/Lang';
import InformerColor from '../../interfaces/components/informer/InformerColor';
import InformerTheme from '../../interfaces/components/informer/InformerTheme';
import StationEventList from '../../interfaces/state/station/StationEventList';
import StationInformerType from '../../interfaces/components/informer/StationInformerType';

import getStationInformerUrl from '../url/getStationInformerUrl';

const id = 213;
const tld = Tld.ru;
const language = Lang.ru;

const size = 5;
const color = InformerColor.default;
const colorId = 1;
const theme = InformerTheme.black;
const type = StationInformerType.plane;

describe('getStationInformerUrl', () => {
    it('Вернет урл если переданы только обязательные параметры', () => {
        expect(getStationInformerUrl({id, tld, language})).toBe(
            `/informers/station/${id}`,
        );
    });

    it('Вернет урл если переданы несколько необязательных параметров', () => {
        expect(getStationInformerUrl({id, tld, language, size, type})).toBe(
            `/informers/station/${id}?size=${size}&type=${type}`,
        );
    });

    it('Если передан event = departure, вернет урл без добавления параметра event', () => {
        expect(
            getStationInformerUrl({
                id,
                tld,
                language,
                size,
                color,
                theme,
                type,
                event: StationEventList.departure,
            }),
        ).toBe(
            `/informers/station/${id}?color=${colorId}&size=${size}&theme=${theme}&type=${type}`,
        );
    });

    it('Если передан event = arrival, вернет урл с добавлением параметра event', () => {
        expect(
            getStationInformerUrl({
                id,
                tld,
                language,
                size,
                color,
                theme,
                type,
                event: StationEventList.arrival,
            }),
        ).toBe(
            `/informers/station/${id}?color=${colorId}&event=${StationEventList.arrival}&size=${size}&theme=${theme}&type=${type}`,
        );
    });
});
