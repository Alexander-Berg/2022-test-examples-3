import {SchedulePO} from '../../../page-objects/SchedulePO'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

it('[webvendor-109] Выбор ресторана', async function () {
  await this.browser.authorize()
  const schedule = new SchedulePO(this.browser)
  await this.browser.$(UiSelectors.sidebarSection('График работы')).waitForDisplayed()
  await schedule.open()
  await schedule.waitForLoad()
  await this.browser.assertView('RestaurantSchedule', UiSelectors.page)

  await this.browser.$(UiSelectors.autocompleteInput).click()
  await schedule.waitForLoad()
  await this.browser.assertView('RestaurantScheduleMarketplace', UiSelectors.page)
})
