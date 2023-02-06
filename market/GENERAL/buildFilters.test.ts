import { filterModels } from './buildFilters';
import { shopModel } from 'src/test/data';
import { UIAvailabilityStatuses } from 'src/utils/types';
import { ShopModelAvailability } from 'src/java/definitions';

describe('buildFilters', () => {
  test('filter models by availability = shopmodel without availability', () => {
    const models = [{ ...shopModel, availability: undefined }];
    expect(
      filterModels(models, {
        availability: UIAvailabilityStatuses.UNKNOWN,
      })
    ).toHaveLength(1);

    expect(filterModels([shopModel], { availability: UIAvailabilityStatuses.UNKNOWN })).toHaveLength(0);
  });

  test('filter models by availability = not delisted models', () => {
    const notDelisted = [
      { ...shopModel, availability: ShopModelAvailability.ACTIVE },
      { ...shopModel, availability: ShopModelAvailability.INACTIVE },
      { ...shopModel, availability: undefined },
    ];
    expect(
      filterModels(notDelisted, {
        availability: UIAvailabilityStatuses.NOT_DELISTED,
      })
    ).toHaveLength(3);
  });

  test('filter models by availability = delisted model', () => {
    const notDelisted = [{ ...shopModel, availability: ShopModelAvailability.DELISTED }];
    expect(
      filterModels(notDelisted, {
        availability: ShopModelAvailability.DELISTED,
      })
    ).toHaveLength(1);
  });
});
