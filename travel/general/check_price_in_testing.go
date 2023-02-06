package checkprice

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/travel/avia/price_prediction/internal/checkprice"
	"context"
	"strings"
)

const (
	moscowID      = 213
	novosibirskID = 65
	istanbulID    = 11508
	tokioID       = 10636
	osakaID       = 10641

	pobedaPrefix        = "DP "
	aeroflotPrefix      = "SU "
	japanAirlinesPrefix = "JL "
)

var goodCasesForTesting = []struct {
	PointFromType string
	PointFromID   uint
	PointToType   string
	PointToID     uint
	RoutePrefix   string
}{
	{
		PointFromType: "c",
		PointFromID:   moscowID,
		PointToType:   "c",
		PointToID:     novosibirskID,
		RoutePrefix:   pobedaPrefix,
	},
	{
		PointFromType: "c",
		PointFromID:   moscowID,
		PointToType:   "c",
		PointToID:     istanbulID,
		RoutePrefix:   aeroflotPrefix,
	},
	{
		PointFromType: "c",
		PointFromID:   tokioID,
		PointToType:   "c",
		PointToID:     osakaID,
		RoutePrefix:   japanAirlinesPrefix,
	},
}

type ServiceInTesting struct {
	logger  log.Logger
	service *Service
}

func (s *ServiceInTesting) CheckPrice(ctx context.Context, request checkprice.CheckPriceRequest) (result checkprice.PriceCategory) {
	// RASPTICKETS-20530, RASPTICKETS-20591 Для некоторых направлений и авиакомпаний всегда хорошая цена для тестирования
	for _, c := range goodCasesForTesting {
		if request.PointFromType == c.PointFromType && request.PointFromID == c.PointFromID &&
			request.PointToType == c.PointToType && request.PointToID == c.PointToID &&
			hasAirline(request.RouteUID, c.RoutePrefix) {
			writeCheckPriceMetric(predictionCategoryMocked)
			return checkprice.PriceCategoryGood
		}
	}
	return s.service.CheckPrice(ctx, request)
}

func hasAirline(routeUID string, airlinePrefix string) bool {
	return strings.Contains(routeUID, airlinePrefix)
}

func newServiceInTesting(l log.Logger, r VariantsPriceStatsRepository) *ServiceInTesting {
	service := newService(l, r)
	return &ServiceInTesting{logger: l, service: service}
}
