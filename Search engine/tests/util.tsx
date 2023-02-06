import * as React from 'react'
import { Provider } from 'react-redux'
import { StaticRouter } from 'react-router-dom'

import { mount } from 'enzyme'

import { store } from '../src/js/store'


export const mountWithProvider = (data: JSX.Element) => mount(
    <StaticRouter context={{}}>
        <Provider store={ store }>
            { data }
        </Provider>
    </StaticRouter>,
)
