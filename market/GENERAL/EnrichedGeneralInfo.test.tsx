import React from 'react';
import { render } from '@testing-library/react';

import { EnrichedGeneralInfo } from './EnrichedGeneralInfo';
import { enrichedOffer } from 'src/test/mockData/enrichedOffer';
import { ENRICHED_NAMES, MATCH_TYPE_NAMES, OFFER_PROBLEM_TYPE_NAMES } from 'src/entities/datacampOffer/constants';
import { ProblemType } from 'src/entities/datacampOffer/types';

describe('<EnrichedGeneralInfo />', () => {
  test('render ok offer', () => {
    const app = render(<EnrichedGeneralInfo data={enrichedOffer} />);

    app.getByText(MATCH_TYPE_NAMES[enrichedOffer.matched_type_value!], { exact: false });
  });

  test('render failed offer', () => {
    const failedOffer = {
      ...enrichedOffer,
      offer_problem: [
        {
          problem_type: ProblemType.VENDOR_TAG_NOT_FOUND,
        },
      ],
    };

    const app = render(<EnrichedGeneralInfo data={failedOffer} />);

    app.getByText(ENRICHED_NAMES.offer_problem, { exact: false });
    app.getByText(OFFER_PROBLEM_TYPE_NAMES[ProblemType.VENDOR_TAG_NOT_FOUND], { exact: false });
  });
});
