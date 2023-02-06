import { shallow } from 'enzyme';
import * as React from 'react';

import { testCategoryUi, testModelUi } from 'src/shared/test-data/test-ui';
import { Images } from 'src/tasks/mapping-moderation/components/Images/Images';
import { OfferInfoDumb } from 'src/tasks/mapping-moderation/components/OfferInfo/OfferInfo';
import { testModerationOffer } from 'src/tasks/mapping-moderation/test-data/test-moderation-offer';

describe('OfferInfo', () => {
  it('Should not display images in case none given', () => {
    const offer = testModerationOffer();
    const offerInfo = shallow(<OfferInfoDumb model={testModelUi()} offer={offer} category={testCategoryUi()} />);
    expect(offerInfo).not.toContainMatchingElement('Images');
  });

  it('Should display images', () => {
    const offer = testModerationOffer();
    offer.pictures = ['http://some1.jpeg', 'http://some2.jpeg'];
    const offerInfo = shallow(<OfferInfoDumb model={testModelUi()} offer={offer} category={testCategoryUi()} />);
    expect(offerInfo).toContainMatchingElement('Images');
    expect(offerInfo.find(Images).prop('urls')).toEqual(['http://some1.jpeg', 'http://some2.jpeg']);
  });
});
