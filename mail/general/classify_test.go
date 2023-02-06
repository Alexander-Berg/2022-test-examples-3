package widgets

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/experiments"
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/types"
	"a.yandex-team.ru/mail/iex/taksa/widgets/avia"
	"a.yandex-team.ru/mail/iex/taksa/widgets/bigimage"
	"a.yandex-team.ru/mail/iex/taksa/widgets/bounce"
	"a.yandex-team.ru/mail/iex/taksa/widgets/calendar"
	"a.yandex-team.ru/mail/iex/taksa/widgets/eshop"
	"a.yandex-team.ru/mail/iex/taksa/widgets/hotels"
	"a.yandex-team.ru/mail/iex/taksa/widgets/onelink"
	"a.yandex-team.ru/mail/iex/taksa/widgets/pkpass"
	"a.yandex-team.ru/mail/iex/taksa/widgets/snippet"
	"a.yandex-team.ru/mail/iex/taksa/widgets/tracker"
	"a.yandex-team.ru/mail/iex/taksa/widgets/urlinfo"
	"a.yandex-team.ru/mail/iex/taksa/widgets/yandexpayment"
	"sort"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestClassify_typesMatchAvia_returnsAvia(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{Avia: avia.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: avia.Class{}})
}

func TestClassify_typesMatchHotels_returnsHotels(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		Hotels: hotels.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: hotels.Class{}})
}

func TestClassify_typesMatchPkpass_returnsPkpass(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		Pkpass: pkpass.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: pkpass.Class{}})
}

func TestClassify_typesMatchBounce_returnsBounce(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		Bounce: bounce.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: bounce.Class{}})
}

func TestClassify_typesMatchOneLink_returnsOneLink(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		OneLink: onelink.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: onelink.Class{}})
}

func TestClassify_typesMatchEshop_returnsEshop(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		Eshop: eshop.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: eshop.Class{}})
}

func TestClassify_typesMatchSnippet_returnsSnippet(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		Snippet: snippet.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: snippet.Class{}})
}

func TestClassify_typesMatchTracker_returnsTracker(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		Tracker: tracker.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: tracker.Class{}})
}

func TestClassify_typesMatchUrlInfo_returnsUrlInfo(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		URLInfo: urlinfo.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: urlinfo.Class{}})
}

func TestClassify_typesMatchBigImage_returnsBigImage(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		BigImage: bigimage.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: bigimage.Class{}})
}

func TestClassify_typesMatchCalendar_returnsCalendar(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{
		Calendar: calendar.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: calendar.Class{}})
}

func TestClassify_experimentsMatchCalendar_returnsCalendar(t *testing.T) {
	types, _ := types.Parse("1")
	exps := experiments.Experiments([]string{"86805"})
	cfg := Config{Calendar: calendar.Config{Types: types, Experiments: exps}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	req := request.Mock{Headers: map[string]string{"X-Yandex-Enabledexpboxes": "85812,0,82;86805,0,36;87370,0,38"}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, req)
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: calendar.Class{}})
}

func TestClassify_experimentsMatchCalendar_returnsNil(t *testing.T) {
	types, _ := types.Parse("1")
	exps := experiments.Experiments([]string{"86805"})
	cfg := Config{Calendar: calendar.Config{Types: types, Experiments: exps}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	req := request.Mock{Headers: map[string]string{"X-Yandex-Enabledexpboxes": "85812,0,82;86945,0,36;87370,0,38"}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, req)
	AssertThat(t, len(res), Is{V: 0})
}

func TestClassify_without_header_returnsNil(t *testing.T) {
	types, _ := types.Parse("1")
	exps := experiments.Experiments([]string{"86805", "83872"})
	cfg := Config{Avia: avia.Config{Types: types, Experiments: exps}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 0})
}

func TestClassify_without_widget_exps_returnsAvia(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{Avia: avia.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	req := request.Mock{Headers: map[string]string{"X-Yandex-Enabledexpboxes": "85812,0,82;86945,0,36;87370,0,38"}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, req)
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: avia.Class{}})
}

func TestClassify_without_exps_returnsAvia(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{Avia: avia.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: avia.Class{}})
}

func TestClassify_typesMatchNone_returnsNil(t *testing.T) {
	types, _ := types.Parse("1")
	cfg := Config{Avia: avia.Config{Types: types}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{2}}, IEX: []interface{}{[]int{2}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 0})
}

func TestClassify_fewWidgets_notEmptyFacts(t *testing.T) {
	aviaTypes, _ := types.Parse("5&16")
	hotelsTypes, _ := types.Parse("35")
	yandexPaymentTypes, _ := types.Parse("74")
	eshopTypes, _ := types.Parse("6&23")
	cfg := Config{
		Avia:          avia.Config{Types: aviaTypes},
		Hotels:        hotels.Config{Types: hotelsTypes},
		YandexPayment: yandexpayment.Config{Types: yandexPaymentTypes},
		Eshop:         eshop.Config{Types: eshopTypes}}
	fact := iex.Fact{
		Envelope: meta.Envelope{Types: []int{5, 6, 16, 19, 20, 35, 55, 74}},
		IEX:      []interface{}{[]int{1}}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 3})
	AssertThat(t, res[0], TypeOf{V: yandexpayment.Class{}})
	AssertThat(t, res[1], TypeOf{V: hotels.Class{}})
	AssertThat(t, res[2], TypeOf{V: avia.Class{}})
}

