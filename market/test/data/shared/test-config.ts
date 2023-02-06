import { Config } from 'src/java/definitions';

export function testConfig(config: Partial<Config>): Config {
  return {
    roles: [],
    login: '',
    uid: 0,
    ...config,
  };
}
