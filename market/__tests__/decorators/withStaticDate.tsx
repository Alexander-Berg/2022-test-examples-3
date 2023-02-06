import React, {useEffect} from 'react'
import {StoryFnReactReturnType} from '@storybook/react/dist/ts3.4/client/preview/types'

const originalDateNow = Date.now
const restore = () => {
  Date.now = originalDateNow
}

const withStaticDate = (date: Date) => (Story: () => StoryFnReactReturnType) => {
  Date.now = () => date.getTime()

  useEffect(() => restore, [])

  return <Story />
}

export default withStaticDate
