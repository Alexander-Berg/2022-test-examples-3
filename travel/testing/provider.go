package testing

import (
	"context"
	"fmt"
	"math"
	"time"

	"a.yandex-team.ru/travel/library/go/renderer"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/interfaces"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/scheduling"
)

type Provider struct {
	notificationsBuilder               scheduling.NotificationsBuilder
	routePointsExtractor               interfaces.RoutePointsExtractor
	settlementNominativeTitleExtractor interfaces.SettlementNominativeTitleExtractor
}

func NewProvider(routePointsExtractor interfaces.RoutePointsExtractor, settlementTitleExtractor interfaces.SettlementNominativeTitleExtractor) *Provider {
	return &Provider{
		routePointsExtractor:               routePointsExtractor,
		notificationsBuilder:               scheduling.NotificationsBuilder{},
		settlementNominativeTitleExtractor: settlementTitleExtractor,
	}
}

func (p *Provider) GetBlock(ctx context.Context, orderInfo *orders.OrderInfo, notification models.Notification) (renderer.Block, error) {
	order, err := orderInfo.ToOrder()
	if err != nil {
		return nil, err
	}
	realNotifyAt := p.getRealNotifyAt(order, notification)
	settlementID, err := p.routePointsExtractor.ExtractArrivalSettlementID(orderInfo)
	if err != nil {
		settlementID = -1
	}
	settlementTitle, found := p.settlementNominativeTitleExtractor.GetNominativeTitle(settlementID)
	if !found {
		settlementTitle = "UNKNOWN"
	}
	return ui.NewUsefulBlock(
		fmt.Sprintf(
			"NotificationID: %d\nOrderID: %s\nNotificationSubtype: %s\nSupposed to be scheduled for %v",
			notification.ID,
			orderInfo.ID,
			notification.Subtype,
			realNotifyAt,
		),
		fmt.Sprintf("order: %+v\nsettlementID: %d\nsettlementTitle: %s", order, settlementID, settlementTitle),
		make([]ui.UsefulBlockItem, 0),
	), nil
}

func (p *Provider) GetBlockType() blocks.BlockType {
	return blocks.TestingBlock
}

func (p *Provider) getRealNotifyAt(order models.Order, notification models.Notification) time.Time {
	notifications, _ := p.notificationsBuilder.Build(order, *notification.Recipient, notification.CreatedAt)
	realNotifyAt := time.Time{}
	for _, n := range notifications {
		if n.Subtype == notification.Subtype {
			return n.NotifyAt.In(time.UTC)
		}
	}
	if math.Abs(float64(realNotifyAt.Sub(notification.NotifyAt))) < float64(5*time.Second) {
		realNotifyAt = notification.NotifyAt.In(time.UTC)
	}
	return realNotifyAt
}
