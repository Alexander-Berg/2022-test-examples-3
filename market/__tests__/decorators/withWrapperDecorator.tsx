import React from 'react'
import {StoryFnReactReturnType} from '@storybook/react/dist/ts3.4/client/preview/types'

const withWrapperDecorator = (widthOrStyle: number | object) => (Story: () => StoryFnReactReturnType) => (
  <div style={typeof widthOrStyle === 'number' ? {width: widthOrStyle} : widthOrStyle}>
    <Story />
  </div>
)

export default withWrapperDecorator
