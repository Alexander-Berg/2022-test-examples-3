import { FrontendConfig, ProcessingStatus } from 'src/java/definitions';
import { processConfig, ProcessedConfig } from 'src/utils/processConfig';

export const testFrontendConfig = (config: Partial<FrontendConfig> = {}): ProcessedConfig =>
  processConfig({
    idmUrl: '/idm',
    mboUrl: 'https://mbo.url/',
    rolesDescription: {},
    contentCommentDescriptions: [],
    processingStatuses: [
      { value: ProcessingStatus.OPEN, title: 'Открыт' },
      { value: ProcessingStatus.PROCESSED, title: 'Обработан' },
    ],
    ...config,
  });
