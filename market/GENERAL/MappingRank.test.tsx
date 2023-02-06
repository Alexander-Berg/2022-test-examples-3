import React from 'react';
import { render } from '@testing-library/react';

import { MappingRankCell } from './MappingRankCell';
import { simpleMappingWithRule } from 'src/test/data';

import { RANK_OPTIONS, LOW_RANK_OPTIONS } from '../../ContentMappingForm/RankInput';

describe('RankCell', () => {
  [...RANK_OPTIONS, ...LOW_RANK_OPTIONS].forEach(el => {
    test(`rank ${el.label}`, () => {
      const app = render(<MappingRankCell row={{ ...(simpleMappingWithRule as any), rank: el.value }} />);
      app.getByText(el.label);
    });
  });
});
