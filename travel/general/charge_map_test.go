package tariffmatcher

import (
	"testing"

	"github.com/stretchr/testify/require"

	farefamiliesstructs "a.yandex-team.ru/travel/avia/fare_families/internal/services/fare_families/data_structs/fare_families"
)

func TestUpdateWorstCharge(t *testing.T) {
	t.Run(
		"TestUpdateWorstCharge", func(t *testing.T) {
			for1000Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "1000",
			}
			for3500Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "3500",
			}
			for4500Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "4500",
			}
			testChargeMap := chargeMap{
				"term1":                                  for1000Rub,
				farefamiliesstructs.RefundableCode:       for1000Rub,
				farefamiliesstructs.ChangingCarriageCode: for4500Rub,
			}
			testFareFamily := farefamiliesstructs.FareFamilyForVariant{
				Terms: []farefamiliesstructs.FareFamilyTermForVariant{
					{
						Code: farefamiliesstructs.RefundableCode,
						Rule: farefamiliesstructs.FareFamilyTermRule{
							Availability: string(farefamiliesstructs.AvailableForCharge),
							Charge: &farefamiliesstructs.Charge{
								Currency: "RUB",
								Value:    "3500",
							},
						},
					},
					{
						Code: farefamiliesstructs.ChangingCarriageCode,
						Rule: farefamiliesstructs.FareFamilyTermRule{
							Availability: string(farefamiliesstructs.AvailableForCharge),
							Charge: &farefamiliesstructs.Charge{
								Currency: "RUB",
								Value:    "3500",
							},
						},
					},
				},
			}
			testChargeMap.updateWorstCharges(&testFareFamily)

			expectedChargeMap := map[string]chargeValue{
				"term1":                                  for1000Rub,
				farefamiliesstructs.RefundableCode:       for3500Rub,
				farefamiliesstructs.ChangingCarriageCode: for4500Rub,
			}
			require.EqualValues(t, expectedChargeMap, testChargeMap)
		},
	)
}

func TestInitWorstCharge(t *testing.T) {
	// Same as TestUpdateWorstCharge, but initial map is empty
	t.Run(
		"TestInitWorstCharge", func(t *testing.T) {
			for3500Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "3500",
			}
			testChargeMap := chargeMap{}
			testFareFamily := farefamiliesstructs.FareFamilyForVariant{
				Terms: []farefamiliesstructs.FareFamilyTermForVariant{
					{
						Code: farefamiliesstructs.RefundableCode,
						Rule: farefamiliesstructs.FareFamilyTermRule{
							Availability: string(farefamiliesstructs.AvailableForCharge),
							Charge: &farefamiliesstructs.Charge{
								Currency: "RUB",
								Value:    "3500",
							},
						},
					},
					{
						Code: farefamiliesstructs.ChangingCarriageCode,
						Rule: farefamiliesstructs.FareFamilyTermRule{
							Availability: string(farefamiliesstructs.AvailableForCharge),
							Charge: &farefamiliesstructs.Charge{
								Currency: "RUB",
								Value:    "3500",
							},
						},
					},
				},
			}
			testChargeMap.updateWorstCharges(&testFareFamily)

			expectedChargeMap := map[string]chargeValue{
				farefamiliesstructs.RefundableCode:       for3500Rub,
				farefamiliesstructs.ChangingCarriageCode: for3500Rub,
			}
			require.EqualValues(t, expectedChargeMap, testChargeMap)
		},
	)
}

func TestCalculateTotalCharge(t *testing.T) {
	t.Run(
		"TestCalculateTotalCharge", func(t *testing.T) {
			for1000Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "1000",
			}
			for1500Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "1500",
			}
			for2500Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "2500",
			}
			forCharge := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
			}
			chargeMapCarrier1 := chargeMap{
				farefamiliesstructs.RefundableCode:       for1000Rub,
				farefamiliesstructs.ChangingCarriageCode: for1500Rub,
			}
			chargeMapCarrier2 := chargeMap{
				farefamiliesstructs.RefundableCode:       for1500Rub,
				farefamiliesstructs.ChangingCarriageCode: forCharge,
			}
			chargeMapPerCarrier := map[int32]chargeMap{
				1: chargeMapCarrier1,
				2: chargeMapCarrier2,
			}
			result := calculateTotalCharges(chargeMapPerCarrier)

			expectedChargeMap := map[string]chargeValue{
				farefamiliesstructs.RefundableCode:       for2500Rub,
				farefamiliesstructs.ChangingCarriageCode: forCharge,
			}
			require.EqualValues(t, result, expectedChargeMap)
		},
	)
}
