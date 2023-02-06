package blocks

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/library/go/renderer"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
)

func TestCollector(t *testing.T) {
	logger := &nop.Logger{}
	ctx := context.Background()

	t.Run(
		"GetBlocks/no blocks returns empty map", func(t *testing.T) {
			s := NewBlocksCollector(logger)
			blocks := make([]BlockConfig, 0)
			orderInfo := &orders.OrderInfo{}
			expected := make([]renderer.Block, 0)

			result, err := s.GetBlocks(ctx, blocks, orderInfo, models.Notification{})

			assert.NoError(t, err)
			assert.Equal(t, expected, result)
		},
	)

	t.Run(
		"GetBlocks/unknown required block returns error", func(t *testing.T) {
			s := NewBlocksCollector(logger)
			blockConfigs := []BlockConfig{{Type: 0, Required: true, IncludedIn: []models.NotificationSubtype{models.NotificationAdhoc}}}
			orderInfo := &orders.OrderInfo{}

			result, err := s.GetBlocks(ctx, blockConfigs, orderInfo, models.Notification{Subtype: models.NotificationAdhoc})

			assert.Error(t, err)
			assert.IsType(t, err, errUnknownBlockType{})
			assert.Nil(t, result)
		},
	)

	t.Run(
		"GetBlocks/block mustn't be presented for notification subtype", func(t *testing.T) {
			s := NewBlocksCollector(logger)
			blockConfigs := []BlockConfig{{Type: GreetingBlock, Required: true, IncludedIn: []models.NotificationSubtype{}}}
			orderInfo := &orders.OrderInfo{}
			expected := make([]renderer.Block, 0)

			result, err := s.GetBlocks(ctx, blockConfigs, orderInfo, models.Notification{Subtype: models.NotificationAdhoc})

			assert.NoError(t, err)
			assert.Equal(t, expected, result)
		},
	)

	t.Run(
		"GetBlocks/block mustn't be presented for order type", func(t *testing.T) {
			s := NewBlocksCollector(logger)
			blockConfigs := []BlockConfig{
				{
					Type:                 GreetingBlock,
					Required:             true,
					IncludedIn:           []models.NotificationSubtype{models.NotificationAdhoc},
					ExcludedForOrderType: []models.OrderType{models.OrderHotel},
				},
			}
			orderInfo := &orders.OrderInfo{}
			expected := make([]renderer.Block, 0)

			result, err := s.GetBlocks(
				ctx,
				blockConfigs,
				orderInfo,
				models.Notification{Subtype: models.NotificationAdhoc, Order: &models.Order{Type: models.OrderHotel}},
			)

			assert.NoError(t, err)
			assert.Equal(t, expected, result)
		},
	)

	t.Run(
		"GetBlocks/unknown unnecessary block returns empty map", func(t *testing.T) {
			s := NewBlocksCollector(logger)
			blockConfigs := []BlockConfig{
				{
					Type:       GreetingBlock,
					Required:   false,
					IncludedIn: []models.NotificationSubtype{models.NotificationAdhoc},
				},
			}
			orderInfo := &orders.OrderInfo{}
			expected := make([]renderer.Block, 0)

			result, err := s.GetBlocks(ctx, blockConfigs, orderInfo, models.Notification{Subtype: models.NotificationAdhoc})

			assert.NoError(t, err)
			assert.Equal(t, expected, result)
		},
	)

	t.Run(
		"GetBlocks/fetching required block finishes with error returns error", func(t *testing.T) {
			blockType := GreetingBlock
			blockProvider := newMockBlockProvider(
				blockType,
				func(context.Context, *orders.OrderInfo, models.Notification) (renderer.Block, error) {
					return nil, fmt.Errorf("some error")
				},
			)
			s := NewBlocksCollector(logger, WithBlockProvider(blockProvider))
			blockConfigs := []BlockConfig{
				{
					Type:       blockType,
					Required:   true,
					IncludedIn: []models.NotificationSubtype{models.NotificationAdhoc},
				},
			}
			orderInfo := &orders.OrderInfo{}

			result, err := s.GetBlocks(ctx, blockConfigs, orderInfo, models.Notification{Subtype: models.NotificationAdhoc})

			assert.Error(t, err)
			assert.IsType(t, err, errGetRequiredBlock{})
			assert.Nil(t, result)
		},
	)

	t.Run(
		"GetBlocks/fetching unnecessary block finishes with error returns empty map", func(t *testing.T) {
			blockType := GreetingBlock
			blockProvider := newMockBlockProvider(
				blockType,
				func(context.Context, *orders.OrderInfo, models.Notification) (renderer.Block, error) {
					return nil, fmt.Errorf("some error")
				},
			)
			s := NewBlocksCollector(logger, WithBlockProvider(blockProvider))
			blockConfigs := []BlockConfig{
				{
					Type:       blockType,
					Required:   false,
					IncludedIn: []models.NotificationSubtype{models.NotificationAdhoc},
				},
			}
			orderInfo := &orders.OrderInfo{}
			expected := make([]renderer.Block, 0)

			result, err := s.GetBlocks(ctx, blockConfigs, orderInfo, models.Notification{Subtype: models.NotificationAdhoc})

			assert.NoError(t, err)
			assert.Equal(t, expected, result)
		},
	)

	t.Run(
		"GetBlocks/multiple blocks", func(t *testing.T) {
			blockType1 := FooterBlock
			blockType2 := HeaderBlock
			block1 := mockBlock{1}
			block2 := mockBlock{2}
			blockProvider1 := newMockBlockProvider(
				blockType1,
				func(context.Context, *orders.OrderInfo, models.Notification) (renderer.Block, error) {
					return block1, nil
				},
			)
			blockProvider2 := newMockBlockProvider(
				blockType2,
				func(context.Context, *orders.OrderInfo, models.Notification) (renderer.Block, error) {
					return block2, nil
				},
			)
			s := NewBlocksCollector(
				logger,
				WithBlockProvider(blockProvider1),
				WithBlockProvider(blockProvider2),
			)
			blockConfigs := []BlockConfig{
				{Type: blockType1, Required: true, IncludedIn: []models.NotificationSubtype{models.NotificationAdhoc}},
				{Type: blockType2, Required: true, IncludedIn: []models.NotificationSubtype{models.NotificationAdhoc}},
			}
			orderInfo := &orders.OrderInfo{}
			expected := []renderer.Block{block1, block2}

			result, err := s.GetBlocks(ctx, blockConfigs, orderInfo, models.Notification{Subtype: models.NotificationAdhoc})

			assert.NoError(t, err)
			assert.Equal(t, expected, result)
		},
	)

	t.Run(
		"GetBlocks/keeps blocks order from config", func(t *testing.T) {
			options := make([]CollectorOption, 0)
			blockConfigs := make([]BlockConfig, 0)
			expectedBlocks := make([]renderer.Block, 0)
			for i := 0; i < 10; i++ {
				blockConfigs = append(
					blockConfigs,
					BlockConfig{Type: BlockType(i), Required: true, IncludedIn: []models.NotificationSubtype{models.NotificationAdhoc}},
				)
				block := mockBlock{i}
				expectedBlocks = append(expectedBlocks, block)
				options = append(
					options,
					WithBlockProvider(
						newMockBlockProvider(
							BlockType(i),
							func(context.Context, *orders.OrderInfo, models.Notification) (renderer.Block, error) {
								return block, nil
							},
						),
					),
				)
			}
			s := NewBlocksCollector(logger, options...)
			orderInfo := &orders.OrderInfo{}

			result, err := s.GetBlocks(ctx, blockConfigs, orderInfo, models.Notification{Subtype: models.NotificationAdhoc})

			assert.NoError(t, err)
			assert.Equal(t, expectedBlocks, result)
		},
	)
}

type mockBlockProvider struct {
	getBlock  func(context.Context, *orders.OrderInfo, models.Notification) (renderer.Block, error)
	blockType BlockType
}

func newMockBlockProvider(
	blockType BlockType,
	getBlock func(context.Context, *orders.OrderInfo, models.Notification) (renderer.Block, error),
) *mockBlockProvider {
	return &mockBlockProvider{blockType: blockType, getBlock: getBlock}
}

func (m *mockBlockProvider) GetBlock(ctx context.Context, orderInfo *orders.OrderInfo, notification models.Notification) (
	renderer.Block,
	error,
) {
	return m.getBlock(ctx, orderInfo, notification)
}

func (m *mockBlockProvider) GetBlockType() BlockType {
	return m.blockType
}

type mockBlock struct {
	Field1 int
}
