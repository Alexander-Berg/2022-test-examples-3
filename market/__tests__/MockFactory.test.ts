import {createMockFactory} from 'shared/test-utils'

test('Mock Factory', () => {
  const mock = createMockFactory({a: 1, b: 2})

  expect([mock({a: 2}), mock({a: 3, b: 5}), mock({b: 3})]).toMatchSnapshot('overrides correctly')
})
