import dayjs from 'dayjs';
import 'dayjs/locale/ru';
dayjs.locale('ru');

export const getEstimatedDate = dayCount => {
    const calculatedDate = dayjs().add(dayCount, 'day');
    return `с ${calculatedDate.format('D MMMM')}`;
};
