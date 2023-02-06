package tarifficator

import (
	"os"
	"testing"

	"github.com/stretchr/testify/require"
)

func prepareTariffsFile(filepath string) error {
	programs := []string{"MARKET_DELIVERY"}
	rows := []TariffYT{
		TariffYT{
			Programs:       programs,
			DeliveryMethod: "PICKUP",
			Type:           RuleTypeGlobal,
		},
		TariffYT{
			Programs:       programs,
			DeliveryMethod: "PICKUP",
			Type:           RuleTypeForPoint,
			Points: []Pickuppoint{
				{11},
			},
		},
	}
	file, err := os.Create(filepath)
	if err != nil {
		return err
	}
	for _, row := range rows {
		bytes, err := json.Marshal(row)
		if err != nil {
			return err
		}
		bytes = append(bytes, '\n')
		_, err = file.Write(bytes)
		if err != nil {
			return err
		}
	}
	return nil
}

func TestReadFromFile(t *testing.T) {
	filepath := "tariffs.lala"
	err := prepareTariffsFile(filepath)
	require.NoError(t, err)
	defer os.Remove(filepath)

	tfs, err := ReadFromFile(filepath, nil, nil)
	require.NoError(t, err)
	require.NotNil(t, tfs)

	fs := tfs.Common
	require.NotNil(t, fs)
	require.Len(t, fs.tariffs, 1)

	val := fs.tariffs[FromToRegions{}]
	tariffs := val.Tariffs
	require.Len(t, tariffs, 1)
	require.Len(t, tariffs[0].Points, 1)
	require.Equal(t, 11, int(tariffs[0].Points[0].ID))
}
