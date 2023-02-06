import {render, screen, cleanup} from '@testing-library/react'

import {Sticker} from '../Sticker'

afterEach(cleanup)

describe('Sticker', () => {
  test('renders children', () => {
    render(<Sticker>children</Sticker>)
    screen.getByText('children')
  })
})
