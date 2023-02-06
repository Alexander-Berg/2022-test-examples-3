import prepareOfficeResources from '../prepareOfficeResources';

describe('prepareOfficeResources', () => {
  test('должен добавлять во все ресурсы дополнительные параметры', () => {
    const intervalsX = 'intervalsX';
    const intervalsY = 'intervalsY';
    const resources = [
      {
        info: {
          email: 'x@ya.ru'
        }
      },
      {
        info: {
          email: 'y@ya.ru'
        }
      }
    ];
    const intervals = {
      'x@ya.ru': intervalsX,
      'y@ya.ru': intervalsY
    };
    const officeId = 10;
    const expectedResult = [
      {
        email: 'x@ya.ru',
        officeId,
        canBook: true,
        isRoomResource: true,
        intervals: intervalsX
      },
      {
        email: 'y@ya.ru',
        officeId,
        canBook: true,
        isRoomResource: true,
        intervals: intervalsY
      }
    ];

    expect(prepareOfficeResources(resources, intervals, officeId)).toEqual(expectedResult);
  });
});
