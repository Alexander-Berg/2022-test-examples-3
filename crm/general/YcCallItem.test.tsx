import React from 'react';
import { TestBed } from 'components/TestBed';
import { render, screen } from '@testing-library/react';
import YcCallItem from './YcCallItem';
import { YcCallItemProps } from './YcCallItem.types';

const createCallItemProps = (
  patchForCallData?: Partial<YcCallItemProps['item']['call']>,
): YcCallItemProps => {
  return {
    item: {
      id: 1,
      date: '2022-05-31T17:27:53.0000000+03:00',
      type: 'YcCall',
      connection: '',
      call: {
        id: 1,
        author: {
          id: 1,
          name: '',
        },
        hasFactors: false,
        date: '2022-05-31T17:27:53.0000000+03:00',
        direction: 'Out',
        ...patchForCallData,
      },
      parent: {
        id: 1,
        type: 'Issue',
        issue: {
          id: 1,
          type: '',
          typeId: 1,
        },
      },
    },
    toggle: false,
    onToggle: () => {},
  };
};

describe('YcCallItem', () => {
  describe('when call data has comment', () => {
    it('displays comment block', () => {
      const { container } = render(
        <TestBed>
          <YcCallItem {...createCallItemProps({ comment: 'comment' })} />
        </TestBed>,
      );

      expect(container.getElementsByClassName(`CallHistoryItem__dataRow`)).toHaveLength(1);
      expect(container.getElementsByClassName(`CallHistoryItem__dataRow`)[0]).toHaveTextContent(
        'comment',
      );
    });
  });

  describe('when call data has no comment', () => {
    it('displays no comment block', () => {
      const { container } = render(
        <TestBed>
          <YcCallItem {...createCallItemProps()} />
        </TestBed>,
      );

      expect(container.getElementsByClassName(`CallHistoryItem__dataRow`)).toHaveLength(0);
    });
  });

  describe('when call data has tags', () => {
    it('displays tags block', () => {
      render(
        <TestBed>
          <YcCallItem {...createCallItemProps({ tags: [{ id: 1, name: 'Tag', color: '#000' }] })} />
        </TestBed>,
      );

      expect(screen.queryByTestId('tags')).toBeInTheDocument();
      expect(screen.queryByText('Tag')).toBeInTheDocument();
    });
  });

  describe('when call data has empty array of tags', () => {
    it('displays no tags block', () => {
      render(
        <TestBed>
          <YcCallItem {...createCallItemProps({ tags: [] })} />
        </TestBed>,
      );

      expect(screen.queryByTestId('tags')).not.toBeInTheDocument();
    });
  });
});
