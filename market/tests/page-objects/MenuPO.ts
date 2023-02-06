import {POBase} from './POBase'
import {MenuSelectors} from './selectors.generated/MenuSelectors'
import {UiSelectors} from './selectors.generated/UiSelectors'

export class MenuPO extends POBase {
  readonly pageName = 'Меню'

  protected editCategoryModalPO = {
    clickCustomCategoryRadio: () => {
      return this.browser.$(MenuSelectors.radioButtonCustom).click()
    },

    clickPredefinedCategoryRadio: () => {
      return this.browser.$(MenuSelectors.radioButtonPrimary).click()
    },

    openCategoriesListSelect: async () => {
      await this.browser.$(MenuSelectors.categoriesListSelect).waitForDisplayed()
      return this.browser.$(MenuSelectors.categoriesListSelect).click()
    },

    clickCategoriesSelectOption: (categoryName: string) => {
      return this.browser.$(MenuSelectors.categoriesListOption(categoryName)).click()
    },

    enterCategoryName: async (categoryName: string) => {
      await this.browser.clearValue(MenuSelectors.inputCategoryName)
      return this.browser.$(MenuSelectors.inputCategoryName).setValue(categoryName)
    },

    isSubmitDisabled: async (): Promise<boolean> => {
      return !(await this.browser.$(MenuSelectors.editCategorySubmitBtn).isEnabled())
    },

    submitCategory: async () => {
      await this.browser.$(MenuSelectors.editCategorySubmitBtn).click()
      return this.editPositionPO
    },

    close: async () => {
      await this.browser.$(UiSelectors.modalClose).click()
      return this.browser.waitForHidden(UiSelectors.modal, 3000)
    }
  }

  protected categoryPO = {
    openCategoryContextMenu: async () => {
      await this.browser.$(MenuSelectors.itemContextMenu).waitForDisplayed()
      return this.browser.$(MenuSelectors.itemContextMenu).click()
    },
    editCategory: async () => {
      await this.categoryPO.openCategoryContextMenu()
      await this.browser.$(MenuSelectors.categoryContextEdit).click()
      return this.editCategoryModalPO
    },
    deleteCategory: async () => {
      await this.categoryPO.openCategoryContextMenu()
      return this.browser.$(MenuSelectors.categoryContextDelete).click()
    },
    openPositionContextMenu: async (positionName: string) => {
      await this.browser.$(MenuSelectors.itemContextMenu).waitForDisplayed()
      return this.browser.$(`${MenuSelectors.item(positionName)} ${MenuSelectors.itemContextMenu}`).click()
    },
    editPosition: async (positionName: string) => {
      await this.categoryPO.openPositionContextMenu(positionName)
      await this.browser.$(MenuSelectors.itemContextEdit).click()
      return this.editPositionPO
    },
    deletePosition: async (positionName: string) => {
      await this.categoryPO.openPositionContextMenu(positionName)
      return this.browser.$(MenuSelectors.itemContextDelete).click()
    },
    addPosition: async () => {
      await this.browser.$(MenuSelectors.categoryAddPosition).click()
      return this.editPositionPO
    },
    toggleExpandPosition: (positionName: string) => {
      return this.browser.$(`${MenuSelectors.item(positionName)} ${MenuSelectors.categoryPositionExpand}`).click()
    },
    selectPositionCheckbox: (positionName: string) => {
      return this.browser.$(`${MenuSelectors.item(positionName)} ${MenuSelectors.categoryItemCheckbox}`).click()
    },
    getActiveCategoryLength: async () => {
      const activeCategoryLength = await this.browser.$(MenuSelectors.activeCategoriesLength).getText()

      return Number(activeCategoryLength)
    },
    getDisableCategoryLength: async () => {
      const disableCategoriesLength = await this.browser.$(MenuSelectors.disableCategoriesLength).getText()

      return Number(disableCategoriesLength)
    },
    getDishCountInCategory: async () => {
      const categoryDishCount = await this.browser.$(MenuSelectors.categoryDishCount).getText()

      return Number(categoryDishCount)
    },
    getCheckedCheckbox: async () => {
      return await this.browser.$(MenuSelectors.categoryCheckboxChecked).isDisplayed()
    },
    getSemiBlackCheckbox: async () => {
      return await this.browser.$(MenuSelectors.categoryCheckboxSemiBlack).isDisplayed()
    }
  }

