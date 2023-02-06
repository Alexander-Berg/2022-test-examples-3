package hardconfig

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"
)

var (
	logisticPoint = int64(1000556473830)
)

func TestProdConfigs(t *testing.T) {
	blacklist, err := NewFromToBlackListMap("from_to_blacklist.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации from_to_blacklist.yaml")
	require.NotNil(t, blacklist, "конфигурация from_to_blacklist.yaml пуста")

	for _, blacklistByPartner := range blacklist {
		for _, item := range blacklistByPartner {
			require.NotZero(t, item.PartnerID, "поле partner_id - обязательное и не может быть равно 0")
			require.NotZero(t, item.From, fmt.Sprintf("поле from - обязательное, запись партнера - %d", item.PartnerID))
			require.NotEmpty(t, item.To, fmt.Sprintf("поле to - обязательное, запись партнера - %d", item.PartnerID))
		}
	}

	limitation, err := NewLimitationForPaymentTypes("limitation_for_payment_types.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации limitation_for_payment_types.yaml")
	require.NotNil(t, limitation, "конфигурация limitation_for_payment_types.yaml пуста")
	require.Greater(t, len(limitation), 0, "в конфигурации limitation_for_payment_types.yaml отсутствуют элементы")

	for _, limitationByPartner := range limitation {
		for _, item := range limitationByPartner {
			require.NotZero(t, item.PartnerID, "поле partner_id - обязательное и не может быть равно 0")
			require.NotEmpty(t, item.PartnerName, fmt.Sprintf("поле partner_name - обязательное, запись партнера - %d", item.PartnerID))
		}
	}

	_, err = NewLogisticDayStartByPartner("logistic_day_start_by_partners.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации logistic_day_start_by_partners.yaml")

	_, err = NewLogisticDayStartByType("logistic_day_start_by_types.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации logistic_day_start_by_types.yaml")

	nodesForValidation, err := NewNodesForValidationMap("nodes_for_validation.yaml", true)
	require.NoError(t, err, "ошибка при чтении конфигурации nodes_for_validation.yaml")
	require.NotNil(t, nodesForValidation, "конфигурация nodes_for_validation.yaml пуста")
	require.Greater(t, len(nodesForValidation), 0, "В конфигурации nodes_for_validation.yaml отсутствуют элементы")
}

func TestTestingConfigs(t *testing.T) {
	blacklist, err := NewFromToBlackListMap("from_to_blacklist_testing.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации from_to_blacklist_testing.yaml")
	require.NotNil(t, blacklist, "конфигурация from_to_blacklist_testing.yaml пуста")

	for _, blacklistByPartner := range blacklist {
		for _, item := range blacklistByPartner {
			require.NotZero(t, item.PartnerID, "поле partner_id - обязательное и не может быть равно 0")
			require.NotEmpty(t, item.From, fmt.Sprintf("поле from - обязательное, запись партнера - %d", item.PartnerID))
			require.NotEmpty(t, item.To, fmt.Sprintf("поле to - обязательное, запись партнера - %d", item.PartnerID))
		}
	}

	limitation, err := NewLimitationForPaymentTypes("limitation_for_payment_types_testing.yaml")
	require.NoError(t, err, "Ошибка при чтении конфигурации limitation_for_payment_types_testing.yaml")
	require.NotNil(t, limitation, "Конфигурация limitation_for_payment_types_testing.yaml пуста")
	require.Greater(t, len(limitation), 0, "В конфигурации limitation_for_payment_types_testing.yaml отсутствуют элементы")

	for _, limitationByPartner := range limitation {
		for _, item := range limitationByPartner {
			require.NotZero(t, item.PartnerID, "поле partner_id - обязательное и не может быть равно 0")
			require.NotEmpty(t, item.PartnerName, fmt.Sprintf("поле partner_name - обязательное, запись партнера - %d", item.PartnerID))
		}
	}

	_, err = NewLogisticDayStartByPartner("logistic_day_start_by_partners_testing.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации logistic_day_start_by_partners_testing.yaml")

	_, err = NewLogisticDayStartByType("logistic_day_start_by_types_testing.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации logistic_day_start_by_types_testing.yaml")

	nodesForValidation, err := NewNodesForValidationMap("nodes_for_validation_testing.yaml", true)
	require.NoError(t, err, "ошибка при чтении конфигурации nodes_for_validation_testing.yaml")
	require.NotNil(t, nodesForValidation, "конфигурация nodes_for_validation_testing.yaml пуста")
	require.Greater(t, len(nodesForValidation), 0, "В конфигурации nodes_for_validation_testing.yaml отсутствуют элементы")
}
