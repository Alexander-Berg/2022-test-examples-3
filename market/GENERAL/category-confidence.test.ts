import { shopModel } from 'src/test/data';
import {
  getConfidenceEnumValues,
  getValueFromConfidenceEnums,
  CategoryConfidence,
  confidenceValueFromModel,
  BAD_CATEGORY_CONFIDENCE,
  MEDIUM_CATEGORY_CONFIDENCE,
  HIGHT_CATEGORY_CONFIDENCE,
} from 'src/entities/category';

const testCases = [
  {
    enums: [CategoryConfidence.BAD],
    values: {
      categoryConfidenceTo: BAD_CATEGORY_CONFIDENCE,
      marketCategoryChecked: false,
    },
  },
  {
    enums: [CategoryConfidence.MEDIUM],
    values: {
      categoryConfidenceFrom: BAD_CATEGORY_CONFIDENCE,
      categoryConfidenceTo: MEDIUM_CATEGORY_CONFIDENCE,
      marketCategoryChecked: false,
    },
  },
  {
    enums: [CategoryConfidence.HIGHT],
    values: {
      categoryConfidenceFrom: MEDIUM_CATEGORY_CONFIDENCE,
      marketCategoryChecked: false,
    },
  },
  {
    enums: [CategoryConfidence.BAD, CategoryConfidence.MEDIUM],
    values: {
      categoryConfidenceTo: MEDIUM_CATEGORY_CONFIDENCE,
      marketCategoryChecked: false,
    },
  },
  {
    enums: [CategoryConfidence.MEDIUM, CategoryConfidence.HIGHT],
    values: {
      categoryConfidenceFrom: BAD_CATEGORY_CONFIDENCE,
      marketCategoryChecked: false,
    },
  },
  {
    enums: [CategoryConfidence.CHECKED],
    values: {
      marketCategoryChecked: true,
    },
  },
];

const casesValueFromModel = [
  {
    value: { marketCategoryConfidence: BAD_CATEGORY_CONFIDENCE },
    enum: CategoryConfidence.BAD,
  },
  {
    value: { marketCategoryConfidence: MEDIUM_CATEGORY_CONFIDENCE },
    enum: CategoryConfidence.MEDIUM,
  },
  {
    value: { marketCategoryConfidence: HIGHT_CATEGORY_CONFIDENCE },
    enum: CategoryConfidence.HIGHT,
  },
  {
    value: { marketCategoryConfidence: HIGHT_CATEGORY_CONFIDENCE, marketCategoryChecked: true },
    enum: CategoryConfidence.CHECKED,
  },
];

describe('category-confidence', () => {
  testCases.forEach(el => {
    test(`getValueFromConfidenceEnums ${el.enums}`, () => {
      expect(getValueFromConfidenceEnums(el.enums)).toEqual(el.values);
    });
  });

  testCases.forEach(el => {
    test(`getConfidenceEnumValues ${el.enums}`, () => {
      expect(getConfidenceEnumValues(el.values)).toEqual(el.enums);
    });
  });

  casesValueFromModel.forEach(el => {
    test(`confidenceValueFromModel ${el.enum}`, () => {
      expect(confidenceValueFromModel({ ...shopModel, ...el.value })).toEqual(el.enum);
    });
  });
});
