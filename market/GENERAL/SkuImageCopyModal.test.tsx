import { render } from '@testing-library/react';
import React from 'react';
import { partialWrapper } from '@yandex-market/mbo-test-utils';
import { NormalisedModel } from '@yandex-market/mbo-parameter-editor';

import { SkuImageCopyModal } from './SkuImageCopyModal';

describe('', () => {
  it('renders', () => {
    render(<SkuImageCopyModal sku={partialWrapper<NormalisedModel>({})} onClose={jest.fn()} />);
  });
});
