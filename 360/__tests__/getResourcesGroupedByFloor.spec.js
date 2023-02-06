import getResourcesGroupedByFloor from '../getResourcesGroupedByFloor';

describe('suggestMeetingTimes/utils/getResourcesGroupedByFloor', () => {
  test('должен вернуть список переговорок сгруппированный и отсортированный по этажам', () => {
    const resource1 = {id: 1, floor: 2};
    const resource2 = {id: 2, floor: 1};
    const resource3 = {id: 3, floor: 3};
    const resource4 = {id: 4, floor: 1};
    const resource5 = {id: 5, floor: 3};

    const resources = [resource1, resource2, resource3, resource4, resource5];
    const expectedResources = [
      {floor: 1, resources: [resource2, resource4]},
      {floor: 2, resources: [resource1]},
      {floor: 3, resources: [resource3, resource5]}
    ];

    expect(getResourcesGroupedByFloor(resources)).toEqual(expectedResources);
  });
});
