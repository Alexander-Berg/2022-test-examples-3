import {OrderedMap} from 'immutable';

import groupOfficesByCity from '../groupOfficesByCity';
import OfficeRecord from '../../OfficeRecord';

describe('groupOfficesByCity', () => {
  test('должен вернуть пустой массив, если офисов нет', () => {
    const offices = new OrderedMap();

    expect(groupOfficesByCity(offices)).toEqual([]);
  });

  test('должен вернуть офисы сгруппированные по названию города, если они есть', () => {
    const office1 = new OfficeRecord({
      id: 1,
      name: 'БЦ Бенуа',
      cityName: 'Санкт-Петербург'
    });
    const office2 = new OfficeRecord({
      id: 2,
      name: 'БЦ Морозов',
      cityName: 'Москва'
    });
    const office3 = new OfficeRecord({
      id: 3,
      name: 'БЦ Достык',
      cityName: 'Алматы'
    });
    const office4 = new OfficeRecord({
      id: 4,
      name: 'БЦ Строганов',
      cityName: 'Москва'
    });

    const offices = new OrderedMap({
      [office1.id]: office1,
      [office2.id]: office2,
      [office3.id]: office3,
      [office4.id]: office4
    });
    const expectedResult = [
      {cityName: office1.cityName, offices: [office1]},
      {cityName: office2.cityName, offices: [office2, office4]},
      {cityName: office3.cityName, offices: [office3]}
    ];

    expect(groupOfficesByCity(offices)).toEqual(expectedResult);
  });
});
