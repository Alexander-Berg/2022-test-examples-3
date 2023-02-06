import React from 'react';
import { render } from '@testing-library/react';
import { Form } from 'react-final-form';

import {
  ArrayNameInputAdapter,
  CheckboxAdapter,
  NumberInputAdapter,
  SelectAdapter,
  TextAreaAdapter,
  TextInputAdapter,
} from './FieldAdapters';

const props: any = {
  input: {
    value: null,
    onChange: () => null,
    name: '',
    onBlur: () => null,
    onFocus: () => null,
  },
  meta: {},
};

describe('FieldAdapters', () => {
  describe('<TextInputAdapter>', () => {
    it('renders without errors', () => {
      render(<TextInputAdapter {...props} />);
    });
  });

  describe('<NumberInputAdapter>', () => {
    it('renders without errors', () => {
      render(<NumberInputAdapter {...props} />);
    });
  });

  describe('<TextAreaAdapter>', () => {
    it('renders without errors', () => {
      render(<TextAreaAdapter {...props} />);
    });
  });

  describe('<CheckboxAdapter>', () => {
    it('renders without errors', () => {
      render(<CheckboxAdapter {...props} />);
    });
  });

  describe('<SelectAdapter>', () => {
    it('renders without errors', () => {
      render(<SelectAdapter {...props} options={[]} />);
    });
  });

  describe('<ArrayNameInputAdapter>', () => {
    it('renders without errors', () => {
      render(
        <Form
          onSubmit={() => undefined}
          render={props => (
            <form onSubmit={props.handleSubmit}>
              <ArrayNameInputAdapter name="nameArray" />
            </form>
          )}
        />
      );
    });
  });
});
