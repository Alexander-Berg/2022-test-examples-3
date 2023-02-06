import { ReactWrapper } from 'enzyme';

import { Carousel } from 'src/tasks/common-logs/components/Carousel/Carousel';

export const switchOnOffer = (app: ReactWrapper, offerId: string) => {
  app
    .find(Carousel)
    .findWhere(item => item.props().offerId === offerId)
    .find('.CarouselElement')
    .simulate('click');

  app.update();
};
