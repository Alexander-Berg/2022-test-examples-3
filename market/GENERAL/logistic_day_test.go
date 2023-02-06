package hardconfig

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
)

func TestLogisticDaysByPartner(t *testing.T) {
	byPartner, err := NewLogisticDayStartByPartner("logistic_day_by_partner_test.yaml")
	require.NoError(t, err)
	require.NotNil(t, byPartner, "logistic_day_by_partner is empty")

	byType, err := NewLogisticDayStartByType("logistic_day_by_type_test.yaml")
	require.NoError(t, err)
	require.NotNil(t, byType, "logistic_day_by_partner is empty")

	f := HardConfig{
		LogisticDayByPartners: byPartner,
		LogisticDayByTypes:    byType,
	}

	hour, ok := f.LogisticDayByTypes[enums.PartnerTypeFulfillment]
	require.True(t, ok)
	require.Equal(t, int8(1), hour)

	hour, ok = f.LogisticDayByTypes[enums.PartnerTypeDropship]
	require.True(t, ok)
	require.Equal(t, int8(2), hour)

	_, ok = f.LogisticDayByTypes[enums.PartnerTypeDSBS]
	require.False(t, ok)

	hour, ok = f.LogisticDayByPartners[1003937]
	require.True(t, ok)
	require.Equal(t, int8(3), hour)
	hour, ok = f.LogisticDayByPartners[1003938]
	require.True(t, ok)
	require.Equal(t, int8(4), hour)
}
