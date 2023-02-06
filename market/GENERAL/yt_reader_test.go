package dsbs

import (
	"bytes"
	"testing"
	"unsafe"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/ytutil"
)

func TestReadDsbsCourierTariffs(t *testing.T) {
	yson := `{"shop_id"=10796386;"region_group_id"=447;"partner_id"=48638;"tariff_type"="DEFAULT";"payment_types"=#;"from"=215;"to"=215;"price_from"=100000;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10792440;"region_group_id"=451;"partner_id"=48616;"tariff_type"="DEFAULT";"payment_types"=#;"from"=972;"to"=972;"price_from"=#;"price_to"=100000;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10809340;"region_group_id"=514;"partner_id"=48825;"tariff_type"="DEFAULT";"payment_types"=#;"from"=10740;"to"=10740;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10798610;"region_group_id"=519;"partner_id"=48654;"tariff_type"="UNIFORM";"payment_types"=#;"from"=75;"to"=75;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=%true;"days_from"=1;"days_to"=1;"order_before_hour"=13;"market_pickup_enabled"=%false;};
	{"shop_id"=10798771;"region_group_id"=520;"partner_id"=48656;"tariff_type"="UNIFORM";"payment_types"=#;"from"=43;"to"=43;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=%true;"days_from"=0;"days_to"=0;"order_before_hour"=19;"market_pickup_enabled"=%false;};
	{"shop_id"=10808728;"region_group_id"=591;"partner_id"=48818;"tariff_type"="UNIFORM";"payment_types"=#;"from"=47;"to"=47;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=%true;"days_from"=0;"days_to"=0;"order_before_hour"=22;"market_pickup_enabled"=%false;};
	{"shop_id"=10810316;"region_group_id"=764;"partner_id"=48830;"tariff_type"="UNIFORM";"payment_types"=#;"from"=20636;"to"=20636;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=%true;"days_from"=3;"days_to"=3;"order_before_hour"=13;"market_pickup_enabled"=%true;};
	{"shop_id"=10802174;"region_group_id"=779;"partner_id"=48744;"tariff_type"="DEFAULT";"payment_types"=#;"from"=65;"to"=65;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10811891;"region_group_id"=637;"partner_id"=48891;"tariff_type"="FROM_FEED";"payment_types"=#;"from"=10716;"to"=10716;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10811892;"region_group_id"=637;"partner_id"=48892;"tariff_type"="FROM_FEED";"payment_types"=#;"from"=10716;"to"=10716;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=%false;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10812050;"region_group_id"=738;"partner_id"=48894;"tariff_type"="WEIGHT";"payment_types"=#;"from"=10740;"to"=10740;"price_from"=#;"price_to"=#;"weight_from"=2;"weight_to"=5;"feed_id"=#;"category_id"=#;"has_delivery"=%true;"days_from"=2;"days_to"=2;"order_before_hour"=13;"market_pickup_enabled"=%false;};
	{"shop_id"=10812050;"region_group_id"=738;"partner_id"=48894;"tariff_type"="WEIGHT";"payment_types"=#;"from"=10740;"to"=10740;"price_from"=#;"price_to"=#;"weight_from"=5;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=%true;"days_from"=2;"days_to"=2;"order_before_hour"=13;"market_pickup_enabled"=%true;};
	{"shop_id"=10812050;"region_group_id"=738;"partner_id"=48894;"tariff_type"="WEIGHT";"payment_types"=#;"from"=10740;"to"=10740;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=2;"feed_id"=#;"category_id"=#;"has_delivery"=%true;"days_from"=1;"days_to"=1;"order_before_hour"=13;"market_pickup_enabled"=%true;};
	{"shop_id"=10828860;"region_group_id"=706;"partner_id"=49083;"tariff_type"="DEFAULT";"payment_types"=#;"from"=65;"to"=65;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=%false;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10836579;"region_group_id"=570;"partner_id"=49150;"tariff_type"="DEFAULT";"payment_types"=#;"from"=56;"to"=56;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10836702;"region_group_id"=571;"partner_id"=49152;"tariff_type"="DEFAULT";"payment_types"=#;"from"=56;"to"=56;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10836733;"region_group_id"=573;"partner_id"=49154;"tariff_type"="DEFAULT";"payment_types"=#;"from"=56;"to"=56;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10843442;"region_group_id"=2758;"partner_id"=49234;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10843655;"region_group_id"=2759;"partner_id"=49235;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10843941;"region_group_id"=2760;"partner_id"=49236;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10844117;"region_group_id"=2761;"partner_id"=49237;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10844303;"region_group_id"=2762;"partner_id"=49238;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%true;};
	{"shop_id"=10842200;"region_group_id"=3096;"partner_id"=49213;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10842270;"region_group_id"=3097;"partner_id"=49214;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10842428;"region_group_id"=3098;"partner_id"=49215;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};
	{"shop_id"=10842449;"region_group_id"=3099;"partner_id"=49216;"tariff_type"="DEFAULT";"payment_types"=#;"from"=213;"to"=213;"price_from"=#;"price_to"=#;"weight_from"=#;"weight_to"=#;"feed_id"=#;"category_id"=#;"has_delivery"=#;"days_from"=#;"days_to"=#;"order_before_hour"=#;"market_pickup_enabled"=%false;};`
	buf := bytes.NewBufferString(yson)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)

	scanner := NewShopCourierScanner(nil, nil)
	err = doRead(reader, scanner)
	require.NoError(t, err)
	require.Len(t, scanner.Tariffs, 8)

	for _, sh := range scanner.Tariffs {
		for _, trf := range sh {
			if trf.PriceFrom != 0 {
				require.Equal(t, uint64(1000), trf.PriceFrom)
			}
			if trf.PriceTo != MaxPrice {
				require.Equal(t, uint64(1000), trf.PriceTo)
			}
		}
	}

	require.Len(t, scanner.DSBSToOutletShops, 2)
	require.Contains(t, scanner.DSBSToOutletShops, uint64(10810316))
	require.Contains(t, scanner.DSBSToOutletShops, uint64(10812050))
}

