package hardconfig

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
)

func TestNodesForValidationDecode(t *testing.T) {
	nodesForValidation, err := NewNodesForValidationMap("nodes_for_validation_test.yaml", true)
	require.NoError(t, err)
	require.NotNil(t, nodesForValidation, "nodes_for_validation_test is empty")

	require.Equal(t, NodesForValidationMap{
		enums.PartnerTypeFulfillment: map[int64]NodesForValidation{
			145: {
				PartnerID:    145,
				PartnerType:  "FULFILLMENT",
				Name:         "Маршрут Тест",
				NeedValidate: true,
			},
			171: {
				PartnerID:    171,
				PartnerType:  "FULFILLMENT",
				Name:         "Томилино Тест",
				NeedValidate: true,
			},
		},
	}, nodesForValidation)
}
