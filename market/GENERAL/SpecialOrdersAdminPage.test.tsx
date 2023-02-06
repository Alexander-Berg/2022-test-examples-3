import { TestCmApp } from 'src/test/setupApp';
import { SpecialOrdersAdminPage } from './SpecialOrdersAdminPage';

describe('SpecialOrdersAdminPage', () => {
  it('renders', () => {
    const app = new TestCmApp('/special-order-admin');
    expect(app.find(SpecialOrdersAdminPage)).toHaveLength(1);
  });
});
