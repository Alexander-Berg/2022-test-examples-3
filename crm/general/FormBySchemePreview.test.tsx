import React from 'react';
import { Form } from 'types/api/form/Form';
import { render, screen, fireEvent } from '@testing-library/react';
import { FormBySchemePreview } from './FormBySchemePreview';

const simpleFormScheme: Form = {
  meta: {
    fieldsVisibility: ['Text'],
    fields: [
      {
        id: 'Text',
        type: 'Text',
        title: 'Text caption',
        access: 3,
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
          type: 'Text',
          data: {
            value: 'Text value',
          },
        },
      ],
    },
  ],
};

describe('FormBySchemePreview', () => {
  it('renders preview form', () => {
    render(<FormBySchemePreview scheme={simpleFormScheme} />);

    expect(screen.queryByText(/Text caption/)).toBeInTheDocument();
    expect(screen.queryByText(/Text value/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /редактировать/i })).not.toBeInTheDocument();
  });

  it('renders preview form with edit', () => {
    const mockHandleEdit = jest.fn();

    render(<FormBySchemePreview scheme={simpleFormScheme} onEdit={mockHandleEdit} />);

    const editButton = screen.queryByRole('button', { name: /редактировать/i });

    expect(screen.queryByText(/Text caption/)).toBeInTheDocument();
    expect(screen.queryByText(/Text value/)).toBeInTheDocument();
    expect(editButton).toBeInTheDocument();

    fireEvent.click(editButton!);

    expect(mockHandleEdit).toBeCalledTimes(1);
  });
});
