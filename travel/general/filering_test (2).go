package filtering2

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaAPI "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	"a.yandex-team.ru/travel/app/backend/internal/avia/search/searchcommon"
)

func TestBuildFilters(t *testing.T) {
	searchContext, _ := searchcommon.ParseQIDToProto("220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru")
	_, res := ApplyFilters(context.Background(), &nop.Logger{}, nil, nil, searchContext, &aviaAPI.SearchFiltersReq{})

	require.Nil(t, res.QuickBaggage.MinPriceWithBaggage)
	require.Nil(t, res.QuickTransfer.MinPriceNoTransfer)
}
