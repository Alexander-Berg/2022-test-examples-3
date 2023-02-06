import { CheckoutPickupOption } from '../types/checkout-api';

type PickupData = [string, string, { lat: number, lon: number }];

const pickupData: PickupData[] = [
    ['Москва Ленинградское', '125212, Москва г, Ленинградское ш, д.52', { lat: 55.836610, lon: 37.484774 }],
    ['Москва Щёлковское', '105523, Москва г, Щёлковское ш, д.29', { lat: 55.810218, lon: 37.780482 }],
    ['Москва Дружбы', '119330, Москва г, Дружбы ул, д.2/19, подъезд 1', { lat: 55.713162, lon: 37.522028 }],
    ['Москва Изюмская', '117624, Москва г, Изюмская ул, д.37, корпус 3', { lat: 55.551100, lon: 37.571884 }],
    ['Москва 800-летия Москвы', '127247, Москва г, 800-летия Москвы ул, д.14', { lat: 55.878016, lon: 37.548941 }],
    ['Москва Анадырский', '129327, Москва г, Анадырский проезд, д.8, корпус 1', { lat: 55.863555, lon: 37.682835 }],
    ['Москва Ленинградское', '125212, Москва г, Ленинградское ш, д.58, строение 26, пав. 144', { lat: 55.842550, lon: 37.484244 }],
    ['Москва Жулебинский', '109145, Москва г, Жулебинский б-р, д.5', { lat: 55.698553, lon: 37.844900 }],
    ['Москва Куликовская', '117628, Москва г, Куликовская ул, д.9', { lat: 55.567402, lon: 37.561976 }],
    ['Москва Ангарская', '125412, Москва г, Ангарская ул, д.26, корпус 1', { lat: 55.876854, lon: 37.520779 }],
];

const getPickupOptions = (
    data: PickupData,
    { needCoords, index }: { needCoords: boolean, index: number } = { needCoords: true, index: 0 }
): CheckoutPickupOption => {
    const [label, address, coordinates] = data;
    const pickupOption: CheckoutPickupOption = {
        id: `pickup-${index}-${label}`,
        label,
        address
    };

    if (needCoords) {
        pickupOption.coordinates = coordinates;
    }

    return pickupOption;
};

export const generatePickupOptions = (count = 5) => {
    return new Array(count).fill(null).map<CheckoutPickupOption>((_, index) => {
        const randomDataIndex = Math.round(Math.random() * 9);

        return getPickupOptions(pickupData[randomDataIndex], { needCoords: false, index });
    });
};

export const uniqPickupOptions = pickupData.map((data, index) =>
    getPickupOptions(data, { needCoords: true, index }));

export const uniqPickupOptionsWithoutCoordinates = pickupData.map((data, index) =>
    getPickupOptions(data, { needCoords: false, index }));
