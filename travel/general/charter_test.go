package badges

import (
	"testing"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviatdapiclient"
)

var (
	charterObserverBuilder = NewCharterBadgeObserverBuilder(&nop.Logger{})
)

func TestSimpleCharterIsCharter(t *testing.T) {
	onlyObserveSnippetHelper(
		t,
		charterObserverBuilder,
		&aviatdapiclient.SearchResultFare{
			Prices: []aviatdapiclient.SearchResultPrice{
				{Charter: true},
				{Charter: true},
				{Charter: true},
			},
		},
		&aviaSearchProto.Snippet{},
		[]*aviaSearchProto.Badge{{Type: aviaSearchProto.BadgeType_BADGE_TYPE_CHARTER}},
	)
}

func TestSimpleCharterIsSpecialConditions(t *testing.T) {
	onlyObserveSnippetHelper(
		t,
		charterObserverBuilder,
		&aviatdapiclient.SearchResultFare{
			Prices: []aviatdapiclient.SearchResultPrice{
				{Charter: true},
				{Charter: true},
				{Charter: false},
			},
		},
		&aviaSearchProto.Snippet{},
		[]*aviaSearchProto.Badge{{Type: aviaSearchProto.BadgeType_BADGE_TYPE_SPECIAL_CONDITIONS}},
	)
}

func TestSimpleCharterNoBadges(t *testing.T) {
	onlyObserveSnippetHelper(
		t,
		charterObserverBuilder,
		&aviatdapiclient.SearchResultFare{
			Prices: []aviatdapiclient.SearchResultPrice{
				{Charter: false},
				{Charter: false},
				{Charter: false},
			},
		},
		&aviaSearchProto.Snippet{},
		nil,
	)
}
