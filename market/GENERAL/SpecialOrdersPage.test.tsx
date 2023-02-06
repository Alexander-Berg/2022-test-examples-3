import { TestCmApp } from 'src/test/setupApp';
import { SpecialOrdersPage } from './SpecialOrdersPage';

describe('SpecialOrdersPage', () => {
  it('renders', () => {
    const app = new TestCmApp('/special-order');
    expect(app.find(SpecialOrdersPage)).toHaveLength(1);
  });
});
