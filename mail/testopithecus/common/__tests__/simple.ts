import { TestsRegistry } from '../code/tests/register-your-test-here';
import { Registry } from '../code/utils/registry';
import { ConsoleLog } from './pod/console-log';

describe('Should run simple test', () => {
  setUpRegistry()

  it('like that', (done) => {
    Registry.get().logger!.log('Some text')
    done()
  });
});

describe('Run tests with map reduce', () => {
  setUpRegistry()

  const registry: TestsRegistry = new TestsRegistry(ConsoleLog.LOGGER)
  const testNames: string[] = []
  for (const test of registry.getAllTests().filter(t => testNames.includes(t.description))) {
    Registry.get().logger!.log(test.description)
  }
});

function setUpRegistry() {
  Registry.get().logger = ConsoleLog.LOGGER;
}
