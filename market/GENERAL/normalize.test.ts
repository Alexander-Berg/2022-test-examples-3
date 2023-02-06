import { getNormalizedParameterValueLinks } from './normalize';

describe('parameterValueLinks normalize', () => {
  it('works', () => {
    expect(
      getNormalizedParameterValueLinks([
        {
          param_id: '7893318',
          linked_param_id: '12782797',
          type: 'DIRECT',
          linked_value: [
            {
              option_id: '152705',
              linked_option_id: ['13730016', '13730017', '13730018'],
            },
            {
              option_id: '10706215',
              linked_option_id: ['26031910'],
            },
            {
              option_id: '676111',
              linked_option_id: ['18264090', '18264110', '18264111'],
            },
          ],
        },
      ] as any)
    ).toEqual([
      {
        parameterId: 7893318,
        type: 'DIRECT',
        linkedParamId: 12782797,
        linkedValue: [
          { linkedOptionIds: [13730016, 13730017, 13730018], optionId: 152705 },
          { linkedOptionIds: [26031910], optionId: 10706215 },
          { linkedOptionIds: [18264090, 18264110, 18264111], optionId: 676111 },
        ],
      },
    ]);
  });
});
