import { getProtoModel } from './deNormalize';
import { testNormalisedModel } from './testNormalisedModel';
import { testProtoModel } from './testProtoModel';
import { getNormalizedModel } from './normalize';

describe('normaliseModel', () => {
  it('should work', () => {
    const normalizedModel = getNormalizedModel(testProtoModel);
    const processedProtoModel = getProtoModel(testProtoModel, normalizedModel);

    expect(normalizedModel).toEqual(testNormalisedModel);
    expect(processedProtoModel).toEqual(testProtoModel);
  });
});
