import {Context} from 'react'
import {IEnv} from 'core-legacy/types'
import {EnvContext} from 'core-di/context'

export const EnvContextTest = EnvContext as Context<Partial<IEnv> | null>
