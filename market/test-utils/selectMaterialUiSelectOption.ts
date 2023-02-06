import {fireEvent} from 'core-legacy/test-utils/index'
import {within} from '@testing-library/react'

// TODO: Проверить для multiselect. Возможно потребуется эмуляция закрытия селекта
export const selectMaterialUiSelectOption = async (select: HTMLElement, optionText: string) => {
  const div = select.querySelector('[role=button]')
  fireEvent.mouseDown(div!)
  const list = document.body.querySelector('ul[role=listbox]')
  const listItem = within(list as any).getByText(optionText)
  fireEvent.click(listItem)
  await within(select).findByText(optionText)
}
