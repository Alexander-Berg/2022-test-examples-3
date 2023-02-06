import { ReactWrapper } from 'enzyme';

import { setupTestApp } from 'src/test/setupApp';
import { Button } from 'src/components';
import { ReplenishmentFooter } from './ReplenishmentFooter';

let app: ReactWrapper;

beforeEach(() => {
  ({ app } = setupTestApp(`/replenishment`));
  app.update();
});

describe('ReplenishmentPage <ReplenishmentFooter />', () => {
  it('Should render', () => {
    expect(app.find(ReplenishmentFooter)).toBeDefined();
  });

  it('Should send button disabled by default', () => {
    expect(app.find(ReplenishmentFooter)).toBeDefined();
    expect(app.find(ReplenishmentFooter).find(Button).last().props().disabled).toBe(true);
  });
});
