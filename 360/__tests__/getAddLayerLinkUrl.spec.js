import config from 'configs/config';
import * as envConfig from 'configs/environment';

import getAddLayerLinkUrl from '../getAddLayerLinkUrl';

describe('getAddLayerLinkUrl', () => {
  const layerId = 'layer_id';
  const timezoneId = 'tz_id';
  const calendarUrl = 'calendar';
  const privateToken = 'private_token';
  const layerName = 'layer_name';

  beforeEach(() => {
    sinon.stub(config.date, 'timezoneId').value(timezoneId);
    sinon.stub(config.urls, 'calendar').value(calendarUrl);
    sinon.stub(envConfig, 'host').value(calendarUrl);
  });

  test('c private_token, корп', () => {
    sinon.stub(envConfig, 'isCorp').value(true);
    expect(getAddLayerLinkUrl(layerId, layerName, privateToken)).toBe(
      'calendar/?layer_ids=layer_id&layer_name=layer_name&modal=confirmSharing&private_token=private_token&tz_id=tz_id'
    );
  });

  test('c private_token, паблик', () => {
    sinon.stub(envConfig, 'isCorp').value(false);
    expect(getAddLayerLinkUrl(layerId, layerName, privateToken)).toBe(
      // eslint-disable-next-line
      'calendar/?ics=calendar%2Fexport%2Fics.xml%3Fprivate_token%3Dprivate_token%26tz_id%3Dtz_id&layer_name=layer_name&modal=confirmFeeding'
    );
  });

  test('без private_token', () => {
    expect(getAddLayerLinkUrl(layerId, layerName)).toBe(
      'calendar/?layer_ids=layer_id&layer_name=layer_name&modal=confirmSharing&tz_id=tz_id'
    );
  });
});
