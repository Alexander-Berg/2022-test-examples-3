import {OrderedMap} from 'immutable';

import OfficeRecord from '../OfficeRecord';
import groupOfficesByCity from '../utils/groupOfficesByCity';
import {
  getOffices,
  getAllOffices,
  getOfficesGroupedByCity,
  getAllOfficesGroupedByCity
} from '../officesSelectors';

jest.mock('../utils/groupOfficesByCity');

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
  name: '• БЦ Достык',
  cityName: 'Алматы'
});
const office4 = new OfficeRecord({
  id: 4,
  name: 'БЦ Строганов',
  cityName: 'Москва'
});

describe('officesSelectors', () => {
  describe('getAllOffices', () => {
    test('должен возвращать все офисы', () => {
      const state = {
        offices: new OrderedMap({
          [office1.id]: office1,
          [office2.id]: office2,
          [office3.id]: office3,
          [office4.id]: office4
        })
      };
      const expectedResult = new OrderedMap({
        [office1.id]: office1,
        [office2.id]: office2,
        [office3.id]: office3,
        [office4.id]: office4
      });

      expect(getAllOffices(state)).toEqual(expectedResult);
    });
  });
  describe('getOffices', () => {
    test('должен возвращать офисы, названия которых не начинаются с "•"', () => {
      const state = {
        offices: new OrderedMap({
          [office1.id]: office1,
          [office2.id]: office2,
          [office3.id]: office3,
          [office4.id]: office4
        })
      };
      const expectedResult = new OrderedMap({
        [office1.id]: office1,
        [office2.id]: office2,
        [office4.id]: office4
      });

      expect(getOffices(state)).toEqual(expectedResult);
    });
  });
  describe('getOfficesGroupedByCity', () => {
    test('должен прогонять результат getOffices через groupOfficesByCity', () => {
      groupOfficesByCity.mockReset();
      const offices = new OrderedMap({
        [office1.id]: office1,
        [office2.id]: office2,
        [office4.id]: office4
      });

      getOfficesGroupedByCity.resultFunc(offices);
      expect(groupOfficesByCity).toHaveBeenCalledTimes(1);
      expect(groupOfficesByCity).toHaveBeenCalledWith(offices);
    });
  });
  describe('getAllOfficesGroupedByCity', () => {
    test('должен прогонять результат getAllOffices через groupOfficesByCity', () => {
      groupOfficesByCity.mockReset();
      const offices = new OrderedMap({
        [office1.id]: office1,
        [office2.id]: office2,
        [office3.id]: office3,
        [office4.id]: office4
      });

      getAllOfficesGroupedByCity.resultFunc(offices);
      expect(groupOfficesByCity).toHaveBeenCalledTimes(1);
      expect(groupOfficesByCity).toHaveBeenCalledWith(offices);
    });
  });
});
