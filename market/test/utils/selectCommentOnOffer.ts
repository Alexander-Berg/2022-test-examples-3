import { ReactWrapper } from 'enzyme';

import { CommentEditor } from 'src/shared/common-logs/components/CommentEditor';
import { OfferComponent } from 'src/tasks/common-logs/components/Offer/Offer';
import { Select } from 'src/shared/components';

export const selectComment = (app: ReactWrapper, commentType: string) => {
  const select = app.find(OfferComponent).find(CommentEditor).find(Select);

  select.props().onChange!({ label: commentType, value: commentType } as any);

  app.update();
};
