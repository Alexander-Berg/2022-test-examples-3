export const tabs = {
  data: {
    tabs: [
      {
        tabId: 1,
        tabCaption: 'Кредиты',
        blocks: {
          '1': {
            componentType: 'Table',
            title: 'AccountCredits',
            url: '/v0/blocks/accountCredits?accountId=9955466',
          },
          '3': {
            componentType: 'Table',
            title: 'AccountDebts',
            url: '/v0/blocks/accountDebts?accountId=9955466',
          },
        },
        layout: {
          schema: { rows: [{ areaId: '1', blockWidthRatio: 1 }] },
          areaIdToBlocksIds: { '1': ['1', '3'] },
        },
      },
    ],
  },
};
