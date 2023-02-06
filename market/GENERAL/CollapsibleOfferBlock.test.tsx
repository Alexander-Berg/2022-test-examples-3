import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupTestProvider } from 'src/test/utils';
import { CollapsibleOfferBlock } from './CollapsibleOfferBlock';
import { ROUTES } from 'src/routes/routes';

describe('<CollapsibleOfferBlock />', () => {
  test('expand from url query', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider
        initialLocation={{
          pathname: ROUTES.OFFER_DIAGNOSTIC_PAGE,
          // сразу проставляем ид блока что бы проверить, что при открытии по ссылке блок развернется
          search: `?expandedBlock=SKU`,
        }}
      >
        <CollapsibleOfferBlock
          id="SKU"
          title="SKU"
          data="expanded_test"
          ContentComponent={({ data }) => <div>{data}</div>}
        />
      </Provider>
    );

    screen.getByText('expanded_test');

    userEvent.click(screen.getByTitle('Свернуть'));

    // после сворачивания блок должен удалятся из урла
    expect(window.location.search).toBe('');

    userEvent.click(screen.getByTitle('Развернуть'));

    // при разворачивани ид блока должно попадать в урл
    expect(window.location.search).toBe('?expandedBlock=SKU');
  });

  test('default expanded block', () => {
    const { Provider } = setupTestProvider();
    render(
      <Provider>
        <CollapsibleOfferBlock
          id="SKU"
          title="SKU"
          data="expanded_test"
          defaultExpanded
          ContentComponent={({ data }) => <div>{data}</div>}
        />
      </Provider>
    );

    userEvent.click(screen.getByTitle('Свернуть'));
    userEvent.click(screen.getByTitle('Развернуть'));

    /**
     * при разворачивании блок с флагом defaultExpanded не должен попадать в урл,
     * так как он все равно при переходе по ссылке развернется
     */
    expect(window.location.search).toBe('');
  });
});
