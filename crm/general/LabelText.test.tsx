import React from 'react';
import { render, screen } from '@testing-library/react';
import { LabelText } from './LabelText';

describe('LabelText', () => {
  describe('props.label', () => {
    describe('when defined', () => {
      it('renders label', () => {
        render(<LabelText label="test label" />);
        expect(screen.getByText('test label')).toBeInTheDocument();
      });
    });
  });

  describe('props.text', () => {
    describe('when defined', () => {
      it('renders text', () => {
        render(<LabelText text="test text" />);
        expect(screen.getByText('test text')).toBeInTheDocument();
      });
    });
  });
});
