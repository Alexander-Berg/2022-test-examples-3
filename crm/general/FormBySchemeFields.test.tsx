import React from 'react';
import { Form } from 'components/FinalForm';
import { Form as FormScheme } from 'types/api/form/Form';
import { render, screen } from '@testing-library/react';
import { FormBySchemeFields } from './FormBySchemeFields';

const createFormScheme = ({ access = 3, type = 'Text' } = {}): FormScheme => ({
  meta: {
    fieldsVisibility: ['Text'],
    fields: [
      {
        id: 'Text',
        type,
        title: 'Text caption',
        access,
        isFieldsUpdateNeeded: false,
      },
    ],
  },
  data: [
    {
      id: '1',
      fields: [
        {
          id: 'Text',
          type,
          data: {
            value: 'Text value',
          },
        },
      ],
    },
  ],
});

describe('FormBySchemeFields', () => {
  describe('preview mode', () => {
    it('renders supported fields', () => {
      render(<FormBySchemeFields scheme={createFormScheme()} />);

      expect(screen.queryByText(/Text caption/)).toBeInTheDocument();
      expect(screen.queryByText(/Text value/)).toBeInTheDocument();
    });

    it('renders fallback for unknown fields', () => {
      render(<FormBySchemeFields scheme={createFormScheme({ type: 'UnknownType' })} />);

      expect(screen.queryByText(/UnknownType/)).toBeInTheDocument();
    });
  });

  describe('edit mode', () => {
    it('renders supported fields', () => {
      const handleSubmit = () => {};

      render(
        <Form onSubmit={handleSubmit}>
          <FormBySchemeFields scheme={createFormScheme()} isFormMode />
        </Form>,
      );

      expect(screen.queryByText(/Text caption/)).toBeInTheDocument();
      expect(screen.queryByDisplayValue(/Text value/)).toBeInTheDocument();
    });

    it('renders readonly fields', () => {
      render(<FormBySchemeFields scheme={createFormScheme({ access: 1 })} isFormMode />);

      expect(screen.queryByText(/Text caption/)).toBeInTheDocument();
      expect(screen.queryByText(/Text value/)).toBeInTheDocument();
      expect(screen.queryByDisplayValue(/Text value/)).not.toBeInTheDocument();
    });

    it('renders fallback for unknown fields', () => {
      render(<FormBySchemeFields scheme={createFormScheme({ type: 'UnknownType' })} isFormMode />);

      expect(screen.queryByText(/UnknownType/)).toBeInTheDocument();
    });
  });
});
