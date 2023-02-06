import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Category as CategoryStore } from 'modules/categorization2/State/defaultStores/Category';
import { Column } from './Column';
import { Category } from '../../../types';
import { categoryTestId, checkboxTestId } from '../../Category/Category.constants';

describe('components/Tree/Column', () => {
  beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
  });

  afterAll(() => {
    jest.clearAllMocks();
  });

  const categories: Category[] = [
    {
      id: 1,
      name: 'Category 1',
      isLeaf: true,
    },
    {
      id: 2,
      name: 'Category 2',
      isLeaf: true,
    },
  ].map((dto) => new CategoryStore(dto));

  describe('props.categories', () => {
    it('renders all categories in list', () => {
      render(<Column depth={0} categories={categories} />);
      const categoryNodes = screen.getAllByTestId(categoryTestId);

      expect(categoryNodes).toHaveLength(2);
    });
  });

  describe('props.onClick', () => {
    it('calls callback on category click', () => {
      const handleClick = jest.fn();
      const depth = 1337;
      render(<Column depth={depth} categories={categories} onClick={handleClick} />);
      const categoryNodes = screen.getAllByTestId(categoryTestId);

      fireEvent.click(categoryNodes[0]);

      expect(handleClick).toBeCalledTimes(1);
      expect(handleClick).toBeCalledWith(1, depth);
    });
  });

  describe('props.onSelect', () => {
    it('calls callback on category select', () => {
      const handleChange = jest.fn();
      render(<Column depth={1337} categories={categories} onChange={handleChange} />);
      const checkboxNodes = screen.getAllByTestId(checkboxTestId);

      fireEvent.click(checkboxNodes[0]);

      expect(handleChange).toBeCalledTimes(1);
      expect(handleChange).toBeCalledWith(1, expect.any(Boolean));
    });
  });
});
