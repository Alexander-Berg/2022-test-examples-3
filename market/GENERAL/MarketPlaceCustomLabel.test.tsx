import {renderWithProviders} from 'core-legacy/test-utils'
import MarketPlaceCustomLabel from './MarketPlaceCustomLabel'
import type {Props as ComponentProps} from './MarketPlaceCustomLabel'
import React from 'react'

const SIGNS = ['₸', '₽', 'Br']

const DEFAULT_COMPONENT_PROPS: ComponentProps = {
  changeOn: null,
  currencySign: '₽',
  humanPaymentType: 'Наличная',
  paymentType: 'cashless'
}

const testMarketPlaceCustomLabel = (overwriteProps: Partial<ComponentProps>) => {
  const props: ComponentProps = {
    ...DEFAULT_COMPONENT_PROPS,
    ...overwriteProps
  }

  test(componentPropsToTestLabel(props), async () => {
    const {asFragment} = await renderWithProviders(<MarketPlaceCustomLabel {...props} />)

    expect(asFragment()).toMatchSnapshot()
  })
}

const componentPropsToTestLabel = (props: ComponentProps): string => {
  const propsString = Object.keys(props)
    .sort()
    .reduce((result, name) => result + `${name}=${props[name as keyof ComponentProps]} `, '')

  return `MarketPlaceCustomLabel: ${propsString}`
}

const propsAvailableValuesForTests: {[T in keyof ComponentProps]: Array<ComponentProps[T]>} = {
  changeOn: [null, 50],
  currencySign: SIGNS,
  humanPaymentType: ['Наличная'],
  paymentType: ['cashless', 'cash']
}

describe('Order', () => {
  describe('MarketPlaceCustomLabel', () => {
    const propsNames = Object.keys(propsAvailableValuesForTests) as Array<keyof ComponentProps>
    const brutAllPropsRecursivelyAndMakeTest = (
      names: Array<keyof ComponentProps>,
      index: number,
      collectedProps: ComponentProps
    ) => {
      const propsName = names[index]
      propsAvailableValuesForTests[propsName].forEach((propValue: any) => {
        const props = {...collectedProps, [propsName]: propValue}

        if (index === names.length - 1) {
          testMarketPlaceCustomLabel(props)
        } else {
          brutAllPropsRecursivelyAndMakeTest(names, index + 1, props)
        }
      })
    }

    brutAllPropsRecursivelyAndMakeTest(propsNames, 0, DEFAULT_COMPONENT_PROPS)
  })
})
