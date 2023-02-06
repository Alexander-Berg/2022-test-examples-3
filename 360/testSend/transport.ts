import { PublicModels } from '@/../common/models';
import { getModelsApi, TTransportModel } from '@/utils/transport';

export type Models = {
    [PublicModels.TestSendTask]: TTransportModel<PublicModels.TestSendTask>;
};

export const campaignsTransport = getModelsApi<Models>();

export function requestTestSendTask(
    project_slug: string,
    campaign_slug: string,
    recipients: string[],
    user_template_variables?: Record<string, string>,
) {
    const modelsRequestParams = [
        { name: PublicModels.TestSendTask, params: {
            project_slug, campaign_slug, recipients, user_template_variables,
        } },
    ] as const;

    return campaignsTransport.request(modelsRequestParams);
}

export const isModelResolved = campaignsTransport.isModelResolved.bind(campaignsTransport);
