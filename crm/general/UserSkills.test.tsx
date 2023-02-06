import React from 'react';
import { render, screen } from '@testing-library/react';
import { UserSkills } from './UserSkills';

describe('UserSkills', () => {
  describe('when no set value', () => {
    it('renders placeholder', () => {
      render(<UserSkills />);

      expect(screen.queryByText('—')).toBeInTheDocument();
    });
  });

  describe('when value set to empty array', () => {
    it('renders placeholder', () => {
      render(<UserSkills value={[]} />);

      expect(screen.queryByText('—')).toBeInTheDocument();
    });
  });

  describe('when not empty array', () => {
    it('renders skills', () => {
      render(
        <UserSkills
          value={[
            { id: 1, name: 'skill1', value: 1 },
            { id: 2, name: 'skill2', value: 2 },
          ]}
        />,
      );

      expect(screen.queryByText('skill1 1')).toBeInTheDocument();
      expect(screen.queryByText('skill2 2')).toBeInTheDocument();
    });
  });
});
