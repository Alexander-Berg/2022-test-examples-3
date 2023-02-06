package references

import (
	"fmt"
	"io"

	"github.com/golang/protobuf/proto"
)

type Populator struct {
	items []proto.Message
}

func NewPopulator(items ...proto.Message) *Populator {
	return &Populator{items: items}
}

func (p *Populator) Populate(writer io.Writer) error {
	for _, item := range p.items {
		message, err := proto.Marshal(item)
		if err != nil {
			return fmt.Errorf("got error while marshalling message: %w", err)
		}
		if _, err := writer.Write(message); err != nil {
			return fmt.Errorf("got error while writing message: %w", err)
		}
	}
	return nil
}
