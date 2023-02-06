import { CommandsQueueRaker } from '../../src/xplat/common/code/api/commands-queue-raker'
import { Command } from '../../src/xplat/common/code/api/entities/command'
import { JSONItem, MapJSONItem } from '../../src/xplat/common/code/api/json/json-types'
import { Network } from '../../src/xplat/common/code/api/network/network'
import { Result } from '../../src/xplat/common/code/api/result'
import { getVoid } from '../../src/xplat/common/code/api/result'
import { CommandEntity, CommandsQueue } from '../../src/xplat/common/code/api/storage/commands-queue'
import { Storage } from '../../src/xplat/common/code/api/storage/storage'
import { Registry } from '../../src/xplat/common/code/registry'
import { RuntimeClassInfo } from '../../src/xplat/common/code/utils/runtime-info'
import { promiseError, reject, resolve } from '../../src/xplat/common/ys/xpromise-support'
import { Decodable, Encodable, int64 } from '../../src/xplat/common/ys/ys'

const storage: Storage = {
  runQuery: jest.fn(),
  runStatement: jest.fn(),
  prepareStatement: jest.fn(),
  withinTransaction: jest.fn(),
  close: jest.fn(),
  drop: jest.fn(),
  notifyAboutChanges: jest.fn(),
}

const network: Network = {
  execute: jest.fn(),
  executeGettingObjects: jest.fn(),
  resolveURL: jest.fn(),
}

Registry.registerJSONSerializer({
  deserializeDecodable<T extends Decodable>(_: RuntimeClassInfo, item: string): Result<T> {
    try {
      return new Result(JSON.parse(item) as T, null)
    } catch (e) {
      return new Result<T>(null, e)
    }
  },
  serializeEncodable<T extends Encodable>(item: T): Result<string> {
    try {
      return new Result(JSON.stringify(item), null)
    } catch (e) {
      return new Result<string>(null, e)
    }
  },
  serialize(item: JSONItem) {
    throw new Error('Not Implemented')
  },
  deserialize<T>(item: string, materializer: (jsonItem: JSONItem) => Result<T>) {
    throw new Error('Not Implemented')
  },
})

const commandsQueue = new CommandsQueue(storage)

