import { Model } from '@yandex-int/duffman';

import { PublicModels, TModelResultsMap, TModelParamsMap } from '../../../common/models';
import { Services } from '../../constants/services';

type TModelParams = TModelParamsMap[PublicModels.TestSendTask];
type TModelResult = TModelResultsMap[PublicModels.TestSendTask];

export class TestSendTask extends Model<TModelParams, TModelResult> {
    async action(params: TModelParams, core: App.Core): Promise<TModelResult> {
        return core.service(Services.Fan)('/test-send-task',
            { method: 'POST' },
            {
                body: {
                    recipients: params.recipients,
                    user_template_variables: params.user_template_variables ?? {},
                },
                query: {
                    campaign_slug: params.campaign_slug,
                    account_slug: params.project_slug,
                },
            },
        );
    }
}
