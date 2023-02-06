package tarifficator

import (
	"errors"
	"io"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
)

const pickupTariffXML1 = `
<tariff
	id="7001"
	carrier-id="1"
	delivery-method="PICKUP"
	is-for-customer="0"
	is-for-shop="1"
>
 <parameters>
  <m3weight>200</m3weight>
  <programs>
   <program name-key="MARKET_DELIVERY"/>
  </programs>
 </parameters>
 <rules>
  <offer-rule weight-max="30000" width-max="101" height-max="103" length-max="102" dim-sum-max="180">
   <location-rule from="213">
    <location-rule to="2">
     <offer-rule weight-max="5000">
       <pickuppoint id="10000752228" code="197N"/>
       <pickuppoint id="10000756339" code="197N"/>
     </offer-rule>
    </location-rule>
   </location-rule>
   <location-rule from="2">
    <location-rule to="2">
     <offer-rule weight-max="5000">
       <pickuppoint id="10000752228" code="197N"/>
       <pickuppoint id="10000756339" code="197N"/>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
`

const pickupTariffXML2 = `
<tariff
	id="10007001"
	carrier-id="1"
	delivery-method="PICKUP"
	is-for-customer="0"
	is-for-shop="1"
>
 <parameters>
  <m3weight>200</m3weight>
  <programs>
   <program name-key="MARKET_DELIVERY"/>
  </programs>
 </parameters>
 <rules>
  <offer-rule weight-max="30000" width-max="101" height-max="103" length-max="102" dim-sum-max="180">
   <location-rule from="213">
    <location-rule to="2">
     <offer-rule weight-max="5000">
       <pickuppoint id="10000752228" code="197N"/>
       <pickuppoint id="10000756339" code="197N"/>
       <pickuppoint id="10000880055" code="197N"/>
     </offer-rule>
    </location-rule>
   </location-rule>
   <location-rule from="2">
    <location-rule to="2">
     <offer-rule weight-max="5000">
       <pickuppoint id="10000752228" code="197N"/>
       <pickuppoint id="10000756339" code="197N"/>
       <pickuppoint id="10000880055" code="197N"/>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
`

const pickupTariffXML3 = `
<tariff
	id="10007002"
	carrier-id="2"
	delivery-method="COURIER"
	is-for-customer="0"
	is-for-shop="1"
>
 <parameters>
  <m3weight>200</m3weight>
  <programs>
   <program name-key="MARKET_DELIVERY"/>
  </programs>
 </parameters>
 <rules>
  <offer-rule weight-max="30000" width-max="101" height-max="103" length-max="102" dim-sum-max="180">
   <location-rule from="213">
    <location-rule to="2">
     <offer-rule weight-max="5000">
       <pickuppoint id="10000752229" code="197N"/>
       <pickuppoint id="10000756339" code="197N"/>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
`

const pickupTariffXML4 = `
<tariff
	id="10007003"
	carrier-id="3"
	delivery-method="PICKUP"
	is-for-customer="0"
	is-for-shop="1"
>
 <parameters>
  <m3weight>200</m3weight>
  <programs>
   <program name-key="MARKET_DELIVERY"/>
  </programs>
 </parameters>
 <rules>
  <offer-rule weight-max="30000" width-max="101" height-max="103" length-max="102" dim-sum-max="180">
   <location-rule from="213">
    <location-rule to="2">
     <offer-rule weight-max="5000">
       <pickuppoint id="10000752229" code="197N"/>
       <pickuppoint id="10000756340" code="197N"/>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
`

const b2bCourierTariffXML = `
<tariff
	id="10007004"
	type="COURIER"
	carrier-id="227"
	delivery-method="COURIER"
	is-for-customer="0"
	is-for-shop="1"
>
 <parameters>
  <m3weight>200</m3weight>
  <programs>
   <program name-key="B2B"/>
  </programs>
 </parameters>
 <rules>
  <offer-rule weight-max="30000" width-max="101" height-max="103" length-max="102" dim-sum-max="180">
   <location-rule from="213">
    <location-rule to="2">
     <offer-rule weight-max="5000">
      <option cost="203" delta-cost="0" days-min="1" days-max="1" scale="1000"/>
     </offer-rule>
     <offer-rule weight-min="5000" weight-max="10000">
      <option cost="233" delta-cost="1" days-min="1" days-max="2" scale="1000"/>
     </offer-rule>
     <offer-rule weight-min="10000" weight-max="30000">
      <option cost="233" delta-cost="20" days-min="2" days-max="2" scale="1000"/>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
`

func BytesGetterTest(path string) (io.ReadCloser, error) {
	switch path {
	case "a":
		return io.NopCloser(strings.NewReader(pickupTariffXML1)), nil
	case "b":
		return io.NopCloser(strings.NewReader(pickupTariffXML2)), nil
	case "c":
		return io.NopCloser(strings.NewReader(pickupTariffXML3)), nil
	case "d":
		return io.NopCloser(strings.NewReader(pickupTariffXML4)), nil
	case "e":
		return io.NopCloser(strings.NewReader(b2bCourierTariffXML)), nil
	default:
		return nil, errors.New("unknown file")
	}
}

type MockWriter struct {
	tariffs []*TariffYT
}

func (m *MockWriter) Write(value interface{}) error {
	m.tariffs = append(m.tariffs, value.(*TariffYT))
	return nil
}
func (m *MockWriter) Commit() error {
	return nil
}
func (m *MockWriter) Rollback() error {
	return nil
}

