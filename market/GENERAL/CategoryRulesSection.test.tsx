import React from 'react';
import { screen } from '@testing-library/react';

import { Subject } from 'src/java/definitions';
import { renderWithProvider } from 'src/test/setupTestProvider';
import { CategoryRulesSection, CategoryRulesSectionProps } from '.';

const defaultProps: CategoryRulesSectionProps = {
  rules: [],
  parameters: [],
  subject: Subject.PARAMETER,
  onCreate: jest.fn(),
  onRemove: jest.fn(),
  onUpdate: jest.fn(),
};

describe('<CategoryRulesSection />', () => {
  it('renders without errors', () => {
    renderWithProvider(<CategoryRulesSection {...defaultProps} />);
  });

  it('should contains table', () => {
    renderWithProvider(<CategoryRulesSection {...defaultProps} />);

    expect(screen.getByRole('grid')).toBeInTheDocument();
  });

  it('should contains create button', () => {
    renderWithProvider(<CategoryRulesSection {...defaultProps} />);

    expect(screen.getByText(/Создать правило/i)).toBeInTheDocument();
  });
});
