package helpers

import (
	"fmt"
	"testing"

	tpb "a.yandex-team.ru/travel/proto"
	"github.com/stretchr/testify/assert"
)

func TestSum(t *testing.T) {
	type TestCase struct {
		Arg1     *tpb.TPrice
		Arg2     *tpb.TPrice
		Expected *tpb.TPrice
	}
	for index, testCase := range []TestCase{
		TestCase{
			Arg1:     &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 2, Amount: 10100},
			Arg2:     &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 2, Amount: 234},
			Expected: &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 2, Amount: 10334},
		},
		TestCase{
			Arg1:     &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 1, Amount: 1010},
			Arg2:     &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 2, Amount: 234},
			Expected: &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 2, Amount: 10334},
		},
		TestCase{
			Arg1:     &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 2, Amount: 234},
			Arg2:     &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 0, Amount: 101},
			Expected: &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Precision: 2, Amount: 10334},
		},
	} {
		t.Run(fmt.Sprintf("testCase.%v", index), func(t *testing.T) {
			res, err := Sum(testCase.Arg1, testCase.Arg2)
			assert.NoError(t, err)
			assert.Equal(t, testCase.Expected.String(), res.String())
		})
	}
}
