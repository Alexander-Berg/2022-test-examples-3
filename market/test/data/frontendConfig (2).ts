import { FrontendConfig } from 'src/java/definitions';

export const testFrontendConfig = (config: Partial<FrontendConfig> = {}): FrontendConfig => {
  return {
    idmUrl: '/idm',
    mboUrl: 'https://mbo.url/',
    rolesDescription: {},
    ...config,
  };
};
