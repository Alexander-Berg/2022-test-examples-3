package testutils

var Counter = sequence{}

type sequence struct {
	value uint64
}

func (s *sequence) Int32() int32 {
	s.value++
	return int32(s.value)
}

func (s *sequence) Int() int {
	s.value++
	return int(s.value)
}
