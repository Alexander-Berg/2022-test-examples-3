import { act, screen } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';

describe('App:', () => {
  test('should display the audit page', () => {
    setupTestApp({ route: ROUTES.AUDIT.path });
    // Текст разбит на две части: первая буква красным цветом
    expect(screen.getByText(ROUTES.AUDIT.title.slice(1)).tagName).toEqual('H1');
  });

  test('should go to statistics page on history.push', () => {
    const { history } = setupTestApp({ route: ROUTES.AUDIT.path });
    /**
     * NOTE: the syntax `app.find(NavLink).at(0).simulate('click');` wouldn't work here.
     * To transition to another page use: `history.push(<path>)`
     */
    act(() => {
      history.push(ROUTES.STATISTIC.path);
    });

    expect(screen.getByText(ROUTES.STATISTIC.title.slice(1)).tagName).toEqual('H1');
  });
});
