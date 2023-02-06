import { testOffer } from 'src/shared/test-data/test-offers';
import { ModerationOffer } from 'src/tasks/mapping-moderation/helpers/moderation-types';

let nextId = 1;

export function testModerationOffer(init: Partial<ModerationOffer> = {}): ModerationOffer {
  const { id = nextId++, ...rest } = init;

  return {
    id,
    isAvailable: true,
    title: `Offer #${id}`,
    pictures: [],
    offer: testOffer({ id: `${id}` }),
    ...rest,
  };
}
