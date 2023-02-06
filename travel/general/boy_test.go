package badges

import (
	"testing"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
)

var (
	boyObserverBuilder = NewBoyBadgeObserverBuilder(&nop.Logger{})
)

func TestSimpleBoyTrue(t *testing.T) {
	onlyObserveRawVariantHelper(t, boyObserverBuilder, true, true, []*aviaSearchProto.Badge{
		{
			Type:          aviaSearchProto.BadgeType_BADGE_TYPE_BOOK_ON_YANDEX,
			OptionalValue: nil,
		},
	})
}

func TestSimpleBoyFalse(t *testing.T) {
	onlyObserveRawVariantHelper(t, boyObserverBuilder, false, false, nil)
}
