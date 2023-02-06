package sorting

import (
	"testing"

	"github.com/stretchr/testify/require"

	aviaApi "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	v1 "a.yandex-team.ru/travel/app/backend/api/common/v1"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/avia/library/go/services/featureflag"
	"a.yandex-team.ru/travel/library/go/containers"
)

var (
	defaultBadgesOrder = []aviaApi.Snippet_BadgeType{
		aviaApi.Snippet_BADGE_TYPE_BEST_PRICE,
		aviaApi.Snippet_BADGE_TYPE_POPULAR,
		aviaApi.Snippet_BADGE_TYPE_COMFY,
		aviaApi.Snippet_BADGE_TYPE_AVIACOMPANY_DIRECT_SELLING,
		aviaApi.Snippet_BADGE_TYPE_BOOK_ON_YANDEX,
	}
	mapping = map[aviaApi.Snippet_BadgeType]string{
		aviaApi.Snippet_BADGE_TYPE_BEST_PRICE:                 "TRAVEL_APP_RECOMMENDED_SORT_ENABLE_BEST_PRICE",
		aviaApi.Snippet_BADGE_TYPE_POPULAR:                    "TRAVEL_APP_RECOMMENDED_SORT_ENABLE_POPULAR",
		aviaApi.Snippet_BADGE_TYPE_COMFY:                      "TRAVEL_APP_RECOMMENDED_SORT_ENABLE_COMFY",
		aviaApi.Snippet_BADGE_TYPE_AVIACOMPANY_DIRECT_SELLING: "TRAVEL_APP_RECOMMENDED_SORT_ENABLE_AVIACOMPANY_DIRECT_SELLING",
		aviaApi.Snippet_BADGE_TYPE_BOOK_ON_YANDEX:             "TRAVEL_APP_RECOMMENDED_SORT_ENABLE_BOOK_ON_YANDEX",
	}
	sortBy       = aviaApi.SearchSort_SEARCH_SORT_RECOMMENDED_FIRST
	defaultFlags = featureflag.NewFlags(
		containers.SetOf(
			"TRAVEL_APP_RECOMMENDED_SORT_ENABLE_BEST_PRICE",
			"TRAVEL_APP_RECOMMENDED_SORT_ENABLE_POPULAR",
			"TRAVEL_APP_RECOMMENDED_SORT_ENABLE_COMFY",
			"TRAVEL_APP_RECOMMENDED_SORT_ENABLE_AVIACOMPANY_DIRECT_SELLING",
			"TRAVEL_APP_RECOMMENDED_SORT_ENABLE_BOOK_ON_YANDEX",
		),
		nil,
	)
)

func generateReadySorter() *InterestingSorter {
	sorter := NewInterestingSorter(&InterestingSorterConfig{
		BadgesOrder:             defaultBadgesOrder,
		BadgesToFlagCodeMapping: mapping,
	}, featureflag.NewMockStorage(&defaultFlags))

	return sorter
}

