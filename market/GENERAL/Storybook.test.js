import initStoryshots, {renderOnly} from '@storybook/addon-storyshots'
import React from 'react'

jest.mock('recharts')
jest.mock('@material-ui/core/Dialog', () => ({
  __esModule: true,
  default: ({children}) => React.createElement('div', {}, ...children)
}))

initStoryshots({test: renderOnly})