func TestReadDsbsPickupPointTariffs(t *testing.T) {
	yson := `{"id"=1;"logistics_point_id"=10001011909;"shop_id"=10738162;"partner_id"=48701;"price_from"=#;"price_to"=#;"order_before_hour"=4;"days_from"=2;"days_to"=2;"cost"=0;};
	{"id"=3;"logistics_point_id"=10001013705;"shop_id"=11011032;"partner_id"=50019;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=1;"days_to"=1;"cost"=0;};
	{"id"=4;"logistics_point_id"=10001013706;"shop_id"=11065403;"partner_id"=50502;"price_from"=#;"price_to"=#;"order_before_hour"=#;"days_from"=#;"days_to"=#;"cost"=#;};
	{"id"=5;"logistics_point_id"=10001013707;"shop_id"=11065403;"partner_id"=50502;"price_from"=#;"price_to"=500000;"order_before_hour"=19;"days_from"=5;"days_to"=5;"cost"=90000;};
	{"id"=6;"logistics_point_id"=10001013708;"shop_id"=10753032;"partner_id"=48746;"price_from"=#;"order_before_hour"=24;"days_from"=0;"days_to"=0;"cost"=0;};
	{"id"=7;"logistics_point_id"=10001013710;"shop_id"=10807362;"partner_id"=48791;"price_to"=#;"order_before_hour"=24;"days_from"=1;"days_to"=1;"cost"=200000;};
	{"id"=8;"logistics_point_id"=10001013709;"shop_id"=10747887;"partner_id"=48703;"price_from"=#;"price_to"=#;"order_before_hour"=#;"days_from"=#;"days_to"=#;"cost"=0;};
	{"id"=9;"logistics_point_id"=10001013711;"shop_id"=10456107;"partner_id"=48700;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=1;"days_to"=1;};
	{"id"=10;"logistics_point_id"=10001013712;"shop_id"=10727225;"partner_id"=48803;"price_from"=#;"price_to"=#;"order_before_hour"=#;"days_from"=#;"days_to"=#;"cost"=0;};
	{"id"=11;"logistics_point_id"=10001013713;"shop_id"=10809137;"partner_id"=48823;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=1;"days_to"=1;"cost"=0;};
	{"id"=12;"logistics_point_id"=10001013714;"shop_id"=10937435;"partner_id"=49680;"order_before_hour"=24;"days_from"=0;"days_to"=0;};
	{"id"=13;"logistics_point_id"=10001013715;"shop_id"=10934514;"partner_id"=49657;"price_from"=#;"price_to"=#;"order_before_hour"=#;"days_from"=#;"days_to"=#;"cost"=0;};
	{"id"=14;"logistics_point_id"=10001013716;"shop_id"=10456107;"partner_id"=48700;"price_from"=#;"price_to"=#;"order_before_hour"=#;"days_from"=#;"days_to"=#;"cost"=0;};
	{"id"=15;"logistics_point_id"=10001013717;"shop_id"=10930334;"partner_id"=49616;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=2;"days_to"=2;"cost"=0;};
	{"id"=16;"logistics_point_id"=10001013718;"shop_id"=11031140;"partner_id"=50149;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=2;"days_to"=2;"cost"=0;};
	{"id"=17;"logistics_point_id"=10001013719;"shop_id"=10796156;"partner_id"=48634;"price_from"=#;"price_to"=#;"order_before_hour"=#;"days_from"=#;"days_to"=#;"cost"=0;};
	{"id"=18;"logistics_point_id"=10001013720;"shop_id"=10801284;"partner_id"=48704;"price_from"=#;"price_to"=#;"order_before_hour"=#;"days_from"=#;"days_to"=#;"cost"=0;};
	{"id"=19;"logistics_point_id"=10001013721;"shop_id"=10806905;"partner_id"=48772;"price_from"=#;"price_to"=#;"order_before_hour"=20;"days_from"=0;"days_to"=0;"cost"=0;};
	{"id"=20;"logistics_point_id"=10001013724;"shop_id"=10671338;"partner_id"=48769;"price_from"=#;"price_to"=#;"order_before_hour"=#;"days_from"=#;"days_to"=#;"cost"=0;};
	{"id"=21;"logistics_point_id"=10001013722;"shop_id"=10942440;"partner_id"=49732;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=2;"days_to"=2;"cost"=20000;};
	{"id"=22;"logistics_point_id"=10001013723;"shop_id"=10832356;"partner_id"=49125;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=3;"days_to"=3;"cost"=#;};
	{"id"=23;"logistics_point_id"=10001013810;"shop_id"=11077829;"partner_id"=50948;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=4;"days_to"=4;"cost"=0;};
	{"id"=24;"logistics_point_id"=10001013815;"shop_id"=10456100;"partner_id"=48788;"price_from"=#;"price_to"=#;"order_before_hour"=24;"days_from"=1;"days_to"=1;"cost"=0;};`
	buf := bytes.NewBufferString(yson)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)

	pointWithRegion := int64(10001013710)
	regionMap := geobase.NewExample()
	outletIds := []int64{
		10001011909,
		10001013810,
		10001013815,
	}
	var i int64
	for i < 19 {
		outletIds = append(outletIds, 10001013705+i)
		i++
	}
	outl := []outlets.Outlet{}
	for _, oID := range outletIds {
		outl = append(outl, outlets.Outlet{
			ID:       oID,
			RegionID: geobase.RegionMoscow,
		})
	}
	ous := outlets.Make(outl, &regionMap, nil)

	scanner := NewShopPickupPointScanner(ous)
	err = doRead(reader, scanner)
	require.NoError(t, err)
	require.Len(t, scanner.Tariffs, 15)

	checked := false
	for _, tariffs := range scanner.Tariffs {
		for _, tariff := range tariffs {
			for _, point := range tariff.LogisticsPoints {
				if point == pointWithRegion {
					require.Equal(t, geobase.RegionMoscow, tariff.RegionID)
					checked = true
				}
			}
		}
	}
	require.True(t, checked)
}

func TestSizeof(t *testing.T) {
	assert.Equal(t, 72, int(unsafe.Sizeof(ShopCourierTariffRT{})), "ShopCourierTariffRT")         // 104 => 72
	assert.Equal(t, 56, int(unsafe.Sizeof(ShopPickupPointTariffRT{})), "ShopPickupPointTariffRT") // 72 => 56
}
