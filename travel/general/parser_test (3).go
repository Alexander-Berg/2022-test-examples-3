package task

import (
	"testing"

	"github.com/stretchr/testify/assert"

	pb "a.yandex-team.ru/travel/proto"
	"a.yandex-team.ru/travel/trains/search_api/api"
	"a.yandex-team.ru/travel/trains/worker/internal/pkg/clients/imclient/models"
)

func TestFixTrainNumber(t *testing.T) {
	testCases := []struct {
		trainNumber          string
		expectFixTrainNumber string
	}{
		{
			trainNumber:          "175F",
			expectFixTrainNumber: "175F",
		},
		{
			trainNumber:          "175Ф",
			expectFixTrainNumber: "175Ф",
		},
		{
			trainNumber:          "175*Ф",
			expectFixTrainNumber: "175Ф",
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			trainNumber := fixTrainNumber(tc.trainNumber)
			assert.Equal(t, tc.expectFixTrainNumber, trainNumber)
		})
	}
}

func TestUpdateBestTariff(t *testing.T) {
	testCases := []struct {
		tariff1      *trainTariff
		tariff2      *trainTariff
		expectTariff *trainTariff
	}{
		{
			tariff1: &trainTariff{
				ticketPrice: &Price{
					value:    100,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    100,
					currency: "RUB",
				},
				severalPrices:          false,
				seats:                  15,
				hasNonRefundableTariff: false,
			},
			tariff2: &trainTariff{
				ticketPrice: &Price{
					value:    200,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    200,
					currency: "RUB",
				},
				severalPrices:          false,
				seats:                  25,
				hasNonRefundableTariff: false,
			},
			expectTariff: &trainTariff{
				ticketPrice: &Price{
					value:    100,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    100,
					currency: "RUB",
				},
				severalPrices:          true,
				seats:                  40,
				hasNonRefundableTariff: false,
			},
		},
		{
			tariff1: &trainTariff{
				ticketPrice: &Price{
					value:    200,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    150,
					currency: "RUB",
				},
				severalPrices: false,
				seats:         15,
			},
			tariff2: &trainTariff{
				ticketPrice: &Price{
					value:    200,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    175,
					currency: "RUB",
				},
				severalPrices: false,
				seats:         25,
			},
			expectTariff: &trainTariff{
				ticketPrice: &Price{
					value:    200,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    175,
					currency: "RUB",
				},
				severalPrices: false,
				seats:         40,
			},
		},
		{
			tariff1: &trainTariff{
				ticketPrice: &Price{
					value:    200,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    50,
					currency: "RUB",
				},
				severalPrices: false,
				seats:         15,
			},
			tariff2: &trainTariff{
				ticketPrice: &Price{
					value:    200,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    25,
					currency: "RUB",
				},
				severalPrices: false,
				seats:         25,
			},
			expectTariff: &trainTariff{
				ticketPrice: &Price{
					value:    200,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    50,
					currency: "RUB",
				},
				severalPrices: false,
				seats:         40,
			},
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			bestTraiff := updateBestTariff(tc.tariff1, tc.tariff2)
			assert.Equal(t, tc.expectTariff.ticketPrice, bestTraiff.ticketPrice)
			assert.Equal(t, tc.expectTariff.servicePrice, bestTraiff.servicePrice)
			assert.Equal(t, tc.expectTariff.seats, bestTraiff.seats)
			assert.Equal(t, tc.expectTariff.severalPrices, bestTraiff.severalPrices)
			assert.Equal(t, tc.expectTariff.hasNonRefundableTariff, bestTraiff.hasNonRefundableTariff)
		})
	}
}

