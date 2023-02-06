package main

import (
	"sort"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRemoveDuplicates(t *testing.T) {
	assert := assert.New(t)

	suggests := []SuggestMetaInfo{
		{pointKey: "c1"},
		{pointKey: "s123"},
		{pointKey: "s3", level: 0},
		{pointKey: "s3", level: 1},
	}
	fabric := SuggestFabric{Settlement2AllStations: map[string][]string{"c1": {"s11", "s123"}}}
	actual := fabric.RemoveDuplicates(suggests)
	assert.Equal([]SuggestMetaInfo{{pointKey: "c1"}, {pointKey: "s123"}, {pointKey: "s3", level: 1}}, actual)
}

func TestRemoveEqualToCities(t *testing.T) {
	assert := assert.New(t)

	suggests := []SuggestMetaInfo{
		{pointKey: "s123"},
		{pointKey: "c1"},
		{pointKey: "s11", level: 1},
		{pointKey: "s3", level: 0},
		{pointKey: "s3", level: 1},
	}
	fabric := SuggestFabric{Settlement2AllStations: map[string][]string{"c1": {"s11", "s123"}}}
	actual := fabric.RemoveAirportsEqualToCities(suggests, "ru")
	assert.Equal([]SuggestMetaInfo{{pointKey: "c1"}, {pointKey: "s11", level: 1}, {pointKey: "s3", level: 1}}, actual)

}

func TestIsSortedByDirectionPopularity(t *testing.T) {
	assert := assert.New(t)

	p1 := SuggestMetaInfo{pointKey: "c1", directionPopularity: 5}
	p2 := SuggestMetaInfo{pointKey: "s11", directionPopularity: 3}
	p3 := SuggestMetaInfo{pointKey: "s4", directionPopularity: 2}
	p4 := SuggestMetaInfo{pointKey: "s123", directionPopularity: 1}
	p5 := SuggestMetaInfo{pointKey: "s3", directionPopularity: 0}

	suggests := []SuggestMetaInfo{
		p2, p3, p5, p1, p4,
	}
	suggestsSort := []SuggestMetaInfo{
		p1, p2, p3, p4, p5,
	}
	sort.Sort(SuggestMetaInfoSort(suggests))
	assert.Equal(suggestsSort, suggests)
	sort.Sort(SuggestMetaInfoSortBlended(suggests))
	assert.Equal(suggestsSort, suggests)
}
