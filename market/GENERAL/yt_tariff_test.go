package integration

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/yt/go/schema"
	"a.yandex-team.ru/yt/go/yttest"
)

const courierTariffXML = `
<tariff
	id="6313"
	type="COURIER"
	carrier-id="227"
	delivery-method="COURIER"
	is-for-customer="0"
	is-for-shop="1"
>
 <parameters>
  <m3weight>200</m3weight>
  <programs>
   <program name-key="MARKET_DELIVERY"/>
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

func TestOutputTableCreation(t *testing.T) {
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	outputDir := env.TmpPath()
	outputPath := outputDir.Child("table")
	tmpPath := outputDir.Child("tmp").Child("tmp")
	sch, err := schema.Infer(tr.TariffYT{})
	require.NoError(t, err)

	tu := tr.TariffsUploader{
		TableDir: outputDir,
		TmpPath:  tmpPath,
		Schema:   sch,
	}

	tu.InitLogger()
	tu.SetClientAndCtx(env.YT, env.Ctx)
	err = tu.CreateTmpTable()
	require.NoError(t, err)

	buf := bytes.NewBufferString(courierTariffXML)
	tariff, err := tr.Read(buf)
	assert.NoError(t, err)

	tariffs, err := tu.TariffPrepare(tariff, false, make(tr.PointsMap))
	require.NoError(t, err)

	err = tu.SetUpWriter(tmpPath)
	require.NoError(t, err)

	err = tu.WriteTariffs(tariffs)
	require.NoError(t, err)

	err = tu.Writer.Commit()
	require.NoError(t, err)

	err = tu.MoveTmpTable(outputPath)
	require.NoError(t, err)

	finderSet, err := tr.ReadFromYtWithCli(env.YT, outputPath, nil, nil)
	assert.NoError(t, err)
	checkTariffFinder(finderSet.Common, t)
	checkTariffFinder(finderSet.B2B, t)
	checkTariffFinder(finderSet.Nordstream, t)
}

func checkTariffFinder(tariffsFinder *tr.TariffsFinder, t *testing.T) {
	grule, ok := tariffsFinder.GetGlobalRule(6313)
	assert.True(t, ok)
	assert.Equal(t, 6313, int(grule.ID))
	assert.Equal(t, 227, int(grule.DeliveryServiceID))
	assert.Equal(t, 200, int(grule.M3weight))
	assert.Equal(t, enums.DeliveryMethodMask(enums.DeliveryMethodCourier), grule.DeliveryMethod)
	assert.Equal(t, 0.0, grule.RuleAttrs.WeightMin)
	assert.Equal(t, 30000.0, grule.RuleAttrs.WeightMax)
	assert.Equal(t, 101, int(grule.RuleAttrs.WidthMax))
	assert.Equal(t, 103, int(grule.RuleAttrs.HeightMax))
	assert.Equal(t, 102, int(grule.RuleAttrs.LengthMax))
	assert.Equal(t, 180, int(grule.RuleAttrs.DimSumMax))
	assert.Equal(t, [3]uint32{101, 102, 103}, grule.RuleAttrs.SortedDimLimits)

	rules, ok := tariffsFinder.GetRules(tr.FromToRegions{From: 213, To: 2})
	assert.True(t, ok)
	assert.Len(t, rules, 3)
	{
		rule := rules[0]
		assert.Equal(t, 0.0, rule.WeightMin)
		assert.Equal(t, 5000.0, rule.WeightMax)
		assert.Equal(t, 203, int(rule.Option.Cost))
		assert.Equal(t, 1, int(rule.Option.DaysMin))
		assert.Equal(t, 1, int(rule.Option.DaysMax))
	}
	{
		rule := rules[1]
		assert.Equal(t, 5000.0, rule.WeightMin)
		assert.Equal(t, 10000.0, rule.WeightMax)
		assert.Equal(t, 233, int(rule.Option.Cost))
		assert.Equal(t, 1, int(rule.Option.DaysMin))
		assert.Equal(t, 2, int(rule.Option.DaysMax))
	}
	{
		rule := rules[2]
		assert.Equal(t, 10000.0, rule.WeightMin)
		assert.Equal(t, 30000.0, rule.WeightMax)
		assert.Equal(t, 233, int(rule.Option.Cost))
		assert.Equal(t, 2, int(rule.Option.DaysMin))
		assert.Equal(t, 2, int(rule.Option.DaysMax))
	}

	{
		// Нет тарифов, слишком большой платный вес 43200 грамм
		wadList := []tr.WeightAndDim{tr.NewWeightAndDim(10000, 60, 60, 60)}
		fromTo := tr.FromToRegions{From: 213, To: 2}
		optionResults := tariffsFinder.FindOptions(wadList, fromTo, nil)
		assert.Len(t, optionResults, 0)
	}
}
