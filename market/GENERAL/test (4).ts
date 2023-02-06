import { run } from 'jest-cli';

import { jestDefaultConfig, mergeConfigs } from '../configs/jest/jest.config';
import { getMboCoreConfig } from '../configs/mboCoreConfigParser';

if (process.env.NODE_ENV == null) {
  process.env.NODE_ENV = 'test';
}

customRun();

['SIGINT', 'SIGTERM'].forEach(sig => {
  process.on(sig, () => {
    process.exit();
  });
});

if (process.env.CI !== 'true') {
  // Gracefully exit when stdin ends
  process.stdin.on('end', () => {
    process.exit();
  });
}

function customRun(): void {
  const argv = process.argv.slice(2);
  const mboCoreConfig = getMboCoreConfig();
  const finalConfig = mergeConfigs(jestDefaultConfig, mboCoreConfig.testConfig ?? {});

  argv.push(`--config="${JSON.stringify(finalConfig)}"`);
  run(argv);
}
