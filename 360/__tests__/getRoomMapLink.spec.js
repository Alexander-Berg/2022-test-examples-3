import config from 'configs/config';

import getRoomMapLink from '../getRoomMapLink';

describe('roomCard/utils/getRoomMapLink', () => {
  test('должен возвращать mapUrl из параметров переданной переговорки', () => {
    const mapUrl = Symbol();
    const email = Symbol();
    const roomInfo = {
      email,
      mapUrl
    };

    expect(getRoomMapLink(roomInfo)).toBe(mapUrl);
  });

  test('должен вычислять ссылку на карту, если не передали mapUrl', () => {
    const conferenceRoomMapUrl = 'http://staff-map';
    const mapUrl = null;
    const email = 'res1@ya.ru';
    const roomInfo = {
      email,
      mapUrl
    };
    const expectedResult = 'http://staff-map/res1';

    sinon.stub(config.urls, 'conferenceRoomMap').value(conferenceRoomMapUrl);

    expect(getRoomMapLink(roomInfo)).toBe(expectedResult);
  });
});
