package trips

import (
	"context"
	"time"

	apimodels "a.yandex-team.ru/travel/komod/trips/internal/components/api/trips/models"
	"a.yandex-team.ru/travel/komod/trips/internal/components/api/trips/testing"
	"a.yandex-team.ru/travel/komod/trips/internal/components/processor"
	eventlogger "a.yandex-team.ru/travel/komod/trips/internal/components/processor/eventslogger"
	"a.yandex-team.ru/travel/komod/trips/internal/consts"
	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/trips/matcher"
	"a.yandex-team.ru/travel/library/go/errutil"
)

type TestingTripsProvider struct {
	*Provider
	matcherRuleFactory *matcher.RuleFactory
	pageBuilder        *StartPageBuilder
}

func NewTestingTripsProvider(
	provider *Provider,
	matcherRuleFactory *matcher.RuleFactory,
	pageBuilder *StartPageBuilder,
) *TestingTripsProvider {
	return &TestingTripsProvider{
		Provider:           provider,
		matcherRuleFactory: matcherRuleFactory,
		pageBuilder:        pageBuilder,
	}
}

func (p *TestingTripsProvider) GetTrips(ctx context.Context, ordersList []orders.Order, geoID int) (response *apimodels.TripsRsp, err error) {
	defer errutil.Wrap(&err, "%s.GetTrips", "api.trips.TestingTripsProvider")
	storage := testing.NewFakeStorage()
	for _, o := range ordersList {
		processorComponent := processor.NewProcessor(
			p.logger,
			testing.NewFakeClient(o),
			p.orderInfoExtractor,
			matcher.NewMatcher(p.matcherRuleFactory.Make(), processor.Cfg.Matcher),
			storage,
			eventlogger.NewMockEventsLogger(),
		)
		if err := processorComponent.ProcessOrder(ctx, o.ID(), time.Now()); err != nil {
			return nil, err
		}
	}
	tripsList, _ := storage.GetSession().GetTrips(ctx, "")

	activePage, err := p.pageBuilder.BuildPage(tripsList, GenerateStartToken(apimodels.ActiveTrips), uint(len(tripsList)))
	if err != nil {
		return nil, err
	}
	pastPage, err := p.pageBuilder.BuildPage(tripsList, GenerateStartToken(apimodels.PastTrips), 1000)

	userNow := p.getUserNow(ctx, geoID)
	return &apimodels.TripsRsp{
		Active: p.mapTripListPage(activePage, userNow, consts.LargeActiveTrip),
		Past:   p.mapTripListPage(pastPage, userNow, consts.PastTrip),
	}, nil
}
