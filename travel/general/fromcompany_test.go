package badges

import (
	"testing"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
)

var (
	fromCompanyObserverBuilder = NewFromCompanyBadgeObserverBuilder(&nop.Logger{})
)

func TestSimpleFromCompanyTrue(t *testing.T) {
	onlyObserveRawVariantHelper(
		t,
		fromCompanyObserverBuilder,
		true,
		false,
		[]*aviaSearchProto.Badge{{Type: aviaSearchProto.BadgeType_BADGE_TYPE_AVIACOMPANY_DIRECT_SELLING}})
}

func TestSimpleFromCompanyFalse(t *testing.T) {
	onlyObserveRawVariantHelper(t, fromCompanyObserverBuilder, false, false, nil)
}
