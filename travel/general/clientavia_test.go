package travelapiclient

import (
	"context"
	"testing"

	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/require"
)

func TestAviaStartPayment(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/avia_booking_flow/v1/orders/payment",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, "{}")))

	orderID := ""
	err := client.AviaStartPayment(context.Background(), orderID)
	require.NoError(t, err)
}
