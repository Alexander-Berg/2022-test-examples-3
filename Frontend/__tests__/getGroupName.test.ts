import { getGroupName } from 'sport/lib/getGroupName';

test('getGroupName', () => {
  const baseGroupNameList = [
    'ГруппаА',
    'группа_А',
    'Group  А',
    ' группа А ',
    'Груп паА',
    'Zapad',
    'А',
    'Ё1',
    'Vostok2',
    '1.1',
    '2b',
  ];

  const resultGroupNameList = [
    'Группа А',
    'Группа А',
    'Группа А',
    'Группа А',
    'Груп паА',
    'Zapad',
    'Группа А',
    'Группа Ё1',
    'Vostok2',
    'Группа 1.1',
    '2b',
  ];

  expect(baseGroupNameList.map(getGroupName)).toMatchObject(resultGroupNameList);
});
