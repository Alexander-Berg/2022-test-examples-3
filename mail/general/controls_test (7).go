package pkpass

import (
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"a.yandex-team.ru/mail/iex/taksa/tutil"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"

const (
	epoch string = "1970-01-01T03:00:01+03:00"
)

type emptyError struct {
}

func (e emptyError) Error() string {
	return ""
}

func TestGetMovie_hasNoMovie_givesError(t *testing.T) {
	class := Class{Logger: logger.Mock{}}
	_, err := class.getMovie(Pass{})
	AssertThat(t, err, Not{V: nil})
}

func TestGetMovie_hasMovie_givesMovie(t *testing.T) {
	class := Class{Logger: logger.Mock{}}
	c, err := class.getMovie(Pass{IexDict{"title": "a"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "a"})
}

func TestPrintTime_nilTime_notOk(t *testing.T) {
	nilTime := time.Time{}
	_, ok := printTime(nilTime, tanker.Mock{})
	AssertThat(t, ok, Not{V: true})
}

func TestPrintTime_okTime_givesTimeString(t *testing.T) {
	nonnilTime, _ := tutil.GetTimeFromRfcString(epoch)
	timeStr, ok := printTime(nonnilTime, tanker.Mock{})
	AssertThat(t, ok, Is{V: true})
	AssertThat(t, timeStr, Is{V: "Jan 1 03:00"})
}

func TestPrintDetais(t *testing.T) {
	print := func(d Details) string {
		return d.printDetails(tanker.Mock{})
	}
	when, _ := tutil.GetTimeFromRfcString(epoch)
	AssertThat(t, print(Details{}), Is{V: ""})
	AssertThat(t, print(Details{Date: when}), Is{V: "Jan 1 03:00"})
	AssertThat(t, print(Details{Location: "Molodyozhnyi"}), Is{V: "Molodyozhnyi"})
	AssertThat(t, print(Details{Location: "Molodyozhnyi", Date: when}), Is{V: "Jan 1 03:00, Molodyozhnyi"})
}

func TestGetCount_noCount_givesError(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Tanker: tanker.Mock{}}
	_, err := class.getCount(Pass{})
	AssertThat(t, err, Not{V: nil})
}

func TestGetCount_emptyCount_givesError(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Tanker: tanker.Mock{}}
	_, err := class.getCount(Pass{IexDict{"num_seats": ""}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetCount_haveCount_givesCount(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Tanker: tanker.Mock{}}
	c, err := class.getCount(Pass{IexDict{"num_seats": "2"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "2 билета"})
}

func TestGetSeats_haveSeats_givesSeats(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Tanker: tanker.Mock{}}
	seats := []interface{}{
		map[string]interface{}{"row": float64(1), "seat": float64(2)},
		map[string]interface{}{"row": float64(1), "seat": float64(3)},
		map[string]interface{}{"row": float64(4), "seat": float64(5)}}
	c, err := class.getSeats(Pass{IexDict{"seats": seats}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "_Row_ 1, _Seat_ 2 _And_ 3; _Row_ 4, _Seat_ 5"})
}
