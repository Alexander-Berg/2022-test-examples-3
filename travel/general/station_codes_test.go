package dicts

import (
	"io"
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

type fakeRepositoryUpdater struct {
	messages []proto.Message
}

func (f *fakeRepositoryUpdater) Populate(writer io.Writer) error {
	for _, m := range f.messages {
		bytes, _ := proto.Marshal(m)
		_, _ = writer.Write(bytes)
	}
	return nil
}

func TestStationCodes(t *testing.T) {
	t.Run(
		"UpdateFromSource", func(t *testing.T) {
			repo := NewStationCodesRepository()
			testCode := &rasp.TStationCode{StationId: 1, Code: "CODE", SystemId: rasp.ECodeSystem_CODE_SYSTEM_IATA}
			updater := &fakeRepositoryUpdater{messages: []proto.Message{testCode}}

			err := repo.UpdateFromSource(updater)

			require.NoError(t, err)
			stationID, ok := repo.GetStationIDByCode("CODE")
			require.True(t, ok)
			require.EqualValues(t, 1, stationID)
			stationCode, ok := repo.baseRepository.Get(1)
			require.True(t, ok)
			require.Equal(t, testCode.Code, stationCode.Code)
		},
	)
}