func TestValidate(t *testing.T) {
	testCases := []struct {
		tariff         *trainTariff
		expectNoErrors bool
		expectErrors   map[tariffError]bool
	}{
		{
			tariff: &trainTariff{
				ticketPrice: &Price{
					value:    100,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    100,
					currency: "RUB",
				},
				severalPrices:          false,
				seats:                  15,
				availabilityIndication: available,
			},
			expectNoErrors: true,
			expectErrors:   make(map[tariffError]bool),
		},
		{
			tariff: &trainTariff{
				ticketPrice: &Price{
					value:    100,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    100,
					currency: "RUB",
				},
				severalPrices: false,
				seats:         15,
			},
			expectNoErrors: false,
			expectErrors: map[tariffError]bool{
				unknownTariffError: true,
			},
		},
		{
			tariff: &trainTariff{
				ticketPrice: &Price{
					value:    1,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    100,
					currency: "RUB",
				},
				severalPrices:             false,
				seats:                     0,
				availabilityIndication:    available,
				coachType:                 unknownCoachType,
				placeReservationType:      unknownReservationType,
				isTransitDocumentRequired: true,
				categoryTraits: map[string]bool{
					"is_for_children": true,
				},
			},
			expectNoErrors: false,
			expectErrors: map[tariffError]bool{
				unsupportedCoachTypeTariffError:       true,
				tooCheapTariffError:                   true,
				unsupportedReservationTypeTariffError: true,
				soldOutTariffError:                    true,
				transitDocumentRequiredTariffError:    true,
				childTariffError:                      true,
			},
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			noErrors, errors := validate(*tc.tariff)
			assert.Equal(t, tc.expectNoErrors, noErrors)
			assert.Equal(t, len(tc.expectErrors), len(errors))
			assert.Equal(t, tc.expectErrors, errors)
		})
	}
}

func TestBuildTrainTariffsClasses(t *testing.T) {
	testCases := []struct {
		allTariffs                []*trainTariff
		expectTariffClasses       map[coachType]trainTariff
		expectBrokenTariffClasses map[coachType][]tariffError
	}{
		{
			allTariffs: []*trainTariff{
				&trainTariff{
					ticketPrice: &Price{
						value: 100,
					},
					servicePrice: &Price{
						value: 100,
					},
					severalPrices:             false,
					seats:                     15,
					availabilityIndication:    available,
					coachType:                 platzkarteCoachType,
					isTransitDocumentRequired: false,
					maxSeatsInTheSameCar:      15,
					placeReservationType:      usualReservationType,
				},
				&trainTariff{
					ticketPrice: &Price{
						value: 150,
					},
					servicePrice: &Price{
						value: 100,
					},
					severalPrices:             false,
					seats:                     15,
					availabilityIndication:    available,
					coachType:                 platzkarteCoachType,
					isTransitDocumentRequired: false,
					maxSeatsInTheSameCar:      15,
					placeReservationType:      usualReservationType,
				},
				&trainTariff{
					ticketPrice: &Price{
						value: 150,
					},
					servicePrice: &Price{
						value: 100,
					},
					severalPrices:             false,
					seats:                     15,
					availabilityIndication:    available,
					coachType:                 suiteCoachType,
					isTransitDocumentRequired: false,
					maxSeatsInTheSameCar:      15,
					placeReservationType:      usualReservationType,
				},
				&trainTariff{
					ticketPrice: &Price{
						value: 150,
					},
					servicePrice: &Price{
						value: 100,
					},
					severalPrices:             false,
					seats:                     15,
					availabilityIndication:    available,
					coachType:                 suiteCoachType,
					isTransitDocumentRequired: false,
					maxSeatsInTheSameCar:      15,
					placeReservationType:      usualReservationType,
				},
				&trainTariff{
					ticketPrice: &Price{
						value: 150,
					},
					servicePrice: &Price{
						value: 100,
					},
					severalPrices:             false,
					seats:                     0,
					availabilityIndication:    available,
					coachType:                 suiteCoachType,
					isTransitDocumentRequired: false,
					maxSeatsInTheSameCar:      15,
					placeReservationType:      usualReservationType,
				},
				&trainTariff{
					ticketPrice: &Price{
						value: 150,
					},
					servicePrice: &Price{
						value: 100,
					},
					severalPrices:             false,
					seats:                     0,
					availabilityIndication:    available,
					coachType:                 softCoachType,
					isTransitDocumentRequired: false,
					maxSeatsInTheSameCar:      15,
					placeReservationType:      usualReservationType,
				},
			},
			expectTariffClasses: map[coachType]trainTariff{
				platzkarteCoachType: trainTariff{
					ticketPrice: &Price{
						value: 100,
					},
					servicePrice: &Price{
						value: 100,
					},
					severalPrices:             true,
					seats:                     30,
					availabilityIndication:    available,
					coachType:                 platzkarteCoachType,
					isTransitDocumentRequired: false,
					maxSeatsInTheSameCar:      15,
					placeReservationType:      usualReservationType,
				},
				suiteCoachType: trainTariff{
					ticketPrice: &Price{
						value: 150,
					},
					servicePrice: &Price{
						value: 100,
					},
					severalPrices:             false,
					seats:                     30,
					availabilityIndication:    available,
					coachType:                 suiteCoachType,
					isTransitDocumentRequired: false,
					maxSeatsInTheSameCar:      15,
					placeReservationType:      usualReservationType,
				},
			},
			expectBrokenTariffClasses: map[coachType][]tariffError{
				softCoachType: []tariffError{
					soldOutTariffError,
				},
			},
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			tariffClasses, brokenTariffClasses := buildTrainTariffsClasses(tc.allTariffs)
			assert.Equal(t, tc.expectTariffClasses, tariffClasses)
			assert.Equal(t, tc.expectBrokenTariffClasses, brokenTariffClasses)
		})
	}
}

