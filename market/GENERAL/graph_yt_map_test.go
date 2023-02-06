package graph

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/util"
	"a.yandex-team.ru/yt/go/mapreduce"
	"a.yandex-team.ru/yt/go/yson"
)

type fakeReader struct {
	src     []string
	currVal yson.RawValue
	currIdx int
}

func (f *fakeReader) TableIndex() int {
	panic("not implemented")
}

func (f *fakeReader) KeySwitch() bool {
	panic("not implemented")
}

func (f *fakeReader) RowIndex() int64 {
	panic("not implemented")
}

func (f *fakeReader) RangeIndex() int {
	panic("not implemented")
}

func (f *fakeReader) Scan(value interface{}) error {
	return yson.Unmarshal(f.currVal, value)
}

func (f *fakeReader) MustScan(value interface{}) {
	panic("not implemented")
}

func (f *fakeReader) Next() bool {
	if f.currIdx >= len(f.src) {
		return false
	}

	f.currVal = yson.RawValue(f.src[f.currIdx])
	f.currIdx++

	return true
}

type fakeWriter struct {
	dest []interface{}
}

func (f *fakeWriter) Write(value interface{}) error {
	if f.dest == nil {
		return fmt.Errorf("`dest` has not been initialized")
	}

	f.dest = append(f.dest, value)

	return nil
}

func (f *fakeWriter) MustWrite(value interface{}) {
	if err := f.Write(value); err != nil {
		panic(err)
	}
}

func TestMapSegmentJob_Do(t *testing.T) {
	expectedSegments := []LogisticSegmentYT{
		{ID: 1, Type: "type_seg_1_str", PartnerLmsID: 11, PartnerName: "parner_1_str"},
		{ID: 2, Type: "type_seg_2_str", PartnerLmsID: 22, PartnerName: "parner_2_str"},
	}

	template := "{\"lms_id\"=%d;type=\"%s\";\"partner_lms_id\"=%d;\"partner_name\"=\"%s\";}"

	r := fakeReader{
		src: []string{
			fmt.Sprintf(template, expectedSegments[0].ID, expectedSegments[0].Type, expectedSegments[0].PartnerLmsID, expectedSegments[0].PartnerName),
			fmt.Sprintf(template, expectedSegments[1].ID, expectedSegments[1].Type, expectedSegments[1].PartnerLmsID, expectedSegments[1].PartnerName),
		},
	}

	w := fakeWriter{dest: make([]interface{}, 0, len(expectedSegments))}

	job := MapSegmentJob{}

	err := job.Do(nil, &r, []mapreduce.Writer{&w})
	require.NoError(t, err)

	require.Equal(t, len(expectedSegments), len(w.dest))
	for i, expected := range expectedSegments {
		require.Equal(t, &expected, w.dest[i])
	}
}

func TestMapServiceJob_Do(t *testing.T) {
	expectedServices := []LogisticServiceYT{
		{ID: 1, Type: "type_seg_1_str", CargoTypes: []float64{1, 2}},
		{ID: 2, Type: "type_seg_2_str", CargoTypes: []float64{3, 4}},
	}

	template := "{\"lms_id\"=%d;type=\"%s\";\"cargo_types\"=[%.1f;%.1f];}"

	r := fakeReader{
		src: []string{
			fmt.Sprintf(template, expectedServices[0].ID, expectedServices[0].Type, expectedServices[0].CargoTypes[0], expectedServices[0].CargoTypes[1]),
			fmt.Sprintf(template, expectedServices[1].ID, expectedServices[1].Type, expectedServices[1].CargoTypes[0], expectedServices[1].CargoTypes[1]),
		},
	}

	w := fakeWriter{dest: make([]interface{}, 0, len(expectedServices))}

	job := MapServiceJob{}

	err := job.Do(nil, &r, []mapreduce.Writer{&w})
	require.NoError(t, err)

	require.Equal(t, len(expectedServices), len(w.dest))
	for i, expected := range expectedServices {
		require.Equal(t, &expected, w.dest[i])
	}
}

func TestMapServiceJob_Do_ExpectedError(t *testing.T) {
	r := fakeReader{
		src: []string{
			"{\"lms_id\"=1;type=\"type_1_str\";\"cargo_types\"=[1.0;2.0];}",
			"{\"lms_id\"=2;type=\"type_2_str\";\"cargo_types\"=[\"3.0\";\"4.0\"];}", // broken data type of cargo_types
		},
	}

	w := fakeWriter{dest: make([]interface{}, 0, 2)}

	job := MapServiceJob{}

	err := job.Do(nil, &r, []mapreduce.Writer{&w})
	require.Error(t, err)

	require.Len(t, w.dest, 1)
	require.Equal(t, int64(1), w.dest[0].(*LogisticServiceYT).ID)
}

func TestMapSegmentJob_Do_FilterPoints(t *testing.T) {
	okPoint := int64(1)
	notOkPoint := int64(2)
	expectedSegments := []LogisticSegmentYT{
		{ID: 1, Type: "pickup", PartnerLmsID: 11, PartnerName: "parner_1_str", PointLmsID: &okPoint},
		{ID: 2, Type: "pickup", PartnerLmsID: 22, PartnerName: "parner_2_str", PointLmsID: &notOkPoint},
	}

	template := "{\"lms_id\"=%d;type=\"%s\";\"partner_lms_id\"=%d;\"partner_name\"=\"%s\";\"logistics_point_lms_id\"=%d;}"

	r := fakeReader{
		src: []string{
			fmt.Sprintf(template, expectedSegments[0].ID, expectedSegments[0].Type, expectedSegments[0].PartnerLmsID, expectedSegments[0].PartnerName, *expectedSegments[0].PointLmsID),
			fmt.Sprintf(template, expectedSegments[1].ID, expectedSegments[1].Type, expectedSegments[1].PartnerLmsID, expectedSegments[1].PartnerName, *expectedSegments[1].PointLmsID),
		},
	}

	w := fakeWriter{dest: make([]interface{}, 0, len(expectedSegments))}

	job := MapSegmentJob{UniquePoints: map[int64]struct{}{okPoint: {}}}

	err := job.Do(nil, &r, []mapreduce.Writer{&w})
	require.NoError(t, err)

	t.Log(util.DebugInfo)

	require.Len(t, w.dest, 1)
	require.Equal(t, &expectedSegments[0], w.dest[0])
}
