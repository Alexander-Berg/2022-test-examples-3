import React from 'react'
import {render, waitForElementToBeRemoved, fireEvent} from '@testing-library/react'
import Popover from './Popover'
import Button from 'shared-ui/Button'
import userEvent from '@testing-library/user-event'

const CONTROL_TEXT = 'Клац'
const POPOVER_CONTENT = 'Контент поповера'

describe('Popover', () => {
  it('Показывает/убирает поповер', async () => {
    const {getByRole, queryByText, findByText} = render(
      <Popover control={(handleClick) => <Button onClick={handleClick}>{CONTROL_TEXT}</Button>}>
        {POPOVER_CONTENT}
      </Popover>
    )

    const control = await findByText(CONTROL_TEXT)

    userEvent.click(control)

    expect(queryByText(POPOVER_CONTENT)).toBeInTheDocument()

    const popoverBackground = getByRole('presentation').firstChild!

    fireEvent.click(popoverBackground)

    await waitForElementToBeRemoved(() => queryByText(POPOVER_CONTENT))
  })
})
