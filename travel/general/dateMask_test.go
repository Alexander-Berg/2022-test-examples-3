package dtutil

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestBitsMask_AllOnes(t *testing.T) {
	assert.Equal(t, int64((uint64(1)<<BitsPerElem)-1), GetAllOneBits(BitsPerElem))
}

func TestBitsMask_AddRemovePos(t *testing.T) {
	bm := NewBitsMask(400)
	assert.True(t, bm.IsEmpty())

	bm.AddPos(1)
	assert.False(t, bm.IsEmpty())
	bm.AddPos(15)
	assert.False(t, bm.IsEmpty())
	bm.AddPos(395)
	assert.False(t, bm.IsEmpty())

	bm.RemovePos(15)
	assert.False(t, bm.IsEmpty())
	bm.RemovePos(395)
	assert.False(t, bm.IsEmpty())
	bm.RemovePos(1)
	assert.True(t, bm.IsEmpty())
}

func TestDateMask_AddRange(t *testing.T) {
	dm := NewDateMask(DateCache.IndexOfStringDateP("2020-04-01"), 400)

	assert.Equal(t, []IntDate{}, dm.GetDates())
	assert.Equal(t, IntDate(0), dm.GetFirstDate())

	dm.AddRange(StringDate("2020-04-10"), StringDate("2020-04-10"), 5)

	assert.Equal(t, []IntDate{20200410}, dm.GetDates())
	assert.Equal(t, IntDate(20200410), dm.GetFirstDate())
}

type Range struct {
	since  string
	until  string
	onDays int32
}

func TestDateMask_ShiftDays(t *testing.T) {
	dm := NewDateMask(DateCache.IndexOfStringDateP("2020-04-01"), 400)

	dm.AddStringDate("2020-04-01")
	dm.AddStringDate("2020-07-01") // more days than bits in one mask elem
	dm.ShiftDays(1)
	assert.Equal(t, []IntDate{20200402, 20200702}, dm.GetDates())

	dm = NewDateMask(DateCache.IndexOfStringDateP("2020-04-01"), 400)

	dm.AddStringDate("2020-04-01")
	dm.AddStringDate("2020-04-02")
	dm.AddStringDate("2020-07-02") // more days than bits in one mask elem
	dm.ShiftDays(-1)
	assert.Equal(t, []IntDate{20200401, 20200701}, dm.GetDates())
}

func TestDateMask_GetSingleRange(t *testing.T) {
	dm := NewDateMask(DateCache.IndexOfStringDateP("2020-04-01"), 400)
	dm.AddRange(StringDate("2020-04-10"), StringDate("2020-04-24"), 1235)
	verifyRange(t, dm, Range{"2020-04-10", "2020-04-24", 1235})

	dm = NewDateMask(DateCache.IndexOfStringDateP("2020-04-01"), 400)
	dm.AddRange(StringDate("2020-03-15"), StringDate("2020-04-24"), 1235)
	verifyRange(t, dm, Range{"2020-04-01", "2020-04-24", 1235})

	dm = NewDateMask(DateCache.IndexOfStringDateP("2020-04-01"), 400)
	dm.AddRange(StringDate("2020-03-15"), StringDate("2020-03-31"), 1235)
	verifyRange(t, dm, Range{"", "", 0})

	dm = NewDateMask(DateCache.IndexOfStringDateP("2020-04-01"), 400)
	dm.AddRange(StringDate("2020-03-15"), StringDate("2020-04-03"), 1235)
	verifyRange(t, dm, Range{"2020-04-01", "2020-04-03", 35})
}

func verifyRange(t *testing.T, dm DateMask, expected Range) {
	since, until, onDays := dm.GetSingleRange()
	assert.Equal(t, expected, Range{string(since), string(until), onDays})
}