func TestVFixBrokenClasses(t *testing.T) {
	testCases := []struct {
		brokenClasses            map[coachType][]tariffError
		expectFixedBrokenClasses map[coachType][]tariffError
	}{
		{
			brokenClasses:            map[coachType][]tariffError{},
			expectFixedBrokenClasses: map[coachType][]tariffError{},
		},
		{
			brokenClasses: map[coachType][]tariffError{
				platzkarteCoachType: []tariffError{},
			},
			expectFixedBrokenClasses: map[coachType][]tariffError{
				platzkarteCoachType: []tariffError{},
			},
		},
		{
			brokenClasses: map[coachType][]tariffError{
				unknownCoachType: []tariffError{},
			},
			expectFixedBrokenClasses: map[coachType][]tariffError{},
		},
		{
			brokenClasses: map[coachType][]tariffError{
				platzkarteCoachType: []tariffError{
					unknownTariffError,
				},
				unknownCoachType: []tariffError{
					tooCheapTariffError,
					unsupportedCoachTypeTariffError,
					serviceNotAllowedTariffError,
				},
			},
			expectFixedBrokenClasses: map[coachType][]tariffError{
				platzkarteCoachType: []tariffError{
					unknownTariffError,
				},
			},
		},
		{
			brokenClasses: map[coachType][]tariffError{
				platzkarteCoachType: []tariffError{
					unknownTariffError,
				},
				unknownCoachType: []tariffError{
					unknownTariffError,
					soldOutTariffError,
					transitDocumentRequiredTariffError,
					notAvailableInWebTariffError,
					featureNotAllowedTariffError,
					serviceNotAllowedTariffError,
					carrierNotAllowedForSalesTariffError,
					otherReasonOfInaccessibilityTariffError,
					unsupportedReservationTypeTariffError,
					tooCheapTariffError,
					unsupportedCoachTypeTariffError,
					childTariffError,
				},
			},
			expectFixedBrokenClasses: map[coachType][]tariffError{
				platzkarteCoachType: []tariffError{
					unknownTariffError,
				},
				unknownCoachType: []tariffError{
					unknownTariffError,
					soldOutTariffError,
					transitDocumentRequiredTariffError,
					notAvailableInWebTariffError,
					featureNotAllowedTariffError,
					carrierNotAllowedForSalesTariffError,
					otherReasonOfInaccessibilityTariffError,
					unsupportedReservationTypeTariffError,
					childTariffError,
				},
			},
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			fixedBrokenClasses := fixBrokenClasses(tc.brokenClasses)
			assert.Equal(t, tc.expectFixedBrokenClasses, fixedBrokenClasses)
		})
	}
}

