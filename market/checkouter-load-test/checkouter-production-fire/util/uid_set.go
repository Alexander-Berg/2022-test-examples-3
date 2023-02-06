package util

const (
	MinUID      = 2_190_550_858_753_437_195
	MaxUID      = 2_190_550_859_753_437_194
	MaxCoinsUID = 2_190_550_858_753_537_194

	SingleUID = 2_308_324_861_409_815_965
)

func GetCoinsUIDRange() UIDSet {
	return &UIDRange{From: MinUID, To: MaxCoinsUID}
}

func GetShootingUIDRange() UIDSet {
	return &UIDRange{From: MinUID, To: MaxUID}
}

type UIDArray []int

type UIDRange struct {
	From int
	To   int
}

type UIDSet interface {
	GetUID(rand Rand) int
}

type Rand interface {
	Intn(n int) int
}

func (arr UIDArray) GetUID(rand Rand) int {
	length := len(arr)
	if length == 0 {
		panic("Invalid UIDArray")
	}
	idx := rand.Intn(length)
	return arr[idx]
}

func (rng *UIDRange) GetUID(rand Rand) int {
	if rng.To < rng.From {
		panic("Invalid UIDRange")
	}
	return rng.From + rand.Intn(1+rng.To-rng.From)
}
