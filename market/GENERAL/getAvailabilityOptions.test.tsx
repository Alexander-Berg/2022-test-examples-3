import { ShopModelAvailability } from 'src/java/definitions';
import { shopModel } from 'src/test/data';
import { UIAvailabilityStatuses } from 'src/utils/types';
import { UNKNOWN_STATUS, UNKNOWN_STATUS_TITLE } from '../..';
import { UI_AVAILABILITY_STATUS } from '../../options';
import { getAvailabilityOptions } from './getAvailabilityOptions';

describe('useAvailabilityOptions', () => {
  test('ShopModel without availability', () => {
    const options = getAvailabilityOptions([{ ...shopModel, availability: undefined }]);
    expect(options).toContainEqual({ label: `${UNKNOWN_STATUS_TITLE} (1)`, value: UNKNOWN_STATUS });
    expect(options).toContainEqual({
      label: `${UI_AVAILABILITY_STATUS.NOT_DELISTED} (1)`,
      value: UIAvailabilityStatuses.NOT_DELISTED,
    });
  });

  test('ShopModel with availability = ShopModelAvailability.DELISTED', () => {
    const options = getAvailabilityOptions([{ ...shopModel, availability: ShopModelAvailability.DELISTED }]);
    expect(options).toContainEqual({
      label: `${UI_AVAILABILITY_STATUS[UIAvailabilityStatuses.NOT_DELISTED]} (0)`,
      value: UIAvailabilityStatuses.NOT_DELISTED,
    });
    expect(options).toContainEqual({
      label: `${UI_AVAILABILITY_STATUS.DELISTED} (1)`,
      value: ShopModelAvailability.DELISTED,
    });
  });

  test('ShopModels with availability = ShopModelAvailability.INACTIVE and ShopModelAvailability.ACTIVE', () => {
    const options = getAvailabilityOptions([
      { ...shopModel, availability: ShopModelAvailability.INACTIVE },
      { ...shopModel, availability: ShopModelAvailability.ACTIVE },
    ]);

    expect(options).toContainEqual({
      label: `${UI_AVAILABILITY_STATUS.ACTIVE} (1)`,
      value: ShopModelAvailability.ACTIVE,
    });

    expect(options).toContainEqual({
      label: `${UI_AVAILABILITY_STATUS.INACTIVE} (1)`,
      value: ShopModelAvailability.INACTIVE,
    });

    expect(options).toContainEqual({
      label: `${UI_AVAILABILITY_STATUS.NOT_DELISTED} (2)`,
      value: UIAvailabilityStatuses.NOT_DELISTED,
    });
  });
});
