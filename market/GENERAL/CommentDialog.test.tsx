import * as React from 'react';
import { ContentComment } from '@yandex-market/market-proto-dts/Market/Mboc';
import { ContentCommentType } from '@yandex-market/market-proto-dts/Market/Mboc/ContentCommentTypes';
import { mount } from 'enzyme';

import { CommentDialog } from './CommentDialog';

const COMMENT_WITH_VARIANTS_AND_ITEMS_REQUIRED = 'COMMENT_WITH_VARIANTS_AND_ITEMS_REQUIRED';
const ALLOW_OTHER = 'Опишите';

const commentTypes: ContentCommentType[] = [
  {
    type: COMMENT_WITH_VARIANTS_AND_ITEMS_REQUIRED,
    description: COMMENT_WITH_VARIANTS_AND_ITEMS_REQUIRED,
    variant: [{ name: 'A' }, { name: 'B' }],
    require_items: true,
    allow_other: ALLOW_OTHER,
  },
];

const comments: ContentComment[] = [
  {
    items: [],
    type: COMMENT_WITH_VARIANTS_AND_ITEMS_REQUIRED,
    message: {},
  },
];

describe('CommentDialog', () => {
  it('Should render an error when require_items is true and no variant is selected', () => {
    const onChange = jest.fn();
    const onCancel = jest.fn();
    const dialog = mount(
      <CommentDialog types={commentTypes} value={comments} onChange={onChange} onCancel={onCancel} />
    );
    const errors = dialog.find('[data-testid="validation-error"]');
    expect(errors).toHaveLength(1);
    expect(errors.map(item => item.text())).toEqual([`Укажите хотя бы одно значение, или "${ALLOW_OTHER}"`]);
  });
});
