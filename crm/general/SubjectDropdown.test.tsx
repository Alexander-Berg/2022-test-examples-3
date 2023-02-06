import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react';
import SubjectDropdown from './SubjectDropdown';
import { Subject } from '../NewMailForm.types';

describe('SubjectDropdown', () => {
  const handleChange = jest.fn(() => {});

  describe('when subjects array has default subject', () => {
    const subjects: Subject[] = [
      {
        id: 0,
        isDefault: true,
        name: 'default',
      },
      {
        id: 1,
        isDefault: false,
        name: 'not default',
      },
    ];

    it('shows label for default subject', async () => {
      const { queryByText, getByRole } = render(
        <SubjectDropdown subjects={subjects} onChange={handleChange} />,
      );

      fireEvent.click(getByRole('button'));

      await waitFor(() => {
        expect(queryByText('Тема по умолчанию')).toBeInTheDocument();
      });
    });
  });

  describe('when subjects array has not default subject', () => {
    const subjects: Subject[] = [
      {
        id: 0,
        isDefault: false,
        name: 'not default',
      },
      {
        id: 1,
        isDefault: false,
        name: 'not default',
      },
    ];

    it('does not show label for default subject', async () => {
      const { queryByText, getByRole } = render(
        <SubjectDropdown subjects={subjects} onChange={handleChange} />,
      );

      fireEvent.click(getByRole('button'));

      await waitFor(() => {
        expect(queryByText('Тема по умолчанию')).not.toBeInTheDocument();
      });
    });
  });

  describe('when subjects array has additional subjects', () => {
    it('shows label for additional subjects', async () => {
      const subjects: Subject[] = [
        {
          id: 0,
          isDefault: true,
          name: 'not default',
        },
        {
          id: 1,
          isDefault: false,
          name: 'not default',
        },
        {
          id: 2,
          isDefault: false,
          name: 'not default',
        },
      ];

      const { queryByText, getByRole } = render(
        <SubjectDropdown subjects={subjects} onChange={handleChange} />,
      );

      fireEvent.click(getByRole('button'));

      await waitFor(() => {
        expect(queryByText('Дополнительные темы')).toBeInTheDocument();
      });
    });
  });

  describe('when subjects array has not additional subjects', () => {
    it('shows label for additional subjects', async () => {
      const subjects: Subject[] = [
        {
          id: 0,
          isDefault: true,
          name: 'not default',
        },
      ];

      const { queryByText, getByRole } = render(
        <SubjectDropdown subjects={subjects} onChange={handleChange} />,
      );

      fireEvent.click(getByRole('button'));

      await waitFor(() => {
        expect(queryByText('Дополнительные темы')).not.toBeInTheDocument();
      });
    });
  });

  describe('when subject gets clicked', () => {
    const subjects: Subject[] = [
      {
        id: 100,
        isDefault: true,
        name: 'default subject',
      },
    ];

    it('calls onChange callback', async () => {
      const handleChange = jest.fn(() => {});
      const { queryByText, getByText, getByRole } = render(
        <SubjectDropdown subjects={subjects} onChange={handleChange} />,
      );

      fireEvent.click(getByRole('button'));
      await waitFor(() => {
        expect(queryByText('default subject')).toBeInTheDocument();
      });
      fireEvent.click(getByText('default subject'));

      await waitFor(() => {
        expect(handleChange).toBeCalledTimes(1);
      });
    });
  });
});
