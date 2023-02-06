import { DEFAULT_CATEGORY_ID } from 'src/shared/test-data/test-constants';
import { testOffer } from 'src/shared/test-data/test-offers';
import { OfferInput } from 'src/tasks/mapping-moderation/helpers/input-output';

let nextId = 1;

export function testModerationOfferInput(init: Partial<OfferInput> = {}): OfferInput {
  const { id = nextId++, ...rest } = init;

  const offer = testOffer({ id: `${id}` });

  return {
    id: offer.offer_id!,
    offer_id: offer.offer_id!,
    category_name: 'category_name',
    category_id: String(DEFAULT_CATEGORY_ID),
    ...rest,
  };
}
