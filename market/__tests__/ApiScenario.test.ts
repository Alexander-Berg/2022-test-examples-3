import {VendorApi} from 'core-legacy/types/restyped'
import ApiScenario from 'core-legacy/api/mock/ApiScenario'

describe('ApiScenario', () => {
  test.skip('Api scenario', async () => {
    const scenarioMocker = new ApiScenario<VendorApi>()

    scenarioMocker.mockPost('/4.0/restapp-front/marketing/v1/ad/create', {})
    scenarioMocker.mockGet('/4.0/restapp-front/marketing/v1/ad/balance', {balance: '500'})
    scenarioMocker.mockGet('/4.0/restapp-front/marketing/v1/ad/campaigns', {campaigns: [], meta: {}})

    scenarioMocker
      .onPost('/4.0/restapp-front/marketing/v1/ad/create')
      .mockGet('/4.0/restapp-front/marketing/v1/ad/balance', {balance: '200'})
      .mockGet('/4.0/restapp-front/marketing/v1/ad/campaigns', {
        campaigns: [
          {
            status: 'active',
            place_id: 0,
            has_access: true,
            is_rating_status_ok: true
          }
        ],
        meta: {}
      })
      .end()

    const {balance} = await scenarioMocker.request.get('/4.0/restapp-front/marketing/v1/ad/balance')
    const {campaigns} = await scenarioMocker.request.get('/4.0/restapp-front/marketing/v1/ad/campaigns')

    expect(balance).toEqual('500')
    expect(campaigns).toHaveLength(0)

    await scenarioMocker.request.post('/4.0/restapp-front/marketing/v1/ad/create')

    const {balance: balanceAfterCreate} = await scenarioMocker.request.get('/4.0/restapp-front/marketing/v1/ad/balance')
    const {campaigns: campaignsAfterCreate} = await scenarioMocker.request.get(
      '/4.0/restapp-front/marketing/v1/ad/campaigns'
    )

    expect(balanceAfterCreate).toEqual('200')
    expect(campaignsAfterCreate).toHaveLength(1)
  })

  test.skip('generate response from another request', async () => {
    type Balance = {
      balance: number
    }
    type MyApi = {
      '/balance': {
        GET: {response: Balance}
        POST: {body: Balance; response: {}}
      }
    }

    const mocker = new ApiScenario<MyApi>()

    let totalBalance = 0
    mocker.onPost('/balance').mockGet('/balance', (body: Balance) => ({balance: totalBalance += body.balance}))

    mocker.mockPost('/balance', {})

    await mocker.request.post('/balance', {balance: 5})
    let response = await mocker.request.get('/balance')

    expect(response.balance).toEqual(5)

    await mocker.request.post('/balance', {balance: 40})
    response = await mocker.request.get('/balance')

    expect(response.balance).toEqual(45)
  })
})
