import { ReactWrapper } from 'enzyme';

import { OfferComponent } from 'src/tasks/common-logs/components/Offer/Offer';
import { StatusButtons } from 'src/tasks/common-logs/components/StatusButtons/StatusButtons';

export const selectStatus = (app: ReactWrapper, statusTitle = 'Недостаточно данных') => {
  const el = app
    .find(OfferComponent)
    .find(StatusButtons)
    .findWhere(item => item.text() === statusTitle)
    .first();

  el.simulate('click');

  app.update();
};
