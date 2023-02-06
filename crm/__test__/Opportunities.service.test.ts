import { OpportunitiesServiceImpl } from '../Opportunities.service';

const opportunities = [
  {
    id: 2,
    name: 'name',
  },
];

const opportunitiesDTO = [
  {
    id: '2',
    name: 'name',
  },
];

describe('OpportunitiesServiceImpl', () => {
  it('sets opportunities', () => {
    const opportunitiesService = new OpportunitiesServiceImpl(1);
    opportunitiesService.setOpportunities(opportunities);

    expect(opportunitiesService.opportunities).toStrictEqual(opportunitiesDTO);
  });
});
