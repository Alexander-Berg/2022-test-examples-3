package ycombo

import (
	"bytes"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"

	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestResources(t *testing.T) {
	for _, n := range availableTemplates {
		if n == "index.html" {
			continue
		}
		n = strings.Split(n, ".")[0]
		require.NotNil(t, GetTemplate().Lookup(n))
	}
	{
		buffer := bytes.NewBuffer(make([]byte, 0))
		require.NoError(t, executeForm(buffer, FormData{}))
	}
	{
		buffer := bytes.NewBuffer(make([]byte, 0))
		require.NoError(t, executeCart(buffer, CartData{}))
	} /*
		{
			buffer := bytes.NewBuffer(make([]byte, 0))
			require.NoError(t, executeDebugResult(buffer, DebugResultData{}))
		}*/
	{
		buffer := bytes.NewBuffer(make([]byte, 0))
		require.NoError(t, executeStatus(buffer, StatusData{}))
	}
	{
		buffer := bytes.NewBuffer(make([]byte, 0))
		require.NoError(t, executeNotFound(buffer))
	}
	{
		buffer := bytes.NewBuffer(make([]byte, 0))
		require.NoError(t, executeErrorForm(buffer, fmt.Errorf("check")))
	}
}

func TestCartIDSplit(t *testing.T) {
	expRes := []string{
		"DCjA_w_xeU__XSHCwGDpPw",
		"dW40LKhCIvBv0pfcKtPAFQ",
		"kCC12gpVWb-VqMSpl3_10g",
	}
	input := strings.Join(expRes, "_")
	res := GetWareMd5(input)
	require.Len(t, res, 3)
	for i := range res {
		require.Equal(t, expRes[i], res[i])
	}
}

func TestSplitUInt64(t *testing.T) {
	expRes := []uint64{0, 750, 4294967295, 9223372036854775807, 18446744073709551615}
	input := "0,750 , 4294967295, 9223372036854775807 ,18446744073709551615"
	res := splitUInt64(input)
	require.Len(t, res, len(expRes))
	for i := range res {
		require.Equal(t, expRes[i], res[i])
	}
}

func TestRender(t *testing.T) {
	testRender := func(render Renderer, i int) {
		require.NotPanicsf(t, func() {
			render.formatResponce(&FieldsForRequest{courier: true, pickup: true})
		}, "#%v\n", i)
	}

	renderers := make([]Renderer, 0)
	renderers = append(renderers, &Debug{})
	renderers = append(renderers, &Debug{&cr.FindPathsResult{}})
	renderers = append(renderers, &CourierOptions{})
	renderers = append(renderers, &CourierOptions{&cr.DeliveryOptionsForUser{}, ""})
	renderers = append(renderers, &PickupPointsGrouped{})
	renderers = append(renderers, &PickupPointsGrouped{&cr.PickupPointsGrouped{}})
	renderers = append(renderers, &CartResult{})
	renderers = append(renderers, &CartResult{&cr.FindPathsResult{}})
	renderers = append(renderers, &ScenarioPickup{})
	renderers = append(renderers, &ScenarioPickup{&cr.PickupPointsGrouped{}, &cr.DeliveryRoute{}})
	renderers = append(renderers, &ScenarioCourier{})
	renderers = append(renderers, &ScenarioCourier{&cr.DeliveryOptionsForUser{}, &cr.DeliveryRoute{}})

	for i, renderer := range renderers {
		testRender(renderer, i)
	}

}
