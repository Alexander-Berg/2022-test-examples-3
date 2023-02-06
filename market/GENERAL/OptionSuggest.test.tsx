import React from 'react';
import { render } from '@testing-library/react';

import { SuggestDto } from 'src/java/definitions';
import { OptionSuggest } from './OptionSuggest';

function generateUniqCode(option: SuggestDto) {
  return option.overrideId;
}

function generateContent(option: SuggestDto) {
  return option.overrideName;
}

describe('<OptionSuggest />', () => {
  it('renders without errors', () => {
    render(
      <OptionSuggest<SuggestDto>
        options={[]}
        onChange={() => null}
        generateUniqCode={generateUniqCode}
        generateContent={generateContent}
      />
    );
  });
});
