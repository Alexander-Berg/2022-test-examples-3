import { render } from 'enzyme';
import React from 'react';

import { Loader } from './Loader';

describe('Loader', () => {
  it('renders without errors', () => {
    render(<Loader isLoading />);

    render(<Loader isLoading={false} />);
  });

  it('renders custom placeholder', () => {
    const loadingMessage = 'Testik Testovich';
    const component = render(<Loader isLoading customLoadingMessage={<main>{loadingMessage}</main>} />);
    const pendingElement = component.find('.MboFront_Loader-placeholder');
    expect(pendingElement.children()).toHaveLength(2);
    expect(pendingElement.children().get(1).children[0].data).toBe(loadingMessage);
  });
});
