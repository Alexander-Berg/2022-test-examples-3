package ytutil

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/reflect/protodesc"

	pbgraph "a.yandex-team.ru/market/combinator/proto/graph"
)

func TestFindTablesForStore(t *testing.T) {
	// время между 1 и 2 таблицами < storeDuration
	nodes := []string{"20220222_122200", "20220222_121900", "20220222_121200", "20220222_112200", "20220222_111300", "20220222_101200"}
	{
		expectedNodesToDel := []string{"20220222_122200", "20220222_112200", "20220222_111300", "20220222_101200"} // nodes[:1] + nodes[3:]
		expectedNodesToStore := nodes[1:3]
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 2, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}
	{
		expectedNodesToDel := nodes[:1]
		expectedNodesToStore := nodes[1:]
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 5, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}
	{
		expectedNodesToDel := nodes[:1]
		expectedNodesToStore := nodes[1:]
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 6, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}

	// время между 1 и 2 таблицами > storeDuration
	nodes = []string{"20220222_122300", "20220222_121200", "20220222_112200", "20220222_111300", "20220222_101200"}
	{
		expectedNodesToDel := nodes[2:]
		expectedNodesToStore := nodes[:2]
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 2, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}
	{
		expectedNodesToDel := []string{}
		expectedNodesToStore := nodes
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 5, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}
	{
		expectedNodesToDel := []string{}
		expectedNodesToStore := nodes
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 6, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}

	// время между 1 и 2 таблицами == storeDuration
	nodes = []string{"20220222_122200", "20220222_121200", "20220222_112200", "20220222_111300", "20220222_101200"}
	{
		expectedNodesToDel := []string{"20220222_122200", "20220222_111300", "20220222_101200"}
		expectedNodesToStore := nodes[1:3]
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 2, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}
	{
		expectedNodesToDel := nodes[:1]
		expectedNodesToStore := nodes[1:]
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 5, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}
	{
		expectedNodesToDel := nodes[:1]
		expectedNodesToStore := nodes[1:]
		nodesToDelete, nodesToStore := FindTablesForStore(nodes, 6, time.Minute*10)
		require.Equal(t, expectedNodesToDel, nodesToDelete)
		require.Equal(t, expectedNodesToStore, nodesToStore)
	}
}

func TestGenerateFileDescriptorSet(t *testing.T) {
	message := pbgraph.EnrichedSegment{}
	fds := GenerateFileDescriptorSet(message.ProtoReflect().Descriptor())

	files, err := protodesc.NewFiles(fds)
	require.NoError(t, err)

	_, err = files.FindDescriptorByName(message.ProtoReflect().Descriptor().FullName())
	require.NoError(t, err)
}