func TestPriceToProto(t *testing.T) {
	testCases := []struct {
		price            Price
		expectProtoPrice *pb.TPrice
	}{
		{
			price: Price{
				value:    125,
				currency: "RUB",
			},
			expectProtoPrice: &pb.TPrice{
				Currency:  pb.ECurrency_C_RUB,
				Precision: 2,
				Amount:    12500,
			},
		},
		{
			price: Price{
				value:    125,
				currency: "RUR",
			},
			expectProtoPrice: &pb.TPrice{
				Currency:  pb.ECurrency_C_RUB,
				Precision: 2,
				Amount:    12500,
			},
		},
		{
			price: Price{
				value:    125,
				currency: "USD",
			},
			expectProtoPrice: nil,
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			protoPrice := priceToProto(tc.price)
			assert.Equal(t, tc.expectProtoPrice, protoPrice)
		})
	}
}

func TestMinTariffToProto(t *testing.T) {
	testCases := []struct {
		trainTariff          *trainTariff
		expectMinTariffClass *api.MinTariffsClass
	}{
		{
			trainTariff: &trainTariff{
				ticketPrice: &Price{
					value:    150,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    100,
					currency: "RUB",
				},
				severalPrices:             false,
				seats:                     15,
				availabilityIndication:    available,
				coachType:                 platzkarteCoachType,
				isTransitDocumentRequired: false,
				maxSeatsInTheSameCar:      15,
				placeReservationType:      usualReservationType,
			},
			expectMinTariffClass: &api.MinTariffsClass{
				Price: &pb.TPrice{
					Currency:  pb.ECurrency_C_RUB,
					Precision: 2,
					Amount:    15000,
				},
				Seats: uint32(15),
			},
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			minTariffClass := minTariffToProto(tc.trainTariff)
			assert.Equal(t, tc.expectMinTariffClass, minTariffClass)
		})
	}
}

func TestExtractBrokenClassesCode(t *testing.T) {
	testCases := []struct {
		brokenClasses           map[coachType][]tariffError
		expectBrokenClassesCode api.BrokenClassesCode
		expectOk                bool
	}{
		{
			brokenClasses: map[coachType][]tariffError{
				commonCoachType: []tariffError{
					childTariffError,
				},
				compartmentCoachType: []tariffError{
					serviceNotAllowedTariffError,
				},
			},
			expectBrokenClassesCode: api.BrokenClassesCode_BROKEN_CLASSES_CODE_OTHER,
			expectOk:                true,
		},
		{
			brokenClasses: map[coachType][]tariffError{
				commonCoachType: []tariffError{
					childTariffError,
				},
				compartmentCoachType: []tariffError{
					serviceNotAllowedTariffError,
					soldOutTariffError,
				},
			},
			expectBrokenClassesCode: api.BrokenClassesCode_BROKEN_CLASSES_CODE_SOLD_OUT,
			expectOk:                true,
		},
		{
			brokenClasses: map[coachType][]tariffError{
				commonCoachType:      []tariffError{},
				compartmentCoachType: []tariffError{},
			},
			expectBrokenClassesCode: api.BrokenClassesCode_BROKEN_CLASSES_CODE_INVALID,
			expectOk:                false,
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			brokenClassesCode, ok := extractBrokenClassesCode(tc.brokenClasses)
			assert.Equal(t, tc.expectBrokenClassesCode, brokenClassesCode)
			assert.Equal(t, tc.expectOk, ok)
		})
	}
}

func TestParseCarDescriptions(t *testing.T) {
	testCases := []struct {
		carDescriptions      []string
		expectCategoryTraits map[string]bool
	}{
		{
			carDescriptions: []string{
				"МЖ",
				"БН",
				"НФ",
				"Ж",
				"*Ж",
				"*Д",
			},
			expectCategoryTraits: map[string]bool{
				"can_choose_male_female_mix_places": true,
				"places_without_numbers":            true,
				"is_not_firm":                       true,
				"pet_in_coach":                      true,
				"pet_places_only":                   true,
				"is_for_children":                   true,
			},
		},
		{
			carDescriptions: []string{
				"МЖ БН НФ Ж *Ж *Д",
			},
			expectCategoryTraits: map[string]bool{
				"can_choose_male_female_mix_places": true,
				"places_without_numbers":            true,
				"is_not_firm":                       true,
				"pet_in_coach":                      true,
				"pet_places_only":                   true,
				"is_for_children":                   true,
			},
		},
		{
			carDescriptions: []string{
				"* Ж * Д",
			},
			expectCategoryTraits: map[string]bool{
				"can_choose_male_female_mix_places": false,
				"places_without_numbers":            false,
				"is_not_firm":                       false,
				"pet_in_coach":                      false,
				"pet_places_only":                   true,
				"is_for_children":                   true,
			},
		},
		{
			carDescriptions: []string{
				"   * Ж    * Д   ",
			},
			expectCategoryTraits: map[string]bool{
				"can_choose_male_female_mix_places": false,
				"places_without_numbers":            false,
				"is_not_firm":                       false,
				"pet_in_coach":                      false,
				"pet_places_only":                   true,
				"is_for_children":                   true,
			},
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			categoryTraits := parseCarDescriptions(tc.carDescriptions)
			assert.Equal(t, tc.expectCategoryTraits, categoryTraits)
		})
	}
}

