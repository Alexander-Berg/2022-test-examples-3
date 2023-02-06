import { squashStatuses } from './squashStatuses';

describe('squashStatuses', () => {
  it('works empty', () => {
    expect(squashStatuses([])).toEqual([]);
  });
  it('works', () => {
    expect(
      squashStatuses([
        {
          activeItems: 1,
          categoryId: 1,
          managerUid: 1,
          stuckOffers: [
            {
              offerId: 1,
              offerTitle: 'offer 1',
              shopSku: '1',
              supplierId: 1,
            },
          ],
          totalItems: 2,
        },
        {
          activeItems: 1,
          categoryId: 1,
          managerUid: 1,
          stuckOffers: [
            {
              offerId: 2,
              offerTitle: 'offer 2',
              shopSku: '2',
              supplierId: 2,
            },
          ],
          totalItems: 2,
        },
      ])
    ).toEqual([
      {
        activeItems: 2,
        categoryId: 0,
        managerUid: 1,
        stuckOffers: [
          {
            offerId: 1,
            offerTitle: 'offer 1',
            shopSku: '1',
            supplierId: 1,
          },
          {
            offerId: 2,
            offerTitle: 'offer 2',
            shopSku: '2',
            supplierId: 2,
          },
        ],
        totalItems: 4,
      },
    ]);
  });
});
