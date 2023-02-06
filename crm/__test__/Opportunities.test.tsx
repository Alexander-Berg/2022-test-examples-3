import React from 'react';
import { TestBed } from 'components/TestBed';
import { render, screen } from '@testing-library/react';
import { Opportunities } from '../Opportunities';
import { OpportunitiesServiceStub } from './OpportunitiesServiceStub';

const opportunities = [
  {
    id: '2',
    name: 'name',
  },
];

const account = {
  id: 1,
  info: {
    login: '1',
  },
};

describe('Opportunities', () => {
  it('renders title', () => {
    const opportunitiesServiceStub = new OpportunitiesServiceStub();
    render(<Opportunities opportunitiesService={opportunitiesServiceStub} />);
    expect(screen.queryByText('Сделки')).toBeInTheDocument();
  });

  describe('when has no opportunities', () => {
    it('render link button', () => {
      const opportunitiesServiceStub = new OpportunitiesServiceStub();
      render(<Opportunities opportunitiesService={opportunitiesServiceStub} account={account} />);
      expect(screen.queryByRole('button', { name: 'связать' })).toBeInTheDocument();
    });
  });

  describe('when has opportunities', () => {
    const opportunitiesServiceStub = new OpportunitiesServiceStub();
    opportunitiesServiceStub.opportunities = opportunities;

    it('renders edit button', () => {
      render(
        <TestBed>
          <Opportunities opportunitiesService={opportunitiesServiceStub} account={account} />
        </TestBed>,
      );
      expect(screen.queryByRole('button', { name: 'изменить' })).toBeInTheDocument();
    });

    it('renders opportunities list', () => {
      render(
        <TestBed>
          <Opportunities opportunitiesService={opportunitiesServiceStub} />
        </TestBed>,
      );
      expect(screen.queryByRole('link', { name: String(opportunities[0].id) })).toBeInTheDocument();
      expect(screen.queryByText(opportunities[0].name)).toBeInTheDocument();
    });
  });
});
