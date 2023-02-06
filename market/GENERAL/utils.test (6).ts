import {
  getSourceFragments,
  getFormalizationIntervals,
  sliceFormalizationSource,
  getFormalizationSource,
  getHypothesisFromSource,
} from './utils';
import { shopModel, formalizationValues } from 'src/test/data/shopModel';

const FRAGMENTS_LENGTH = 3;

test('getSourceFragments', () => {
  const { sourceFragments } = getSourceFragments(shopModel, formalizationValues);
  const formalizationFragments = sourceFragments.filter(el => el.isFormalize);
  expect(formalizationFragments.length).toEqual(FRAGMENTS_LENGTH);
});

test('getFormalizationSource', () => {
  const sourceValue = getFormalizationSource(shopModel, formalizationValues[0].valPos!.src);
  expect(sourceValue).toEqual(shopModel.description);
});

test('getFormalizationIntervals', () => {
  const formalizationIntervals = getFormalizationIntervals(formalizationValues);
  expect(formalizationIntervals.length).toEqual(FRAGMENTS_LENGTH);
});

test('sliceFormalizationSource', () => {
  const sourceValue = getFormalizationSource(shopModel, formalizationValues[0].valPos!.src);
  const formalizationIntervals = getFormalizationIntervals(formalizationValues);

  const sourceFragments = sliceFormalizationSource(sourceValue, formalizationIntervals);

  const hypothesisSource = getHypothesisFromSource(shopModel, formalizationValues[0].valPos!);
  const findHypothesis = sourceFragments.find(el => el.text === hypothesisSource);

  expect(findHypothesis).toBeTruthy();
});

test('sliceFormalizationSource', () => {
  const sourceValue = getFormalizationSource(shopModel, formalizationValues[0].valPos!.src);
  const formalizationIntervals = getFormalizationIntervals(formalizationValues);

  // намерено режем текст что б чекнуть что все отработает
  const sourceFragments = sliceFormalizationSource(sourceValue.substring(0, 10), formalizationIntervals);
  expect(sourceFragments).toBeTruthy();
});