func TestParsePlaceReservationTypes(t *testing.T) {
	testCases := []struct {
		imCoachGroup               *models.CarGroup
		expectPlaceReservationType placeReservationType
	}{
		{
			imCoachGroup: &models.CarGroup{
				PlaceReservationTypes: []string{
					"Usual",
				},
			},
			expectPlaceReservationType: usualReservationType,
		},
		{
			imCoachGroup: &models.CarGroup{
				PlaceReservationTypes: []string{
					"TwoPlacesAtOnce",
				},
			},
			expectPlaceReservationType: twoPlacesAtOnceReservationType,
		},
		{
			imCoachGroup: &models.CarGroup{
				PlaceReservationTypes: []string{
					"FourPlacesAtOnce",
				},
			},
			expectPlaceReservationType: fourPlacesAtOnceReservationType,
		},
		{
			imCoachGroup: &models.CarGroup{
				PlaceReservationTypes: []string{
					"",
				},
			},
			expectPlaceReservationType: unknownReservationType,
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			placeReservationType := parsePlaceReservationTypes(tc.imCoachGroup)
			assert.Equal(t, tc.expectPlaceReservationType, placeReservationType)
		})
	}
}

func TestParseTrainTariff(t *testing.T) {
	serviceClass := "3Л"
	testCases := []struct {
		imCoachGroup      *models.CarGroup
		expectTrainTariff *trainTariff
	}{
		{
			imCoachGroup: &models.CarGroup{
				CarType: "Soft",
				PlaceReservationTypes: []string{
					"Usual",
				},
				MinPrice: 1248.0,
				MaxPrice: 2125.0,
				ServiceCosts: []float64{
					125.0,
				},
				TotalPlaceQuantity:     16,
				LowerPlaceQuantity:     5,
				LowerSidePlaceQuantity: 4,
				UpperPlaceQuantity:     1,
				UpperSidePlaceQuantity: 6,
				ServiceClasses: []string{
					"3Л",
				},
				CarDescriptions: []string{
					"МЖ",
				},
				AvailabilityIndication:    "Available",
				IsTransitDocumentRequired: false,
			},
			expectTrainTariff: &trainTariff{
				coachType: softCoachType,
				ticketPrice: &Price{
					value:    1248.0,
					currency: "RUB",
				},
				servicePrice: &Price{
					value:    125.0,
					currency: "RUB",
				},
				severalPrices:             true,
				seats:                     16,
				lowerSeats:                5,
				upperSeats:                1,
				lowerSideSeats:            4,
				upperSideSeats:            6,
				maxSeatsInTheSameCar:      16,
				placeReservationType:      usualReservationType,
				isTransitDocumentRequired: false,
				availabilityIndication:    available,
				serviceClass:              &serviceClass,
				categoryTraits: map[string]bool{
					"can_choose_male_female_mix_places": true,
					"places_without_numbers":            false,
					"is_not_firm":                       false,
					"pet_in_coach":                      false,
					"pet_places_only":                   false,
					"is_for_children":                   false,
				},
			},
		},
	}
	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			trainTariff := parseTrainTariff(tc.imCoachGroup)
			assert.Equal(t, tc.expectTrainTariff, trainTariff)
		})
	}
}
