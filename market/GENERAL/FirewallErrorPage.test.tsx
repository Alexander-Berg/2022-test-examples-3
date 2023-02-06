import React from 'react';

import { render } from '@/test-utils';
import { FirewallErrorPage } from '@/pages/firewall-error-page/FirewallErrorPage';

describe('<FirewallErrorPage />', () => {
  it('renders without errors', () => {
    expect(() => {
      render(<FirewallErrorPage />);
    });

    const component = render(<FirewallErrorPage />);
    expect(component.getByText('puncher', { exact: false }).getAttribute('href')).toBe(
      'https://puncher.yandex-team.ru'
    );
  });
});
