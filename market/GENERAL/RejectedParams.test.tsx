import React from 'react';
import { render } from '@testing-library/react';

import { FormalizedParamType } from 'src/entities/datacampOffer/types';
import { RejectedParams, getParameterValue } from './RejectedParams';

const rejectedNumberParam = {
  param_id: 1,
  number_offer_value: 200,
  number_sku_value: 300,
  type: FormalizedParamType.NUMERIC,
};

const rejectedBoolParam = {
  param_id: 2,
  boolean_offer_value: true,
  boolean_sku_value: false,
  type: FormalizedParamType.BOOLEAN,
};

const rejectedOptionParam = {
  param_id: 3,
  option_offer_id: 12,
  option_sku_id: 13,
  type: FormalizedParamType.ENUM,
};

const rejectedNumberOptionParam = {
  param_id: 4,
  option_offer_id: 12,
  option_sku_id: 13,
  type: FormalizedParamType.NUMERIC_ENUM,
};

const params = [rejectedNumberParam, rejectedBoolParam, rejectedOptionParam, rejectedNumberOptionParam];

describe('<RejectedParams />', () => {
  params.forEach(param => {
    test(`render ${FormalizedParamType[param.type]}`, () => {
      const app = render(<RejectedParams params={[param]} />);
      const diffParam = getParameterValue(param);
      app.getByText(param.param_id);
      app.getByText(FormalizedParamType[param.type]);
      app.getByText(diffParam.offer!);
      app.getByText(diffParam.sku!);
    });
  });
});