  protected editPositionPO = {
    enterName: async (name: string) => {
      await this.browser.clearValue(MenuSelectors.positionNameInput)
      return this.browser.$(MenuSelectors.positionNameInput).setValue(name)
    },
    enterContent: async (content: string) => {
      await this.browser.clearValue(MenuSelectors.positionContentInput)
      return this.browser.$(MenuSelectors.positionContentInput).setValue(content)
    },
    uploadPhoto: async (imagePath: string) => {
      const filePath = await this.browser.uploadFile(imagePath)
      await this.browser.$(UiSelectors.fileUpload).click()
      await this.browser.$(UiSelectors.fileUploadInput).setValue(filePath)
    },
    enterWeight: async (weight: string) => {
      await this.browser.clearValue(MenuSelectors.positionWeightInput)
      return this.browser.$(MenuSelectors.positionWeightInput).setValue(weight)
    },
    selectWeightMeasureUnit: async (unit: 'Грамм' | 'Килограмм' | 'Литров' | 'Миллилитров') => {
      await this.browser.$(MenuSelectors.positionWeightMeasureSelect).click()
      await this.browser.$(MenuSelectors.positionWeightMeasureOption(unit)).click()
      await this.browser.waitForHidden('[role="presentation"]', 2000)
    },
    enterPrice: async (price: string) => {
      // У инпута с ценой странное поведение
      await this.browser.clearValue(MenuSelectors.positionPriceInput)
      await this.browser.clearValue(MenuSelectors.positionPriceInput)
      return this.browser.$(MenuSelectors.positionPriceInput).setValue(price)
    },
    selectVat: async (vat: '10%' | '20%' | 'Не облагается') => {
      await this.browser.$(MenuSelectors.positionVatSelect).click()
      await this.browser.$(MenuSelectors.positionVatOption(vat)).click()
      await this.browser.waitForHidden('[role="presentation"]', 2000)
    },
    async fill({
      name = 'Оливье',
      content = 'Картошка, колбаса, огурец...',
      weight = '250',
      weightMeasure = 'Грамм',
      price = '350,00',
      vat = 'Не облагается'
    } = {}) {
      await this.enterName(name)
      await this.enterContent(content)
      await this.enterWeight(weight)
      await this.selectWeightMeasureUnit(weightMeasure)
      await this.enterPrice(price)
      await this.selectVat(vat)
    },

    addOptionsGroup: async () => {
      const optionsGroup = this.browser.$(MenuSelectors.positionAddOptionsGroupBtn)
      await optionsGroup.scrollIntoView()
      // await this.browser.wait(50)
      await optionsGroup.click()
    },
    enterOptionGroupName: (name: string) => {
      return this.browser.$(MenuSelectors.positionOptionsGroupNameInput).setValue(name)
    },
    addOption: () => {
      return this.browser.$(MenuSelectors.positionAddOptionBtn).click()
    },
    removeOptionGroup: () => {
      return this.browser.$(MenuSelectors.positionOptionsGroupRemoveBtn).click()
    },
    toggleOptionRequired: () => {
      return this.browser.$(MenuSelectors.positionOptionRequiredCheckbox).click()
    },
    enterOptionName: async (name: string) => {
      await this.browser.clearValue(MenuSelectors.positionOptionNameInput)
      return this.browser.$(MenuSelectors.positionOptionNameInput).setValue(name)
    },
    enterOptionPrice: async (price: string) => {
      await this.browser.clearValue(MenuSelectors.positionOptionPriceInput)
      return this.browser.$(MenuSelectors.positionOptionPriceInput).setValue(price)
    },

    submit: () => {
      return this.browser.$(MenuSelectors.positionSubmitBtn).click()
    },
    isSubmitDisabled: async (): Promise<boolean> => {
      return !(await this.browser.$(MenuSelectors.positionSubmitBtn).isEnabled())
    }
  }

  async selectCategory(name: string) {
    await this.browser.$(MenuSelectors.category(name)).click()
    return this.categoryPO
  }

  async toggleSelectedCategory() {
    return this.browser.$(MenuSelectors.categoryCheckbox).click()
  }

  async waitForLoad() {
    return this.browser.$(MenuSelectors.activeCategoriesList).waitForDisplayed({timeout: 15000})
  }

  async clickSaveButton() {
    return this.browser.$(MenuSelectors.saveBtn).click()
  }

  async clickAddCategory() {
    await this.browser.$(MenuSelectors.addCategory).click()
    return this.editCategoryModalPO
  }

  async open(): Promise<void> {
    await this.browser.wait(1500) // FIXME временный фикс из-за бага с меню. Не уходит реквест если после логина моментально переходить в меню. Раньше такой же wait был в команде authorize, поэтому работало.
    await super.open()
  }
}
