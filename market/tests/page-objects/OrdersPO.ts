import {OrdersSelectors} from './selectors.generated/OrdersSelectors'
import {POBase} from './POBase'
import {UiSelectors} from './selectors.generated/UiSelectors'

export class OrdersPO extends POBase {
  readonly pageName = 'Заказы'

  waitOrdersLoaded() {
    return this.browser.$(OrdersSelectors.listItem).waitForDisplayed()
  }

  get ordersList() {
    return this.browser.$$(OrdersSelectors.listItem)
  }

  get refreshButton() {
    return this.browser.$(`${UiSelectors.refreshButton}`)
  }

  get activeOrders() {
    return this.browser.$(`${OrdersSelectors.listWip}`)
  }

  get completedOrders() {
    return this.browser.$(`${OrdersSelectors.listCompleted}`)
  }

  get callbackButton() {
    return this.browser.$(`${OrdersSelectors.orderUtilityActions} [alt="callback"]`)
  }

  get printButton() {
    return this.browser.$(`${OrdersSelectors.orderUtilityActions} [alt="print"]`)
  }

  get chatButton() {
    return this.browser.$(OrdersSelectors.orderSupportChatButton)
  }

  ordersCount(): Promise<number> {
    return this.ordersList.length
  }

  async hasOrders(): Promise<boolean> {
    return Boolean(await this.ordersList.length)
  }

  //#region Order
  get primaryButton() {
    return this.browser.$(OrdersSelectors.buttonPrimaryAction)
  }

  get cancelButton() {
    return this.browser.$(OrdersSelectors.buttonCancel)
  }

  get confirmCancelButton() {
    return this.browser.$(OrdersSelectors.confirmCancelButton)
  }

  get cancellationReasonWidget() {
    return this.browser.$(OrdersSelectors.cancellationReasonWidget)
  }

  get statusBadge() {
    return this.browser.$(OrdersSelectors.orderBadgeStatus)
  }

  get compensationBadge() {
    return this.browser.$(OrdersSelectors.orderBadgeCompensationType)
  }

  get deliveryBadge() {
    return this.browser.$(OrdersSelectors.orderBadgeDeliveryType)
  }

  get orderPositions() {
    return this.browser.$$(OrdersSelectors.orderPositionRoot)
  }

  getOrderPropertyValue(selector: string) {
    return this.browser.$(selector).$(OrdersSelectors.orderPropertyValue).getText()
  }

  getOrderPropertyByLabel(label: string) {
    return this.browser.$(`${OrdersSelectors.orderPropertyValue}[data-label="${label}"]`).getText()
  }

  getItemById(orderId: string) {
    return this.browser.$(`${OrdersSelectors.listItem}[data-order-id="${orderId}"]`)
  }

  waitStatus(status: string) {
    return this.browser.waitUntil(
      async () => {
        await this.refreshButton.waitForClickable()
        await this.refreshButton.click()
        await this.refreshButton.waitForClickable()
        const actualStatus = await this.statusBadge.getText()

        return actualStatus === status
      },
      {timeout: 180000}
    )
  }

  async waitPennyConfigLoad() {
    const order = this.browser.$(OrdersSelectors.listItem)
    await order.waitForDisplayed()

    return this.browser.waitUntil(async () => {
      // Ждем пока доедут конфиги с копейками
      const price = await order.$(OrdersSelectors.listItemTitle).getText()

      return /\d+\.\d{2}/.test(price)
    })
  }

  //#endregion Order
}
