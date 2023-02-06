import React from 'react';
import { withRouter } from 'react-router';
import { render } from '@/test-utils';

import { ViewerDto } from '@/dto';
import { AccessDeniedPage as Page } from '.';

const AccessDeniedPage = withRouter(Page);

describe('<AccessDeniedPage />', () => {
  it('should be rendered without errors', () => {
    expect(() => {
      render(<AccessDeniedPage />);
    }).not.toThrow();

    expect(() => {
      render(<AccessDeniedPage user={null} />);
    }).not.toThrow();

    const component = render(<AccessDeniedPage user={{ login: 'vault_dweller' } as ViewerDto} />);
    const loginBlock = component.getByText('ault_dweller', { exact: false });
    expect(loginBlock).toBeInTheDocument();
  });
});
