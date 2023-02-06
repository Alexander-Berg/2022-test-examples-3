package tariffmatcher

import (
	"testing"

	"github.com/stretchr/testify/require"

	farefamiliesstructs "a.yandex-team.ru/travel/avia/fare_families/internal/services/fare_families/data_structs/fare_families"
)

func TestWorseCharge(t *testing.T) {
	type TestData struct {
		TestName string
		Left     chargeValue
		Right    chargeValue
		Result   chargeValue
	}

	notAvailable := chargeValue{
		availability: farefamiliesstructs.NotAvailable,
	}
	unknown := chargeValue{}
	forCharge := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
	}
	forFree := chargeValue{
		availability: farefamiliesstructs.AvailableForFree,
	}
	for1000Rub := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "RUB",
		value:        "1000",
	}
	for2500Rub := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "RUB",
		value:        "2500",
	}
	for200Usd := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "USD",
		value:        "200",
	}

	tests := []TestData{
		{
			TestName: "not_available v. unknown",
			Left:     notAvailable,
			Right:    unknown,
			Result:   notAvailable,
		},
		{
			TestName: "not_available v. forCharge",
			Left:     notAvailable,
			Right:    forCharge,
			Result:   notAvailable,
		},
		{
			TestName: "not_available v. forFree",
			Left:     notAvailable,
			Right:    forFree,
			Result:   notAvailable,
		},
		{
			TestName: "not_available v. for1000rub",
			Left:     notAvailable,
			Right:    for1000Rub,
			Result:   notAvailable,
		},
		{
			TestName: "not_available v. for200usd",
			Left:     notAvailable,
			Right:    for200Usd,
			Result:   notAvailable,
		},
		{
			TestName: "unknown v. not_available",
			Left:     unknown,
			Right:    notAvailable,
			Result:   notAvailable,
		},
		{
			TestName: "unknown v. forCharge",
			Left:     unknown,
			Right:    forCharge,
			Result:   unknown,
		},
		{
			TestName: "unknown v. forFree",
			Left:     unknown,
			Right:    forFree,
			Result:   unknown,
		},
		{
			TestName: "unknown v. for1000rub",
			Left:     unknown,
			Right:    for1000Rub,
			Result:   unknown,
		},
		{
			TestName: "unknown v. for200usd",
			Left:     unknown,
			Right:    for200Usd,
			Result:   unknown,
		},
		{
			TestName: "free v. not_available",
			Left:     forFree,
			Right:    notAvailable,
			Result:   notAvailable,
		},
		{
			TestName: "free v. forCharge",
			Left:     forFree,
			Right:    forCharge,
			Result:   forCharge,
		},
		{
			TestName: "free v. unknown",
			Left:     forFree,
			Right:    unknown,
			Result:   unknown,
		},
		{
			TestName: "free v. for1000rub",
			Left:     forFree,
			Right:    for1000Rub,
			Result:   for1000Rub,
		},
		{
			TestName: "free v. for200usd",
			Left:     forFree,
			Right:    for200Usd,
			Result:   for200Usd,
		},
		{
			TestName: "for1000rub v. for200usd",
			Left:     for1000Rub,
			Right:    for200Usd,
			Result:   for200Usd,
		},
		{
			TestName: "for200usd v. for1000rub",
			Left:     for200Usd,
			Right:    for1000Rub,
			Result:   for200Usd,
		},
		{
			TestName: "for1000rub v. for2500rub",
			Left:     for1000Rub,
			Right:    for2500Rub,
			Result:   for2500Rub,
		},
		{
			TestName: "for1000rub v. forCharge",
			Left:     for1000Rub,
			Right:    forCharge,
			Result:   forCharge,
		},
		{
			TestName: "forCharge v. for200usd",
			Left:     forCharge,
			Right:    for200Usd,
			Result:   forCharge,
		},
	}

	for _, test := range tests {
		t.Run(
			test.TestName, func(t *testing.T) {
				result := worseCharge(test.Left, test.Right)
				require.EqualValues(t, test.Result, result)
			},
		)
	}
}

