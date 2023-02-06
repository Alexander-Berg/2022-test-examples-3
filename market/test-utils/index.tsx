import React, {PropsWithChildren} from 'react'
import {IEnv} from 'core-legacy/types'
import AppState, {AppStateType} from 'core-legacy/AppState'
import {Provider} from 'core-di'
import {Router} from 'react-router-dom'
import {createBrowserHistory, History} from 'history'
import {render, RenderOptions, RenderResult} from '@testing-library/react'
import {SnapshotIn} from 'mobx-state-tree'
import {getMinimalApiMock} from 'core-legacy/test-utils/minimalApiMock'
import {withIdProvider} from 'core-legacy/hocs/provide-id'
import {StylesProvider} from '@material-ui/core/styles'
import {Rule, StyleSheet} from 'jss'
import {VendorApi} from 'core-legacy/types/restyped'
import createEnv, {TestingEnv} from 'core-legacy/test-utils/create-env'
import ApiScenario from 'core-legacy/api/mock/ApiScenario'

interface ProvidersProps {
  appState: AppStateType
  env: IEnv
  history: History
  generateClassName?: GenerateClassName
}

interface Options {
  stateSnapshot?: SnapshotIn<typeof AppState>
  envOverrides?: Partial<RenderWithProvidersEnv>
  renderOptions?: Omit<RenderOptions, 'queries' | 'wrapper'>
  history?: History
  extendApiMock?(api: ApiScenario<VendorApi>): void
}

export interface RenderWithProvidersEnv extends TestingEnv {
  api: ApiScenario<VendorApi>
}

interface RenderWithProvidersResult extends RenderResult {
  appState: AppStateType
  env: RenderWithProvidersEnv
  history: History
  api: ApiScenario<VendorApi>
}

/**
 * Utility function that mocks the `IntersectionObserver` API. Necessary for components that rely
 * on it, otherwise the tests will crash. Recommended to execute inside `beforeEach`.
 * @param intersectionObserverMock - Parameter that is sent to the `Object.defineProperty`
 * overwrite method. `jest.fn()` mock functions can be passed here if the goal is to not only
 * mock the intersection observer, but its methods.
 */
export function setupIntersectionObserverMock({
  root = null,
  rootMargin = '',
  thresholds = [],
  disconnect = () => null,
  observe = () => null,
  takeRecords = () => null,
  unobserve = () => null
} = {}): void {
  class IntersectionObserverMock implements IntersectionObserver {
    readonly root: Element | null = root
    readonly rootMargin: string = rootMargin
    readonly thresholds: ReadonlyArray<number> = thresholds
    disconnect: () => void = disconnect
    observe: (target: Element) => void = observe
    // @ts-ignore
    takeRecords: () => IntersectionObserverEntry[] = takeRecords
    unobserve: (target: Element) => void = unobserve
  }

  Object.defineProperty(window, 'IntersectionObserver', {
    writable: true,
    configurable: true,
    value: IntersectionObserverMock
  })

  Object.defineProperty(global, 'IntersectionObserver', {
    writable: true,
    configurable: true,
    value: IntersectionObserverMock
  })
}

type GenerateClassName = (rule: Rule, sheet?: StyleSheet<string>) => string

const customClassNameGenerator: GenerateClassName = (rule, styleSheet) =>
  `${styleSheet?.options.classNamePrefix}-${rule.key}`

function renderWithStaticClassnames(ui: React.ReactElement) {
  return render(ui, {
    wrapper: ({children}) => <StylesProvider generateClassName={customClassNameGenerator}>{children}</StylesProvider>
  })
}

export const Providers = withIdProvider(
  ({children, appState, env, history, generateClassName}: PropsWithChildren<ProvidersProps>) => {
    return (
      <StylesProvider generateClassName={generateClassName}>
        <Provider appState={appState} env={env}>
          <Router history={history}>{children}</Router>
        </Provider>
      </StylesProvider>
    )
  }
)

export async function createProvidersWrapper({
  extendApiMock,
  stateSnapshot = {auth: {token: 'token', directToken: 'directToken'}, exp3: {configs: {}}},
  envOverrides,
  history = createBrowserHistory()
}: Options = {}) {
  const api = getMinimalApiMock()
  extendApiMock && extendApiMock(api)

  const env = {
    ...(createEnv({api}) as RenderWithProvidersEnv),
    ...envOverrides
  }
  const appState: AppStateType = AppState.create(stateSnapshot, env)
  await appState.init()
  await appState.initData()

  api.mock.resetHistory()

  const wrapper: React.ComponentType<{}> = ({children}) => (
    <Providers appState={appState} env={env} history={history} generateClassName={customClassNameGenerator}>
      {children}
    </Providers>
  )

  return {wrapper, appState, env, history, api}
}

export const StorybookProvider = ({
  children,
  snapshot = {auth: {token: 'token'}}
}: {
  children: React.ReactNode
  snapshot?: SnapshotIn<typeof AppState>
}) => {
  const api = getMinimalApiMock()

  const env = {
    ...(createEnv({api}) as RenderWithProvidersEnv)
  }
  const appState: AppStateType = AppState.create(snapshot, env)

  return (
    <Providers appState={appState} env={env} history={createBrowserHistory()}>
      {children}
    </Providers>
  )
}

async function renderWithProviders(
  ui: React.ReactElement,
  {extendApiMock, stateSnapshot, envOverrides, renderOptions, history: historyParam}: Options = {}
): Promise<RenderWithProvidersResult> {
  const {wrapper, appState, env, history, api} = await createProvidersWrapper({
    extendApiMock,
    stateSnapshot,
    envOverrides,
    history: historyParam
  })

  return {
    ...render(ui, {
      wrapper,
      ...renderOptions
    }),
    appState,
    env,
    history,
    api
  }
}

// re-export everything
export * from '@testing-library/react'

export {default as serializeModel} from './serialize-model'

// override render method
export {renderWithProviders, renderWithStaticClassnames as render}
