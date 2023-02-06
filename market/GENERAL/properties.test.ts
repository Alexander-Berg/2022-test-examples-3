import {
  isComplexType,
  isArrayType,
  isSimpleType,
  isCheckboxWidgetType,
  isImageWidgetType,
  isRichTextWidgetType,
  isListBoxWidgetType,
  isSuggestWidgetType,
  hasSuggestionType,
  isDateTimeWidgetType,
} from './properties';

describe('utils/properties', () => {
  describe('isComplexType', () => {
    it('should return true', () => {
      expect(isComplexType({ allowedTypes: ['ContentType'] })).toBe(true);
    });

    it('should return false', () => {
      expect(isComplexType({})).toBe(false);
      expect(isComplexType({ allowedTypes: [] })).toBe(false);
    });
  });

  describe('isArrayType', () => {
    it('should return true', () => {
      expect(isArrayType({ allowedValuesMax: 2 })).toBe(true);
      expect(isArrayType({ allowedValuesMax: 5 })).toBe(true);
    });

    it('should return false', () => {
      expect(isArrayType({})).toBe(false);
      expect(isArrayType({ allowedValuesMax: 0 })).toBe(false);
      expect(isArrayType({ allowedValuesMax: 1 })).toBe(false);
    });
  });

  describe('isSimpleType', () => {
    it('should return true', () => {
      expect(isSimpleType({})).toBe(true);
      expect(isSimpleType({ allowedValuesMax: 1 })).toBe(true);
      expect(isSimpleType({ allowedTypes: [] })).toBe(true);
      expect(isSimpleType({ allowedTypes: [], allowedValuesMax: 1 })).toBe(true);
    });

    it('should return false', () => {
      expect(isSimpleType({ allowedValuesMax: 2 })).toBe(false);
      expect(isSimpleType({ allowedTypes: ['ContentType'] })).toBe(false);
      expect(isSimpleType({ allowedValuesMax: 2, allowedTypes: ['ContentType'] })).toBe(false);
    });
  });

  describe('isCheckboxWidgetType', () => {
    it('should return true', () => {
      expect(isCheckboxWidgetType({ widgetType: 'checkbox' })).toBe(true);
    });

    it('should return false', () => {
      expect(isCheckboxWidgetType({})).toBe(false);
      expect(isCheckboxWidgetType({ widgetType: 'checkbox_widget' })).toBe(false);
    });
  });

  describe('isImageWidgetType', () => {
    it('should return true', () => {
      expect(isImageWidgetType({ widgetType: 'upload_image' })).toBe(true);
    });

    it('should return false', () => {
      expect(isImageWidgetType({})).toBe(false);
      expect(isImageWidgetType({ widgetType: 'upload_image_widget' })).toBe(false);
    });
  });

  describe('isRichTextWidgetType', () => {
    it('should return true', () => {
      expect(isRichTextWidgetType({ widgetType: 'rich_text' })).toBe(true);
    });

    it('should return false', () => {
      expect(isRichTextWidgetType({})).toBe(false);
      expect(isRichTextWidgetType({ widgetType: 'rich_text_widget' })).toBe(false);
    });
  });

  describe('isListBoxWidgetType', () => {
    it('should return true', () => {
      expect(isListBoxWidgetType({ widgetType: 'list_box' })).toBe(true);
    });

    it('should return false', () => {
      expect(isListBoxWidgetType({})).toBe(false);
      expect(isListBoxWidgetType({ widgetType: 'list_box_widget' })).toBe(false);
    });
  });

  describe('isSuggestWidgetType', () => {
    it('should return true', () => {
      expect(isSuggestWidgetType({ widgetType: 'suggest' })).toBe(true);
    });

    it('should return false', () => {
      expect(isSuggestWidgetType({})).toBe(false);
      expect(isSuggestWidgetType({ widgetType: 'suggest_widget' })).toBe(false);
    });
  });

  describe('hasSuggestionType', () => {
    it('should return true', () => {
      expect(hasSuggestionType({ suggestionType: 'categories' })).toBe(true);
    });

    it('should return false', () => {
      expect(hasSuggestionType({})).toBe(false);
      // @ts-expect-error
      expect(hasSuggestionType({ suggestionType: 1 })).toBe(false);
    });
  });

  describe('isDateTimeWidgetType', () => {
    it('should return true', () => {
      expect(isDateTimeWidgetType({ widgetType: 'date_time' })).toBe(true);
    });

    it('should return false', () => {
      expect(isDateTimeWidgetType({})).toBe(false);
      expect(isDateTimeWidgetType({ widgetType: 'date_time_widget' })).toBe(false);
    });
  });
});
