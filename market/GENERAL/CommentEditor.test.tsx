import { Textarea } from '@yandex-lego/components/Textarea';
import { ContentCommentType } from '@yandex-market/market-proto-dts/Market/Mboc/ContentCommentTypes';
import * as React from 'react';
import { mount } from 'enzyme';

import { Select } from 'src/shared/components';
import { CommentEditor, validateComment, WRONG_CATEGORY } from 'src/shared/common-logs/components/CommentEditor';

const COMMENT_WITH_OTHER_AND_VARIANTS = 'COMMENT_WITH_OTHER_AND_VARIANTS';
const COMMENT_WITH_OTHER = 'COMMENT_WITH_OTHER';
const COMMENT_WITH_VARIANTS = 'COMMENT_WITH_VARIANTS';
const ALLOW_OTHER = 'Give me other';

describe('CommentEditor::Component', () => {
  const commentTypes: ContentCommentType[] = [
    {
      type: COMMENT_WITH_OTHER_AND_VARIANTS,
      description: COMMENT_WITH_OTHER_AND_VARIANTS,
      allow_other: ALLOW_OTHER,
      variant: [{ name: 'A' }, { name: 'B' }],
    },
    {
      type: COMMENT_WITH_OTHER,
      description: 'COMMENT_WITH_OTHER',
      allow_other: ALLOW_OTHER,
    },
    {
      type: COMMENT_WITH_VARIANTS,
      description: 'COMMENT_WITH_VARIANTS',
      variant: [{ name: 'A' }, { name: 'B' }],
    },
    {
      type: WRONG_CATEGORY,
      description: 'WRONG_CATEGORY',
      allow_other: 'Tell me why?',
    },
  ];

  it('Should render select on empty input and disableWrongCategory', () => {
    const onSaveComment = jest.fn();
    const editor = mount(
      <CommentEditor commentTypes={commentTypes} offerComment={{}} onSaveComment={onSaveComment} disableWrongCategory />
    );

    expect(editor.find(Select).props().options).toEqual([
      { label: 'COMMENT_WITH_OTHER_AND_VARIANTS', value: 'COMMENT_WITH_OTHER_AND_VARIANTS' },
      { label: 'COMMENT_WITH_OTHER', value: 'COMMENT_WITH_OTHER' },
      { label: 'COMMENT_WITH_VARIANTS', value: 'COMMENT_WITH_VARIANTS' },
    ]);
    // NOTE: No wrong category
  });

  it('Should render select on empty input and include wrong category if allowed to', () => {
    const onSaveComment = jest.fn();
    const editor = mount(<CommentEditor commentTypes={commentTypes} offerComment={{}} onSaveComment={onSaveComment} />);

    expect(editor.find(Select).props().options).toEqual([
      { label: 'COMMENT_WITH_OTHER_AND_VARIANTS', value: 'COMMENT_WITH_OTHER_AND_VARIANTS' },
      { label: 'COMMENT_WITH_OTHER', value: 'COMMENT_WITH_OTHER' },
      { label: 'COMMENT_WITH_VARIANTS', value: 'COMMENT_WITH_VARIANTS' },
      { label: 'WRONG_CATEGORY', value: 'WRONG_CATEGORY' },
    ]);
    // NOTE: With wrong category
  });

  it('Should render select with items in case there are some', () => {
    const onSaveComment = jest.fn();
    const editor = mount(
      <CommentEditor
        commentTypes={commentTypes}
        offerComment={{ selectedComment: COMMENT_WITH_VARIANTS, selectedCommentInfoIndices: [0] }}
        onSaveComment={onSaveComment}
        disableWrongCategory
      />
    );
    const variants = editor.find(Select).at(1);
    expect(variants).toExist();
    expect(variants.find(Select).props().options).toEqual([
      { label: 'A', value: '0' },
      { label: 'B', value: '1' },
    ]);
  });

  it('Should have special display for WRONG_CATEGORY if disabled', () => {
    const onSaveComment = jest.fn();
    const editor = mount(
      <CommentEditor
        commentTypes={commentTypes}
        offerComment={{ selectedComment: WRONG_CATEGORY, customCommentText: 'comment' }}
        onSaveComment={onSaveComment}
        disableWrongCategory
      />
    );
    expect(editor.find(Select).prop('isDisabled')).toBeTruthy();
    expect(editor.find(Select).prop('value')).toEqual({ label: 'Неправильная категория', value: 'WRONG_CATEGORY' });
    expect(editor.find(Textarea).exists({ value: 'comment' })).toBeTrue();
  });

  it('Should use usual display for WRONG_CATEGORY if enabled', () => {
    const onSaveComment = jest.fn();
    const editor = mount(
      <CommentEditor
        commentTypes={commentTypes}
        offerComment={{ selectedComment: WRONG_CATEGORY, customCommentText: 'comment' }}
        onSaveComment={onSaveComment}
      />
    );
    expect(editor.find(Select).prop('value')).toEqual({ label: 'Неправильная категория', value: 'WRONG_CATEGORY' });
    // NOTE: You can switch to other comment
    expect(editor.find(Select).prop('disabled')).toBeFalsy();
    expect(editor.find(Textarea).exists({ value: 'comment' })).toBeTrue();
  });

  it('Should append the text value of "allow_other" to the list of selectable options', () => {
    const onSaveComment = jest.fn();
    const customCommentText = 'comment';
    const editor = mount(
      <CommentEditor
        commentTypes={commentTypes}
        offerComment={{
          selectedComment: COMMENT_WITH_OTHER_AND_VARIANTS,
          selectedCommentInfoIndices: [0, -1],
          customCommentText,
        }}
        onSaveComment={onSaveComment}
        disableWrongCategory
      />
    );
    const variants = editor.find(Select).at(1);
    expect(variants).toExist();
    // NOTE: ALLOW_OTHER must be appended to the list of choices
    expect(variants.find(Select).props().options).toEqual([
      { label: 'A', value: '0' },
      { label: 'B', value: '1' },
      { label: 'Give me other', value: '-1' },
    ]);
    // NOTE: ALLOW_OTHER has the value of -1
    expect(variants.prop('value')).toEqual([
      { label: 'A', value: '0' },
      { label: 'Give me other', value: '-1' },
    ]);
    expect(editor.find(Textarea).exists({ value: customCommentText })).toBeTrue();
  });
});

