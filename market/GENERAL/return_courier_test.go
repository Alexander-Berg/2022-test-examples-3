package lite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/combinator-app/routes"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/express"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

const customExpressPartnerID = 1337133713371337

func TestGetCourierOptionsReturn(t *testing.T) {
	graphEx := graph.NewReturnPickupExample(172, 1232312, 1234, 1234567)

	// This tariff couldn't handle such a heavy items
	unsuitableTariff := tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 48,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery | tr.ProgramBeruCrossdock),
		Option: tr.Option{
			Cost:    11,
			DaysMin: 3,
			DaysMax: 5,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 15000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 100,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	globalRule := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 48,
		regionTo:          1,
		ruleType:          tr.RuleTypeGlobal,
	})
	globalRule.IsMarketCourier = true
	globalRule.ID = 1
	shopTariff := tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 48,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery | tr.ProgramBeruCrossdock),
		Option: tr.Option{
			Cost:    42,
			DaysMin: 5,
			DaysMax: 7,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 20000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 100,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   120542,
		},
		Type:            tr.RuleTypePayment,
		IsMarketCourier: true,
	}
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&unsuitableTariff)
	tariffsFinder.Add(&globalRule)
	tariffsFinder.Add(&shopTariff)
	tariffsFinder.Finish(&regionMap)

	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
		Express:       &express.Express{Warehouses: map[int64]*express.Warehouse{customExpressPartnerID: nil}},
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		RegionId: 213,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     2 + shopTariff.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	// Режим обратной совместимости: если нет флага return_courier_options=1, не возвращать ошибок
	req.RearrFactors = "return_courier_options=0"
	resp, err := env.Client.GetCourierOptionsReturn(env.Ctx, req)
	require.NoError(t, err)
	require.Empty(t, resp.Options)

	// Ошибка о невозможности возврата: курьерка не в списке
	req.RearrFactors = "return_courier_options=1;combinator_return_courier_hardcode=1"
	resp, err = env.Client.GetCourierOptionsReturn(env.Ctx, req)
	require.Error(t, err)
	require.Nil(t, resp)

	// Курьерка маркетная: ищем опции
	req.RearrFactors = "return_courier_options=1;combinator_return_courier_hardcode=0"
	resp, err = env.Client.GetCourierOptionsReturn(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)
	require.NotEmpty(t, resp.Options)

	routes.ReturnDeliveryServicesBlacklist[48] = true

	// Курьерка блеклист неактивен - можно вернуть
	req.RearrFactors = "return_courier_options=1;combinator_return_courier_hardcode=0;return_courier_services_blacklist=0"
	resp, err = env.Client.GetCourierOptionsReturn(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)
	require.NotEmpty(t, resp.Options)

	// Блеклист активен - вернуть нельзя
	req.RearrFactors = "return_courier_options=1;combinator_return_courier_hardcode=0;return_courier_services_blacklist=1"
	resp, err = env.Client.GetCourierOptionsReturn(env.Ctx, req)
	require.Error(t, err)
	require.Nil(t, resp)

	routes.ReturnDeliveryServicesBlacklist[48] = false

	// Не ищем опций для экспресса
	req.Items[0].AvailableOffers[0].PartnerId = customExpressPartnerID
	req.RearrFactors = "return_courier_options=1;combinator_return_courier_hardcode=0"
	_, err = env.Client.GetCourierOptionsReturn(env.Ctx, req)
	require.Error(t, err)
}
