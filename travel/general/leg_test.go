package legdb

import (
	// "a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects/model"
	// "github.com/jackc/pgx"
	// "reflect"
	// "strings"
	"testing"
	// "time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	// "a.yandex-team.ru/library/go/core/xerrors"
	dir "a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
)

type RowsMock struct {
	mock.Mock
}

func (r *RowsMock) Next() bool {
	args := r.Called()
	return args.Get(0).(bool)
}

func (r *RowsMock) Scan(dest ...interface{}) error {
	r.Called(dest...)
	return nil
}

func TestLeg_findMatch(t *testing.T) {
	rows := new(RowsMock)

	// findMatch() should ignore the first leg number since that leg does not operate on Tuesday 2018-02-18;
	// the second leg does operate on Tuesdays (and Thursdays), hence it's the leg to use
	rows.On("Next").Return(true).Twice()
	rows.On("Next").Return(false)
	rows.On("Scan", mock.Anything, mock.Anything, mock.Anything).Return(nil).Run(
		func(args mock.Arguments) {
			*args.Get(0).(*int16) = 1
			*args.Get(1).(*dtutil.OperatingDays) = 5
			*args.Get(2).(*int) = 0
		},
	).Once()
	rows.On("Scan", mock.Anything, mock.Anything, mock.Anything).Return(nil).Run(
		func(args mock.Arguments) {
			*args.Get(0).(*int16) = 2
			*args.Get(1).(*dtutil.OperatingDays) = 24
			*args.Get(2).(*int) = 0
		},
	).Once()

	leg, _, err := findMatch("2018-02-18", rows, dir.ARRIVAL)
	assert.NoError(t, err, "failed on finding a leg")
	assert.Equal(t, int16(2), leg, "got the wrong leg number")
}
