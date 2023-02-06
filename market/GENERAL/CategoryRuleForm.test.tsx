import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { GLRuleDto, GLRuleType, Subject } from 'src/java/definitions';
import { CategoryRuleForm, CategoryRuleFormProps } from '.';

const defaultProps: CategoryRuleFormProps = {
  parameters: [],
  subject: Subject.PARAMETER,
  onSubmit: jest.fn(),
};

const defaultRule: GLRuleDto = {
  id: 100,
  categoryHid: 200,
  ifs: [],
  inherited: false,
  name: 'Test Rule',
  published: false,
  thens: [],
  type: GLRuleType.MANUAL,
  weight: 200,
};

describe('<CategoryRuleForm />', () => {
  it('renders without errors', () => {
    render(<CategoryRuleForm {...defaultProps} />);

    expect(screen.getByRole('form')).toBeInTheDocument();
  });

  it('name field must be disabled if a inherited rule is passed', () => {
    render(<CategoryRuleForm {...defaultProps} rule={{ ...defaultRule, inherited: true }} />);

    expect(screen.getByRole('textbox', { name: 'name' })).toBeDisabled();
  });

  it('weight field must be disabled if a inherited rule is passed', () => {
    render(<CategoryRuleForm {...defaultProps} rule={{ ...defaultRule, inherited: true }} />);

    expect(screen.getByRole('textbox', { name: 'weight' })).toBeDisabled();
  });

  it('should show the cancel button if the onCancel property is passed', () => {
    render(<CategoryRuleForm {...defaultProps} onCancel={jest.fn()} />);

    expect(screen.getByText(/Отменить/i)).toBeInTheDocument();
  });

  it('should call onSubmit when submitting the form', () => {
    const handleSubmit = jest.fn();
    render(<CategoryRuleForm {...defaultProps} onSubmit={handleSubmit} onCancel={jest.fn()} />);

    const button = screen.getByRole('button', { name: /Создать/i });
    userEvent.type(screen.getByRole('textbox', { name: 'name' }), 'ruleName');
    userEvent.click(button);

    expect(handleSubmit).toBeCalledTimes(1);
  });
});
