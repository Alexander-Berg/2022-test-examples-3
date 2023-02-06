import * as assert from 'assert';
import { UserParametersUserPool } from '../code/users/user-parameters-pool';
import { UserService } from '../code/users/user-service';
import { UserServicePool } from '../code/users/user-service-pool';
import { int64 } from '../ys/ys'
import { ConsoleLog } from './pod/console-log';
import { DefaultJSONSerializer } from './pod/default-json';
import { PRIVATE_BACKEND_CONFIG } from './private-backend-config';
import { createNetworkClient, createSyncNetwork} from './test-utils';

describe('mobile api distributed lock', () => {
  const client = createNetworkClient();

  const userService = new UserService(createSyncNetwork(), new DefaultJSONSerializer(), ConsoleLog.LOGGER)
  const poolsToTest = [
    // new UserParametersUserPool(client, PRIVATE_BACKEND_CONFIG.account, ConsoleLog.LOGGER), OLD users pool
    new UserServicePool(userService, 'unittest'),
  ]

  for (const userPool of poolsToTest) {
    beforeEach((done) => {
      userPool.reset()
      done()
    });

    it('should lock', (done) => {
      const lock1 = userPool.tryAcquire(int64(100), int64(10000))
      const lock2 = userPool.tryAcquire(int64(100), int64(10000))
      assert.strictEqual(lock2, null);
      lock1!.release()
      const lock3 = userPool.tryAcquire(int64(100), int64(100))
      assert.strictEqual(lock3 !== null, true)
      done()
    });

    it('should release lock after ttl', (done) => {
      const lock1 = userPool.tryAcquire(int64(100), int64(1000))
      const lock2 = userPool.tryAcquire(int64(2000), int64(1000))
      assert.strictEqual(lock2 !== null, true)
      done()
    });

    it('should ping lock', (done) => {
      const lock1 = userPool.tryAcquire(int64(100), int64(0))
      lock1!.ping(int64(2000))
      const lock2 = userPool.tryAcquire(int64(1000), int64(0))
      assert.strictEqual(lock2, null);
      done()
    });
  }
});