func TestInterestingSortSimple(t *testing.T) {
	elem1 := &aviaApi.Snippet{
		Key:      "key1",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key11",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    400,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges: []*aviaApi.Snippet_Badge{
			{
				Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE,
			},
		},
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	elem2 := &aviaApi.Snippet{
		Key:      "key2",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key2",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    500,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges: []*aviaApi.Snippet_Badge{
			{
				Type: aviaApi.Snippet_BADGE_TYPE_COMFY,
			},
		},
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	elem3 := &aviaApi.Snippet{
		Key:      "key3",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key2",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    600,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges: []*aviaApi.Snippet_Badge{
			{
				Type: aviaApi.Snippet_BADGE_TYPE_POPULAR,
			},
		},
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	elem4 := &aviaApi.Snippet{
		Key:      "key4",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key2",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    700,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges:                  nil,
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	elem5 := &aviaApi.Snippet{
		Key:      "key5",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key2",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    800,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges: []*aviaApi.Snippet_Badge{
			{
				Type: aviaApi.Snippet_BADGE_TYPE_BOOK_ON_YANDEX,
			},
		},
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	snippets := []*aviaApi.Snippet{elem1, elem2, elem3, elem4, elem5}

	sorter := generateReadySorter()
	snippets = sorter.Sort(snippets, map[string]*aviaSearchProto.Flight{}, sortBy)

	require.Equal(t, []*aviaApi.Snippet{elem1, elem3, elem2, elem5, elem4}, snippets)
}

func TestInterestingSortCombined(t *testing.T) {
	elem1 := &aviaApi.Snippet{
		Key:      "key1",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key11",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    400,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges: []*aviaApi.Snippet_Badge{
			{
				Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE,
			},
			{
				Type: aviaApi.Snippet_BADGE_TYPE_AVIACOMPANY_DIRECT_SELLING,
			},
		},
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	elem2 := &aviaApi.Snippet{
		Key:      "key2",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key2",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    500,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges: []*aviaApi.Snippet_Badge{
			{
				Type: aviaApi.Snippet_BADGE_TYPE_COMFY,
			},
			{
				Type: aviaApi.Snippet_BADGE_TYPE_BOOK_ON_YANDEX,
			},
		},
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	elem3 := &aviaApi.Snippet{
		Key:      "key3",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key2",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    600,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges: []*aviaApi.Snippet_Badge{
			{
				Type: aviaApi.Snippet_BADGE_TYPE_COMFY,
			},
			{
				Type: aviaApi.Snippet_BADGE_TYPE_POPULAR,
			},
		},
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	elem4 := &aviaApi.Snippet{
		Key:      "key4",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key2",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    700,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges:                  nil,
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	elem5 := &aviaApi.Snippet{
		Key:      "key5",
		Forward:  nil,
		Backward: nil,
		Variant: &aviaApi.Snippet_Variant{
			Key:         "key2",
			PartnerCode: "",
			Price: &v1.Price{
				Currency: "RUB",
				Value:    800,
			},
			Baggage:          nil,
			CarryOn:          nil,
			Refund:           nil,
			OrderRelativeUrl: "",
		},
		Badges: []*aviaApi.Snippet_Badge{
			{
				Type: aviaApi.Snippet_BADGE_TYPE_COMFY,
			},
		},
		Transfers:               nil,
		ForwardDurationMinutes:  0,
		BackwardDurationMinutes: 0,
	}

	snippets := []*aviaApi.Snippet{elem1, elem2, elem3, elem4, elem5}

	sorter := generateReadySorter()
	snippets = sorter.Sort(snippets, map[string]*aviaSearchProto.Flight{}, sortBy)

	require.Equal(t, []*aviaApi.Snippet{elem1, elem3, elem2, elem4, elem5}, snippets)
}

func TestGetFlags(t *testing.T) {
	flags := featureflag.NewFlags(
		containers.SetOf[string](
			"TRAVEL_APP_RECOMMENDED_SORT_ENABLE_BEST_PRICE",
			"TRAVEL_APP_RECOMMENDED_SORT_ENABLE_COMFY",
			"TRAVEL_APP_RECOMMENDED_SORT_ENABLE_BOOK_ON_YANDEX",
		),
		containers.SetOf[string](),
	)
	sorter := NewInterestingSorter(&InterestingSorterConfig{
		BadgesOrder:             defaultBadgesOrder,
		BadgesToFlagCodeMapping: mapping,
	}, featureflag.NewMockStorage(&flags))

	require.Equal(
		t,
		[]aviaApi.Snippet_BadgeType{
			aviaApi.Snippet_BADGE_TYPE_BEST_PRICE,
			aviaApi.Snippet_BADGE_TYPE_COMFY,
			aviaApi.Snippet_BADGE_TYPE_BOOK_ON_YANDEX,
		},
		sorter.GetActualBadgesOrder(),
	)
}
