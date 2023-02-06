import Context from '../Context'
import {LocalStorageStub} from 'core-legacy/test-utils/create-env'
import {LoggerStub, MetrikaStub} from 'shared/test-utils'
import {LOCAL_STORAGE_CONTEXT} from '../../constants'
import {getSnapshot} from 'mobx-state-tree'
import {LOCAL_STORAGE_MOCK} from './mock'

describe('Context', () => {
  const createEnv = () => ({logger: new LoggerStub(), metrika: new MetrikaStub(), localStorage: new LocalStorageStub()})

  test('save', () => {
    const env = createEnv()
    const context = Context.create({}, env)

    env.localStorage.set = jest.fn()

    context.save()
    expect(env.localStorage.set).toBeCalledWith(LOCAL_STORAGE_CONTEXT, getSnapshot(context))
  })

  test('restore', () => {
    const env = createEnv()
    const context = Context.create({}, env)

    env.localStorage.get = jest.fn().mockReturnValue(LOCAL_STORAGE_MOCK)

    context.restore()
    expect(env.localStorage.get).toBeCalledWith(LOCAL_STORAGE_CONTEXT)
    expect(getSnapshot(context)).toEqual(LOCAL_STORAGE_MOCK)
  })

  test('visit flags', () => {
    const context = Context.create({})

    expect(context.hasVisited({partnerId: '1', key: 'info'})).toBeFalsy()
    context.markVisited({partnerId: '1', key: 'info'})
    expect(context.hasVisited({partnerId: '1', key: 'info'})).toBeTruthy()
  })
})