func TestClassify_fewWidgets_emptyFacts(t *testing.T) {
	aviaTypes, _ := types.Parse("5&16")
	hotelsTypes, _ := types.Parse("35")
	yandexPaymentTypes, _ := types.Parse("74")
	eshopTypes, _ := types.Parse("6&23")
	cfg := Config{
		Avia:          avia.Config{Types: aviaTypes},
		Hotels:        hotels.Config{Types: hotelsTypes},
		YandexPayment: yandexpayment.Config{Types: yandexPaymentTypes},
		Eshop:         eshop.Config{Types: eshopTypes}}
	fact := iex.Fact{
		Envelope: meta.Envelope{Types: []int{5, 6, 16, 19, 20, 35, 55, 74}},
		IEX:      []interface{}{}}
	res := Widgets{cfg}.classify(cfg, fact, logger.Mock{}, client.Mock{}, request.Mock{})
	AssertThat(t, len(res), Is{V: 1})
	AssertThat(t, res[0], TypeOf{V: yandexpayment.Class{}})
}

func TestWidgetable_mixedInput_filteredOutput(t *testing.T) {
	types12, _ := types.Parse("1&2")
	types23, _ := types.Parse("2&3")
	cfg := Config{Avia: avia.Config{Types: types12}, Hotels: hotels.Config{Types: types23}}
	all := []meta.Envelope{
		{Mid: "a", Types: []int{1, 2}},
		{Mid: "c", Types: []int{}},
		{Mid: "e", Types: []int{2, 3}},
		{Mid: "g", Types: []int{4, 5}},
		{Mid: "i", Types: []int{1, 3}},
		{Mid: "k", Types: []int{2, 5}}}
	expected := []meta.Envelope{
		{Mid: "a", Types: []int{1, 2}},
		{Mid: "e", Types: []int{2, 3}}}
	actual := Widgets{cfg}.Widgetable(all)
	AssertThat(t, actual, EqualTo{V: expected})
}

func TestIsWidgetEnabled_enabled(t *testing.T) {
	widgetTypes, _ := types.Parse("14&51|90")
	widgetExps := experiments.Experiments([]string{"83010", "37912"})
	envelopeTypes := sort.IntSlice([]int{12, 14, 90})
	userExps := experiments.Experiments([]string{"67381", "83010"})
	cfg := avia.Config{Types: widgetTypes, Experiments: widgetExps}
	fact := iex.Fact{Envelope: meta.Envelope{Types: envelopeTypes}, IEX: []interface{}{}}
	result := isWidgetEnabled(cfg, fact, userExps)
	AssertThat(t, result, EqualTo{V: true})
}

func TestIsWidgetEnabled_not_enabled(t *testing.T) {
	widgetTypes, _ := types.Parse("14&51|90")
	widgetExps := experiments.Experiments([]string{"93712", "37912"})
	envelopeTypes := sort.IntSlice([]int{12, 14, 90})
	userExps := experiments.Experiments([]string{"67381", "83010", "30029"})
	cfg := avia.Config{Types: widgetTypes, Experiments: widgetExps}
	fact := iex.Fact{Envelope: meta.Envelope{Types: envelopeTypes}, IEX: []interface{}{}}
	result := isWidgetEnabled(cfg, fact, userExps)
	AssertThat(t, result, EqualTo{V: false})
}

func TestIsWidgetEnabled_without_widget_exps(t *testing.T) {
	widgetTypes, _ := types.Parse("14&51|90")
	envelopeTypes := sort.IntSlice([]int{12, 14, 51})
	userExps := experiments.Experiments([]string{"67381", "83010"})
	cfg := avia.Config{Types: widgetTypes}
	fact := iex.Fact{Envelope: meta.Envelope{Types: envelopeTypes}, IEX: []interface{}{}}
	result := isWidgetEnabled(cfg, fact, userExps)
	AssertThat(t, result, EqualTo{V: true})
}

func TestIsWidgetEnabled_without_user_exps(t *testing.T) {
	widgetTypes, _ := types.Parse("14&51|90")
	widgetExps := experiments.Experiments(nil)
	envelopeTypes := sort.IntSlice([]int{12, 14, 51})
	userExps, _ := experiments.ParseHeader("")
	cfg := avia.Config{Types: widgetTypes, Experiments: widgetExps}
	fact := iex.Fact{Envelope: meta.Envelope{Types: envelopeTypes}, IEX: []interface{}{}}
	result := isWidgetEnabled(cfg, fact, userExps)
	AssertThat(t, result, EqualTo{V: true})
}
