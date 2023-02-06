import { OpportunitiesService, OpportunityDTO } from '../Opportunities.types';

export class OpportunitiesServiceStub implements OpportunitiesService {
  opportunities: OpportunityDTO[] = [];
  save() {}
}
