import {FilterTransportType} from '../../../transportType';
import IStatePage from '../../../../interfaces/state/IStatePage';
import IStateSearchForm from '../../../../interfaces/state/IStateSearchForm';
import Tld from '../../../../interfaces/Tld';
import Lang from '../../../../interfaces/Lang';
import DateSpecialValue from '../../../../interfaces/date/DateSpecialValue';

import getAllDaySearchUrlToRedirect from '../../searchRedirect';

const page = {
    location: {
        query: {
            fromId: 'c213',
            fromName: 'Москва',
            toId: 'c2',
            toName: 'Санкт-Петербург',
            when: '20+апреля',
            trainTariffClass: 'suite',
        },
    },
} as unknown as IStatePage;

const searchForm = {
    from: {
        slug: 'moscow',
    },
    to: {
        slug: 'yekaterinburg',
    },
    when: {
        text: '2 апреля',
        date: '2018-04-02',
    },
    transportType: FilterTransportType.train,
    searchNext: false,
} as IStateSearchForm;

const tld = Tld.ru;
const language = Lang.ru;

describe('getAllDaySearchUrlToRedirect', () => {
    it('Поиск поездом на все дни', () => {
        expect(
            getAllDaySearchUrlToRedirect({
                searchForm: {
                    ...searchForm,
                    when: {
                        ...searchForm.when,
                        special: 'all-days',
                        text: 'на все дни',
                    },
                } as IStateSearchForm,
                page,
                tld,
                language,
            }),
        ).toBe('/train/moscow--yekaterinburg?trainTariffClass=suite');
    });

    it('Поиск самолетами на все дни', () => {
        expect(
            getAllDaySearchUrlToRedirect({
                searchForm: {
                    ...searchForm,
                    transportType: FilterTransportType.plane,
                    when: {
                        ...searchForm.when,
                        special: DateSpecialValue.allDays,
                        text: 'на все дни',
                    },
                },
                page,
                tld,
                language,
            }),
        ).toBe('/plane/moscow--yekaterinburg?trainTariffClass=suite');
    });

    it('Поиск любым транспортом на все дни', () => {
        expect(
            getAllDaySearchUrlToRedirect({
                searchForm: {
                    ...searchForm,
                    transportType: FilterTransportType.all,
                    when: {
                        ...searchForm.when,
                        special: DateSpecialValue.allDays,
                        text: 'на все дни',
                    },
                },
                page,
                tld,
                language,
            }),
        ).toBe('/all-transport/moscow--yekaterinburg?trainTariffClass=suite');
    });
});
