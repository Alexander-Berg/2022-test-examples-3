import { NamedValue, ProcessingStatus } from 'src/java/definitions';

export const PROCESSING_STATUSES: NamedValue<ProcessingStatus>[] = [
  { value: ProcessingStatus.OPEN, title: 'Открыт' },
  { value: ProcessingStatus.NEED_INFO, title: 'Требуется информация' },
];
