import { getValueYaCommand } from './utils';

const testOptions = {
  id: 'sec-01ehyck1',
  yaPath: '/user/',
};

describe('ya-vault-utils', () => {
  test('getValueYaCommand', () => {
    const command = getValueYaCommand(testOptions);

    expect(command).toBe('/user/ya vault get version sec-01ehyck1 -o ');
  });
});
