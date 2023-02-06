import { getDatacampTimings } from '../timings';
import { DatacampOffer } from '../types';

const offer: DatacampOffer = {
  pictures: {
    market: {
      meta: { timestamp: { seconds: 1 } },
      product_pictures: [],
    },
  },
  content: {
    binding: {
      approved: {
        meta: {},
      },
    },
    status: {
      content_system_status: {
        meta: { timestamp: { seconds: 2 } },
      },
    },
  },
  tech_info: {
    last_parsing: {
      start_parsing: {
        seconds: 4444,
      },
    },
    last_mining: {
      start_mining: {},
    },
  },
};
describe('datacamp timings', () => {
  it('data without meta', () => {
    const data = getDatacampTimings({
      content: {
        binding: {
          approved: {},
        },
      },
    });
    expect(data).toEqual([]);
  });
  it('data without meta', () => {
    const data = getDatacampTimings(offer);
    expect(data).toHaveLength(3);
    expect(data[0].timestamp).toBe(1);
    expect(data[1].timestamp).toBe(2);
    expect(data[2].timestamp).toBe(4444);
  });
});