describe('CommentEditor::validators', () => {
  it('Should not validate when require_items is true and no variant is selected', () => {
    const offerComment = {
      selectedComment: COMMENT_WITH_OTHER_AND_VARIANTS,
      selectedCommentInfoIndices: [],
      customCommentText: '',
    };
    const offerCommentType = {
      type: COMMENT_WITH_OTHER_AND_VARIANTS,
      description: COMMENT_WITH_OTHER_AND_VARIANTS,
      allow_other: ALLOW_OTHER,
      require_items: true,
      variant: [{ name: 'A' }, { name: 'B' }],
    };
    const validationResult = validateComment(offerComment, offerCommentType);
    expect(validationResult).toEqual(`Укажите хотя бы одно значение, или "${ALLOW_OTHER}"`);
  });

  it('Should not validate when allow_other has a value, require_items is true and textarea is empty', () => {
    const offerComment = {
      selectedComment: COMMENT_WITH_OTHER_AND_VARIANTS,
      // NOTE: we have selected the first option ('A') + other
      selectedCommentInfoIndices: [0, -1],
      customCommentText: '',
    };
    const offerCommentType = {
      type: COMMENT_WITH_OTHER_AND_VARIANTS,
      description: COMMENT_WITH_OTHER_AND_VARIANTS,
      allow_other: ALLOW_OTHER,
      require_items: true,
      variant: [{ name: 'A' }, { name: 'B' }],
    };
    const validationResult = validateComment(offerComment, offerCommentType);
    expect(validationResult).toEqual(`Заполните текстовое поле`);
  });

  it('Should validate when allow_other has a value, require_items is true and textarea is non-empty', () => {
    const customCommentText = 'Just a comment';
    const offerComment = {
      selectedComment: COMMENT_WITH_OTHER_AND_VARIANTS,
      selectedCommentInfoIndices: [0, -1],
      customCommentText,
    };
    const offerCommentType = {
      type: COMMENT_WITH_OTHER_AND_VARIANTS,
      description: COMMENT_WITH_OTHER_AND_VARIANTS,
      allow_other: ALLOW_OTHER,
      require_items: true,
      variant: [{ name: 'A' }, { name: 'B' }],
    };
    const validationResult = validateComment(offerComment, offerCommentType);
    expect(validationResult).toBeUndefined();
  });
});