type MockLmsClient struct {
}

func (m *MockLmsClient) IsMarketCourier(partnerID int) (bool, error) {
	return partnerID != 1, nil
}

/*
 TODO panic on IterateTariffs
func TestIterateAndSortTariffs(t *testing.T) {
	tw := TariffWriter{
		BytesGetter: BytesGetterTest,
	}

	_, err := tw.IterateTariffs([]string{
		"b", "f.tmp", "d", "c", "a",
	}, WriterConfig{})

	assert.NoError(t, err)

	{
		tariff := tariffs[0]
		assert.Equal(t, "a", tariff.Path)
		assert.Equal(t, 7001, tariff.Header.ID)
		assert.Equal(t, 1, tariff.Header.CarrierID)
	}
	{
		tariff := tariffs[1]
		assert.Equal(t, "b", tariff.Path)
		assert.Equal(t, 10007001, tariff.Header.ID)
		assert.Equal(t, 1, tariff.Header.CarrierID)
	}
	{
		tariff := tariffs[2]
		assert.Equal(t, "c", tariff.Path)
		assert.Equal(t, 10007002, tariff.Header.ID)
		assert.Equal(t, 2, tariff.Header.CarrierID)
	}
	{
		tariff := tariffs[3]
		assert.Equal(t, "d", tariff.Path)
		assert.Equal(t, 10007003, tariff.Header.ID)
		assert.Equal(t, 3, tariff.Header.CarrierID)
	}
}

*/

func TestProcessTariffs(t *testing.T) {

	writer := MockWriter{}
	tw := TariffsUploader{
		BytesGetter: BytesGetterTest,
		Writer:      &writer,
		LmsClient:   &MockLmsClient{},
	}
	tw.InitLogger()

	fileNames := []string{
		"a", "b", "c", "d", "e",
	}
	metaList, err := tw.getTariffMetaList(fileNames)
	assert.NoError(t, err)
	assert.Len(t, metaList, 5)

	err = tw.writeRows(metaList, nil)
	assert.NoError(t, err)

	{
		tariff := writer.tariffs[0]
		assert.Equal(t, uint64(7001), tariff.ID)
		points := tariff.Points
		assert.Equal(t, 0, len(points))
		assert.Equal(t, false, tariff.IsMarketCourier)
	}
	{
		tariff := writer.tariffs[1]
		assert.Equal(t, uint64(7001), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 2, len(points))
		assert.Equal(t, int64(10000752228), points[0].ID)
		assert.Equal(t, int64(10000756339), points[1].ID)
		assert.Equal(t, false, tariff.IsMarketCourier)
	}
	{
		tariff := writer.tariffs[2]
		assert.Equal(t, uint64(7001), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 2, len(points))
		assert.Equal(t, int64(10000752228), points[0].ID)
		assert.Equal(t, int64(10000756339), points[1].ID)
		assert.Equal(t, false, tariff.IsMarketCourier)
	}
	{
		tariff := writer.tariffs[3]
		assert.Equal(t, uint64(10007001), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 0, len(points))
		assert.Equal(t, false, tariff.IsMarketCourier)
	}
	// еслибудут скачаны (по ошибке) тарифы помеченые IsForCustomer,
	//то они будут распаршены как IsForShop.
	{
		tariff := writer.tariffs[4]
		assert.Equal(t, uint64(10007001), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 1, len(points))
		assert.Equal(t, int64(10000880055), points[0].ID)
		assert.Equal(t, false, tariff.IsMarketCourier)
	}
	{
		tariff := writer.tariffs[5]
		assert.Equal(t, uint64(10007001), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 1, len(points))
		assert.Equal(t, int64(10000880055), points[0].ID)
		assert.Equal(t, false, tariff.IsMarketCourier)
	}
	{
		tariff := writer.tariffs[6]
		assert.Equal(t, uint64(10007002), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 0, len(points))
		assert.Equal(t, true, tariff.IsMarketCourier)
		assert.Equal(t, "COURIER", tariff.DeliveryMethod)
	}
	{
		tariff := writer.tariffs[7]
		assert.Equal(t, uint64(10007002), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 1, len(points))
		assert.Equal(t, int64(10000752229), points[0].ID)
		assert.Equal(t, true, tariff.IsMarketCourier)
		assert.Equal(t, "COURIER", tariff.DeliveryMethod)
	}
	{
		tariff := writer.tariffs[8]
		assert.Equal(t, uint64(10007003), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 0, len(points))
		assert.Equal(t, true, tariff.IsMarketCourier)
		assert.Equal(t, "PICKUP", tariff.DeliveryMethod)
	}
	{
		tariff := writer.tariffs[9]
		assert.Equal(t, uint64(10007003), tariff.ID)

		points := tariff.Points
		assert.Equal(t, 1, len(points))
		assert.Equal(t, int64(10000756340), points[0].ID)
		assert.Equal(t, true, tariff.IsMarketCourier)
		assert.Equal(t, "PICKUP", tariff.DeliveryMethod)
	}
	{
		// b2b courier
		assert.Equal(t, uint64(10007004), writer.tariffs[10].ID)
		assert.Equal(t, uint64(10007004), writer.tariffs[11].ID)
		assert.Equal(t, uint64(10007004), writer.tariffs[12].ID)
	}
}