func TestAddCharge(t *testing.T) {
	type TestData struct {
		TestName string
		Left     chargeValue
		Right    chargeValue
		Result   chargeValue
	}

	notAvailable := chargeValue{
		availability: farefamiliesstructs.NotAvailable,
	}
	unknown := chargeValue{}
	forCharge := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
	}
	forFree := chargeValue{
		availability: farefamiliesstructs.AvailableForFree,
	}
	for1000Rub := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "RUB",
		value:        "1000",
	}
	for2500Rub := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "RUB",
		value:        "2500",
	}
	for3500Rub := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "RUB",
		value:        "3500",
	}
	for200Usd := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "USD",
		value:        "200",
	}
	for400Usd := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "USD",
		value:        "400",
	}
	forInvalidAmountRub := chargeValue{
		availability: farefamiliesstructs.AvailableForCharge,
		currency:     "RUB",
		value:        "10A",
	}

	tests := []TestData{
		{
			TestName: "not_available + unknown",
			Left:     notAvailable,
			Right:    unknown,
			Result:   notAvailable,
		},
		{
			TestName: "not_available + forCharge",
			Left:     notAvailable,
			Right:    forCharge,
			Result:   notAvailable,
		},
		{
			TestName: "not_available + forFree",
			Left:     notAvailable,
			Right:    forFree,
			Result:   notAvailable,
		},
		{
			TestName: "not_available + for1000rub",
			Left:     notAvailable,
			Right:    for1000Rub,
			Result:   notAvailable,
		},
		{
			TestName: "not_available + for200usd",
			Left:     notAvailable,
			Right:    for200Usd,
			Result:   notAvailable,
		},
		{
			TestName: "unknown + not_available",
			Left:     unknown,
			Right:    notAvailable,
			Result:   notAvailable,
		},
		{
			TestName: "unknown + forCharge",
			Left:     unknown,
			Right:    forCharge,
			Result:   unknown,
		},
		{
			TestName: "unknown + forFree",
			Left:     unknown,
			Right:    forFree,
			Result:   unknown,
		},
		{
			TestName: "unknown + for1000rub",
			Left:     unknown,
			Right:    for1000Rub,
			Result:   unknown,
		},
		{
			TestName: "unknown + for200usd",
			Left:     unknown,
			Right:    for200Usd,
			Result:   unknown,
		},
		{
			TestName: "free + not_available",
			Left:     forFree,
			Right:    notAvailable,
			Result:   notAvailable,
		},
		{
			TestName: "free + forCharge",
			Left:     forFree,
			Right:    forCharge,
			Result:   forCharge,
		},
		{
			TestName: "free + unknown",
			Left:     forFree,
			Right:    unknown,
			Result:   unknown,
		},
		{
			TestName: "free + for1000rub",
			Left:     forFree,
			Right:    for1000Rub,
			Result:   for1000Rub,
		},
		{
			TestName: "free + for200usd",
			Left:     forFree,
			Right:    for200Usd,
			Result:   for200Usd,
		},
		{
			TestName: "for1000rub + for200usd",
			Left:     for1000Rub,
			Right:    for200Usd,
			Result:   forCharge,
		},
		{
			TestName: "for200usd + for1000rub",
			Left:     for200Usd,
			Right:    for1000Rub,
			Result:   forCharge,
		},
		{
			TestName: "for1000rub + for2500rub",
			Left:     for1000Rub,
			Right:    for2500Rub,
			Result:   for3500Rub,
		},
		{
			TestName: "for1000rub + forCharge",
			Left:     for1000Rub,
			Right:    forCharge,
			Result:   forCharge,
		},
		{
			TestName: "forCharge + for200usd",
			Left:     forCharge,
			Right:    for200Usd,
			Result:   forCharge,
		},
		{
			TestName: "for200usd + for200usd",
			Left:     for200Usd,
			Right:    for200Usd,
			Result:   for400Usd,
		},
		{
			TestName: "forInvalidRub + for1000rub",
			Left:     forInvalidAmountRub,
			Right:    for1000Rub,
			Result:   forCharge,
		},
		{
			TestName: "for1000rub + forInvalidRub",
			Left:     for1000Rub,
			Right:    forInvalidAmountRub,
			Result:   forCharge,
		},
	}

	for _, test := range tests {
		t.Run(
			test.TestName, func(t *testing.T) {
				result := addCharge(test.Left, test.Right)
				require.EqualValues(t, test.Result, result)
			},
		)
	}
}

func TestUpdateRule(t *testing.T) {
	t.Run(
		"TestUpdateRuleSameCharge", func(t *testing.T) {
			testTerm := farefamiliesstructs.FareFamilyTermRule{
				Availability: string(farefamiliesstructs.AvailableForCharge),
				Charge: &farefamiliesstructs.Charge{
					Currency: "RUB",
					Value:    "1000",
				},
			}
			for1000Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "1000",
			}
			resultTerm, resultSuffix := for1000Rub.updateRule(testTerm)
			require.EqualValues(t, testTerm, resultTerm)
			require.EqualValues(t, "", resultSuffix)
		},
	)

	t.Run(
		"TestUpdateRuleDifferentCharge", func(t *testing.T) {
			testTerm := farefamiliesstructs.FareFamilyTermRule{
				Availability: string(farefamiliesstructs.AvailableForCharge),
				Charge: &farefamiliesstructs.Charge{
					Currency: "RUB",
					Value:    "2500",
				},
			}
			for1000Rub := chargeValue{
				availability: farefamiliesstructs.AvailableForCharge,
				currency:     "RUB",
				value:        "1000",
			}
			expectedTerm := farefamiliesstructs.FareFamilyTermRule{
				Availability: string(farefamiliesstructs.AvailableForCharge),
				Charge: &farefamiliesstructs.Charge{
					Currency: "RUB",
					Value:    "1000",
				},
			}
			resultTerm, resultSuffix := for1000Rub.updateRule(testTerm)
			require.EqualValues(t, expectedTerm, resultTerm)
			require.EqualValues(t, "RUB1000", resultSuffix)
		},
	)
}
