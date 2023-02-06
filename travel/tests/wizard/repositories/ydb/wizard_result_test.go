package ydb

import (
	"context"
	"io/ioutil"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/yatest"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/repositories/ydb"
)

const testDataDirectory = "travel/avia/wizard/tests/wizard/repositories/ydb/testdata"

func TestSearchResultScanner_Set_ShouldWorkWithSimpleProtobuf(t *testing.T) {
	protoBytes, _ := ioutil.ReadFile(yatest.SourcePath(testDataDirectory + "/search_result"))
	const qid = "200730-111025-411.ticket.plane.c54_c146_2020-08-06_2020-08-20_economy_1_0_0_ru.ru"
	scanner := ydb.SearchResultScanner{Context: context.Background()}

	scanner.Set(string(protoBytes))

	require.NoError(t, scanner.Error)
	require.NotNil(t, scanner.Value)
	require.Equal(t, qid, *scanner.Value.Qid)
}

func TestSearchResultScanner_Set_ShouldWorkWithCompressedProtobuf(t *testing.T) {
	protoBytes, _ := ioutil.ReadFile(yatest.SourcePath(testDataDirectory + "/search_result.zz"))
	const qid = "200730-111025-411.ticket.plane.c54_c146_2020-08-06_2020-08-20_economy_1_0_0_ru.ru"
	scanner := ydb.SearchResultScanner{Context: context.Background()}

	scanner.Set(string(protoBytes))

	require.NoError(t, scanner.Error)
	require.NotNil(t, scanner.Value)
	require.Equal(t, qid, *scanner.Value.Qid)
}
