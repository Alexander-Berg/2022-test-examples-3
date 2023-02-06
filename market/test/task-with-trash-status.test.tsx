import { offersMappingsSelector } from 'src/tasks/common-logs/store/offers/selectors';
import simpleTask from 'src/tasks/common-logs/task-suits/test-simple-task';
import { initCommonLogsApp } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';
import { switchOnOffer } from 'src/tasks/common-logs/test/utils/switchOnOffer';
import { selectComment } from './utils/selectCommentOnOffer';
import { selectStatus } from './utils/selectStatus';
import { contentCommentTypes } from 'src/shared/test-data/content-comment-types';

describe('trash comment can be selected', () => {
  it('should select trash comment on offers', async () => {
    const { app, store } = initCommonLogsApp({ initialData: simpleTask });

    const commentToSelectType = contentCommentTypes[0].type;

    const offersIds = simpleTask.logs.map(o => o.offer_id);
    offersIds.forEach(offerId => {
      switchOnOffer(app, offerId);

      selectStatus(app);

      selectComment(app, commentToSelectType!);
    });

    const offerMappings = offersMappingsSelector(store.getState());
    const offerStatuses = Object.keys(offerMappings).map(offerId => offerMappings[offerId].map_status);
    const offerComments = Object.keys(offerMappings).map(
      offerId => offerMappings[offerId].formalised_comments![0].selectedComment
    );

    expect(Object.keys(offerMappings).length).toEqual(offerStatuses.length);
    expect(Object.keys(offerMappings).length).toEqual(
      offerComments.filter((comment: string) => comment === commentToSelectType).length
    );
  });
});
