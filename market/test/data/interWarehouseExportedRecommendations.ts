import { InterWarehouseExportedRecommendationDTO } from 'src/java/definitions-replenishment';

type InterWarehouseExportedRecommendationGenerator = (
  data?: Partial<InterWarehouseExportedRecommendationDTO>
) => InterWarehouseExportedRecommendationDTO;

export const createInterWarehouseExportedRecommendation: InterWarehouseExportedRecommendationGenerator = (
  data = {}
) => ({
  abc: 'C',
  adjustedPurchaseQuantity: 1,
  category: 'Test category',
  correctionReason: {
    id: 0,
    name: '',
  },
  department: 'Дом и сад',
  height: 49,
  length: 47,
  manuallyCreated: false,
  movementId: 1,
  msku: 550430,
  purchaseQuantity: 1,
  ssku: '481645.550430',
  supplierId: 481645,
  supplierType: 1,
  title: '220718 Конвектор CNS 100 S Stiebel Eltron',
  weight: 5100,
  width: 14,
  ...data,
});
