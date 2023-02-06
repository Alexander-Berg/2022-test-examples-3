package metaresponse

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestContext(t *testing.T) {
	var mresp MetaResponse

	ctx := MakeContext(context.Background(), &mresp)
	mresp2 := GetValue(ctx)
	require.NotNil(t, mresp2)
	mresp2.UID = "42"

	require.Equal(t, mresp, *mresp2)
}

func TestContextFail(t *testing.T) {
	mresp := GetValue(context.Background())
	require.Nil(t, mresp)
}

func TestMakeFields(t *testing.T) {
	mresp := MetaResponse{
		UID: "42",
	}
	require.Len(t, mresp.MakeFields(), 2)
}
