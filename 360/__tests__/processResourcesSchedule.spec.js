import i18n from 'utils/i18n';
import processIntervals from 'features/timeline/utils/processIntervals';

import prepareOfficeResources from '../prepareOfficeResources';
import processResources from '../processResources';
import processResourcesSchedule from '../processResourcesSchedule';

const intervals = 'intervals';
const resources = 'resources';
const officeResources = 'officeResources';

jest.mock('utils/i18n');
jest.mock('features/timeline/utils/processIntervals');
jest.mock('../processResources');
jest.mock('../prepareOfficeResources');
processIntervals.mockReturnValue(intervals);
processResources.mockReturnValue(resources);
prepareOfficeResources.mockReturnValue(officeResources);

describe('processResourcesSchedule', () => {
  test('должен возвращать пустой массив, если в ответе api не было офисов', () => {
    const response = {};

    expect(processResourcesSchedule(response)).toEqual([]);
  });
  test('должен фильтровать переговорки по возможности бронирования', () => {
    const response = {
      offices: [
        {
          resources: [{id: 10, info: {canBook: false}}, {id: 20, info: {canBook: true}}]
        }
      ]
    };
    const event = {};
    const date = Date.now();

    processResourcesSchedule(response, event, date);

    expect(processResources).toBeCalledWith([{id: 20, info: {canBook: true}}], event);
  });
  test('не должен фильтровать переговорки по возможности бронирования, если shallFilterNotBookable === false', () => {
    const resources = [{id: 10, info: {canBook: false}}, {id: 20, info: {canBook: true}}];
    const response = {offices: [{resources}]};
    const event = {};
    const date = Date.now();

    processResourcesSchedule(response, event, date, {shallFilterNotBookable: false});

    expect(processResources).toBeCalledWith(resources, event);
  });
  test('должен вызывать processIntervals c результатом processResources', () => {
    const response = {
      offices: [{resources: [{id: 10, info: {canBook: false}}, {id: 20, info: {canBook: true}}]}]
    };
    const event = {};
    const date = Date.now();

    processResourcesSchedule(response, event, date);

    expect(processIntervals).toBeCalledWith(resources, date);
  });
  test('должен вызывать prepareOfficeResources c нужными параметрами', () => {
    const id = 777;
    const response = {
      offices: [
        {
          id,
          resources: [{id: 10, info: {canBook: false}}]
        }
      ]
    };
    const event = {};
    const date = Date.now();

    processResourcesSchedule(response, event, date);

    expect(prepareOfficeResources).toBeCalledWith([], intervals, id);
  });
  test('должен возвращать массив офисов с подготовленной структурой данных', () => {
    const id = 777;
    const response = {
      offices: [
        {
          id,
          resources: [{id: 10, info: {canBook: false}}]
        }
      ]
    };
    const event = {};
    const date = Date.now();

    processResourcesSchedule(response, event, date);

    expect(processResourcesSchedule(response, event, date)).toEqual([
      {
        title: i18n.get('event', 'resourcesInOffice', {name}),
        resources: officeResources
      }
    ]);
  });
});
