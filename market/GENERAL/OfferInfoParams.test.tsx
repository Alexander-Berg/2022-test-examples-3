import {
  getNormalizedCategoryData,
  getNormalizedModel,
  getModelParameterValuesByXslName,
  getRuLocalizedString,
} from '@yandex-market/mbo-parameter-editor';
import { mount } from 'enzyme';
import * as React from 'react';

import { testCategoryProto } from 'src/shared/test-data/test-categories';
import { testModelProto } from 'src/shared/test-data/test-models';
import { testOffer } from 'src/shared/test-data/test-offers';
import { testParameterProto } from 'src/shared/test-data/test-parameters';
import { Highlight } from 'src/tasks/mapping-moderation/components/Image/Highlight/Highlight';
import { OfferInfoParams } from 'src/tasks/mapping-moderation/components/OfferInfoParams/OfferInfoParams';
import { XslNames } from 'src/tasks/mapping-moderation/helpers/xsl-names';

describe('OfferInfoParams', () => {
  const setup = () => {
    const parameter = testParameterProto({ xsl_name: XslNames.BAR_CODE });
    const category = getNormalizedCategoryData(testCategoryProto({ parameters: [parameter] }));
    const model = getNormalizedModel(testModelProto({ categoryData: category }));
    const offer = testOffer();

    return { parameter, category, model, offer };
  };

  it('Should not highlight params if not equal', () => {
    const { offer, model, category } = setup();
    offer.barcode = 'different';

    const params = mount(<OfferInfoParams offer={offer} model={model} category={category} />);
    expect(params.find(Highlight)).toBeEmpty();
  });

  it('Should highlight params if equal', () => {
    const { offer, model, category } = setup();
    offer.barcode =
      getRuLocalizedString(getModelParameterValuesByXslName(model, XslNames.BAR_CODE, category)![0].stringValue)
        ?.value || '';

    const params = mount(<OfferInfoParams offer={offer} model={model} category={category} />);

    expect(params.find(Highlight)).toHaveLength(1);
    expect(params.find(Highlight).text()).toContain(offer.barcode);
  });

  it('Should not display expand if small param count', () => {
    const { offer, model, category } = setup();
    const params = mount(<OfferInfoParams offer={offer} model={model} category={category} />);
    expect(params.find('.OfferInfoParams-MoreLink')).toHaveLength(0);
  });
});
