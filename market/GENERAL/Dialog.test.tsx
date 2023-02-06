import React from 'react'
import {findByRole, findByTestId, fireEvent, render} from '@testing-library/react'
import Dialog from './Dialog'
import {DialogParams} from '../../types'

describe('Dialog', () => {
  const onClose = jest.fn()
  const onConfirm = jest.fn()

  beforeEach(() =>
    renderDialog({
      applyText: 'Принять',
      cancelText: 'Отменить'
    })
  )

  afterEach(() => {
    onClose.mockClear()
  })

  test('вызывает onClose при нажатии на кнопку "отменить"', async () => {
    const element = await findByTestId(document.body, 'ui__dialog-cancel-button')
    fireEvent.click(element)
    expect(onClose).toHaveBeenCalled()
    expect(onConfirm).not.toHaveBeenCalled()
  })

  test('вызывает onConfirm при нажатии на кнопку "принять"', async () => {
    const element = await findByTestId(document.body, 'ui__dialog-apply-button')
    fireEvent.click(element)
    expect(onConfirm).toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })

  test('по-умолчанию вызывает onClose при клике по фону', async () => {
    const element = await findByRole(document.body, 'presentation')

    fireEvent.click(element.children[0])
    expect(onClose).toHaveBeenCalled()
  })

  test('по-умолчанию вызывает onClose при нажатии по esc', async () => {
    const element = await findByRole(document.body, 'presentation')
    fireEvent.keyDown(element.children[0], {
      key: 'Escape',
      code: 'Escape',
      keyCode: 27,
      charCode: 27
    })
    expect(onClose).toHaveBeenCalled()
  })

  describe('при outsideClose=false', function () {
    beforeEach(() =>
      renderDialog({
        applyText: 'Принять',
        cancelText: 'Отменить',
        outsideClose: false
      })
    )

    test('не вызывает onClose при клике по фону', async () => {
      const element = await findByRole(document.body, 'presentation')

      fireEvent.click(element.children[0])
      expect(onClose).not.toHaveBeenCalled()
    })

    test('не вызывает onClose при нажатии по esc', async () => {
      const element = await findByRole(document.body, 'presentation')
      fireEvent.keyDown(element.children[0], {
        key: 'Escape',
        code: 'Escape',
        keyCode: 27,
        charCode: 27
      })
      expect(onClose).not.toHaveBeenCalled()
    })
  })

  function renderDialog(override: Partial<DialogParams> = {}) {
    return render(<Dialog visible progress={false} onConfirm={onConfirm} onClose={onClose} {...override} />)
  }
})
