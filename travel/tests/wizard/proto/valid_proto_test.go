package proto

import (
	"bytes"
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/helpers"
	"a.yandex-team.ru/travel/proto/dicts/avia"
)

func TestProcessAviaCompany(t *testing.T) {
	message := avia.TAviaCompany{
		RaspCompanyId: 1,
	}

	testMessage(t, &message, &avia.TAviaCompany{})
}

func TestProcessCityMajority(t *testing.T) {
	message := avia.TCityMajority{
		Id: 1,
	}

	testMessage(t, &message, &avia.TCityMajority{})
}

func TestProcessCodeSystem(t *testing.T) {
	message := avia.TCodeSystem{
		Id: 1,
	}

	testMessage(t, &message, &avia.TCodeSystem{})
}

func TestProcessCompany(t *testing.T) {
	message := avia.TCompany{
		Id: 1,
	}

	testMessage(t, &message, &avia.TCompany{})
}

func TestProcessCompanyTariff(t *testing.T) {
	message := avia.TCompanyTariff{
		Id: 1,
	}

	testMessage(t, &message, &avia.TCompanyTariff{})
}

func TestProcessCountry(t *testing.T) {
	message := avia.TCountry{
		Id: 1,
	}

	testMessage(t, &message, &avia.TCountry{})
}

func TestProcessCurrency(t *testing.T) {
	message := avia.TCurrency{
		Id: 1,
	}

	testMessage(t, &message, &avia.TCurrency{})
}

func TestProcessIataCorrection(t *testing.T) {
	message := avia.TIataCorrection{
		Id: 1,
	}

	testMessage(t, &message, &avia.TIataCorrection{})
}

func TestProcessRegion(t *testing.T) {
	message := avia.TRegion{
		Id: 1,
	}

	testMessage(t, &message, &avia.TRegion{})
}

func TestProcessSettlementBigImage(t *testing.T) {
	message := avia.TSettlementBigImage{
		Id: 1,
	}

	testMessage(t, &message, &avia.TSettlementBigImage{})
}

func TestProcessSettlement(t *testing.T) {
	message := avia.TSettlement{
		Id: 1,
	}

	testMessage(t, &message, &avia.TSettlement{})
}

func TestProcessStation(t *testing.T) {
	message := avia.TStation{
		Id: 1,
	}

	testMessage(t, &message, &avia.TStation{})
}

func TestProcessStationCode(t *testing.T) {
	message := avia.TStationCode{
		Id: 1,
	}

	testMessage(t, &message, &avia.TStationCode{})
}

func TestProcessStation2Settlement(t *testing.T) {
	message := avia.TStation2Settlement{
		Id: 1,
	}

	testMessage(t, &message, &avia.TStation2Settlement{})
}

func TestProcessTranslatedTitle(t *testing.T) {
	message := avia.TTranslatedTitle{
		Id: 1,
	}

	testMessage(t, &message, &avia.TTranslatedTitle{})
}

func testMessage(t *testing.T, in, out proto.Message) {
	buf := bytes.NewBufferString("")
	err := helpers.WritePtotobufIntoBuffer(buf, in)

	require.NoError(t, err)

	result, err := helpers.CutDataIntoProtobufRows(buf.Bytes())
	require.NoError(t, err)

	err = proto.Unmarshal(result[0], out)
	require.NoError(t, err)

	require.True(t, proto.Equal(in, out))
}
