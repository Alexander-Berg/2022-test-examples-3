import { Store } from '@reatom/core'
import { context } from '@reatom/react'
import { act, fireEvent, render } from '@testing-library/react'
import { createBrowserHistory } from 'history'
import React from 'react'
import App from '@/app/app'
import { store } from '@/store/store'

const history = createBrowserHistory()

jest.mock('@/utils/logger')
jest.mock('@/common/fsm/windowRedirect', () => ({
    windowRedirect: jest.fn().mockImplementation((url: string, searchParams: Record<string, string> = {}) => {
        const newSearchParams = new URLSearchParams(window.location.search)
        Object.entries(searchParams).forEach(([key, value]) => newSearchParams.set(key, value))
        history.push({
            pathname: url,
            search: newSearchParams.toString(),
        })
    }),
}))

const flushPromises = () => Promise.resolve()

describe('App', () => {
    const Wrapper = () => (
        <context.Provider value={store as Store}>
            <App appHistory={history} />
        </context.Provider>
    )

    it('renders correctly', () => {
        expect(() => {
            render(<Wrapper />)
        }).not.toThrowError()
    })

    it('Check menu render', async () => {
        history.push('/ui/')
        const { getByTestId } = render(<Wrapper />)
        await act(async () => {
            await flushPromises()
        })
        expect(getByTestId('menu-list')).toMatchSnapshot()
        const inbound = getByTestId('1')
        expect(inbound).toBeInTheDocument()
        fireEvent.click(inbound)
        const receiving = getByTestId('11')
        expect(receiving).toBeInTheDocument()
        fireEvent.click(receiving)

        expect(window.location.pathname).toEqual('/ui/inbound/receiving/tableInput')
    })
})