describe.skip(CommandsQueueRaker, () => {
  afterEach(jest.restoreAllMocks)
  it('on empty commands queue should complete processing', async () => {
    Registry.setNetworkStatus(true)
    const lengthSpy = jest.spyOn(commandsQueue, 'length').mockReturnValue(resolve(0))

    const commandsQueueRaker = new CommandsQueueRaker(commandsQueue, network)

    await commandsQueueRaker.raker()
    expect(lengthSpy).toBeCalled()
  })

  it('on get command with more than maximum retries should mark failure', async () => {
    Registry.setNetworkStatus(true)
    const getSpy = jest.spyOn(commandsQueue, 'get')
      .mockReturnValue(resolve(null))
      .mockReturnValueOnce(
        resolve(new CommandEntity(int64(1), new Command('message', 'get', '{"mids" : ["1", "2", "3"]}', int64(0)), 3)),
      )
    jest.spyOn(commandsQueue, 'length').mockReturnValue(resolve(1))
    const purgeSpy = jest.spyOn(commandsQueue, 'purge').mockReturnValue(resolve(getVoid()))
    const setStatusFailureSpy = jest.spyOn(commandsQueue, 'setStatusFailure').mockReturnValue(resolve(getVoid()))

    const commandsQueueRaker = new CommandsQueueRaker(commandsQueue, network)

    await commandsQueueRaker.raker()
    expect(purgeSpy).toBeCalled()
    expect(getSpy).toBeCalledTimes(2)
    expect(setStatusFailureSpy).toBeCalledWith(1)
  })

  it('on good response should mark command complete', async () => {
    Registry.setNetworkStatus(true)
    const fetch = jest.spyOn(network, 'execute').mockReturnValue(resolve(new MapJSONItem()))
    const getSpy = jest.spyOn(commandsQueue, 'get')
      .mockReturnValue(resolve(null))
      .mockReturnValueOnce(
        resolve(new CommandEntity(int64(1), new Command('message', 'get', '{"mids" : ["1", "2", "3"]}', int64(0)), 0)),
      )
    jest.spyOn(commandsQueue, 'length').mockReturnValue(resolve(1))
    const purgeSpy = jest.spyOn(commandsQueue, 'purge').mockReturnValue(resolve(getVoid()))
    const setStatusCompleteSpy = jest.spyOn(commandsQueue, 'setStatusComplete').mockReturnValue(resolve(getVoid()))
    const increaseTriesSpy = jest.spyOn(commandsQueue, 'increaseTries').mockReturnValue(resolve(getVoid()))

    const commandsQueueRaker = new CommandsQueueRaker(commandsQueue, network)

    await commandsQueueRaker.raker()
    expect(purgeSpy).toBeCalled()
    expect(getSpy).toBeCalledTimes(2)
    expect(setStatusCompleteSpy).toBeCalledWith(1)
    expect(fetch).toBeCalledTimes(1)
    expect(increaseTriesSpy).not.toBeCalled()
  })

  it('on bad response should increase the number of retries', async () => {
    Registry.setNetworkStatus(true)
    const fetch = jest.spyOn(network, 'execute')
      .mockReturnValue(resolve(new MapJSONItem()))
      .mockReturnValueOnce(reject(promiseError('ERROR')))
    const getSpy = jest.spyOn(commandsQueue, 'get')
      .mockReturnValue(resolve(null))
      .mockReturnValueOnce(
        resolve(new CommandEntity(int64(1), new Command('message', 'get', '{"mids" : ["1", "2", "3"]}', int64(0)), 0)),
      )
      .mockReturnValueOnce(
        resolve(new CommandEntity(int64(1), new Command('message', 'get', '{"mids" : ["1", "2", "3"]}', int64(0)), 1)),
      )
    jest.spyOn(commandsQueue, 'length').mockReturnValue(resolve(1))
    const purgeSpy = jest.spyOn(commandsQueue, 'purge').mockReturnValue(resolve(getVoid()))
    const increaseTriesSpy = jest.spyOn(commandsQueue, 'increaseTries').mockReturnValue(resolve(getVoid()))
    const setStatusCompleteSpy = jest.spyOn(commandsQueue, 'setStatusComplete').mockReturnValue(resolve(getVoid()))

    const commandsQueueRaker = new CommandsQueueRaker(commandsQueue, network)

    await commandsQueueRaker.raker()
    expect(purgeSpy).toBeCalled()
    expect(getSpy).toBeCalledTimes(3)
    expect(increaseTriesSpy).toBeCalledWith(1)
    expect(setStatusCompleteSpy).toBeCalledWith(1)
    expect(fetch).toBeCalledTimes(2)
  })

  it('on error in parsing command should not repeat it', async () => {
    Registry.setNetworkStatus(true)
    const getSpy = jest.spyOn(commandsQueue, 'get')
      .mockReturnValue(resolve(null))
      .mockReturnValueOnce(
        resolve(new CommandEntity(int64(1), new Command('message', 'get', '', int64(0)), 0)),
      )
    jest.spyOn(commandsQueue, 'length').mockReturnValue(resolve(1))
    const purgeSpy = jest.spyOn(commandsQueue, 'purge').mockReturnValue(resolve(getVoid()))
    const setStatusFailureSpy = jest.spyOn(commandsQueue, 'setStatusFailure').mockReturnValue(resolve(getVoid()))
    const increaseTriesSpy = jest.spyOn(commandsQueue, 'increaseTries').mockReturnValue(resolve(getVoid()))

    const commandsQueueRaker = new CommandsQueueRaker(commandsQueue, network)

    await commandsQueueRaker.raker()
    expect(purgeSpy).toBeCalled()
    expect(getSpy).toBeCalledTimes(2)
    expect(increaseTriesSpy).not.toBeCalled()
    expect(setStatusFailureSpy).toBeCalledWith(1)
  })

  it('on offline should complete processing', async () => {
    Registry.setNetworkStatus(true)
    const getSpy = jest.spyOn(commandsQueue, 'get')
      .mockReturnValue(
        resolve(new CommandEntity(int64(1), new Command('message', 'get', '{"mids" : ["1", "2", "3"]}', int64(0)), 1)),
      )
    jest.spyOn(commandsQueue, 'length').mockReturnValue(resolve(1))
    const purgeSpy = jest.spyOn(commandsQueue, 'purge').mockReturnValue(resolve(getVoid()))

    const commandsQueueRaker = new CommandsQueueRaker(commandsQueue, network)

    await commandsQueueRaker.raker()
    expect(purgeSpy).toBeCalled()
    expect(getSpy).toBeCalledTimes(1)
  })
})
