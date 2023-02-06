import { Modal, Button } from 'lego-on-react';
import block from 'propmods';
// eslint-disable-next-line @typescript-eslint/no-use-before-define
import React from 'react';

import { GeoPoint } from './TestChat';

import './TestGeo.scss';

const b = block('TestGeo');

const DEFAULT_POINT = [55.75, 37.62];

const reducePrecision = (v: number) => Number(`${Math.round(v * 10000)}e-4`);

export type TestGeoProps = {
    onCancel(): void;
    onSubmit(_point: GeoPoint): void;
};
export const TestGeo = ({ onSubmit, onCancel }: TestGeoProps) => {
    const [point, setPoint] = React.useState(DEFAULT_POINT);
    React.useLayoutEffect(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        let _map: any;
        // setTimeout чтобы модал успел отрисовать детей
        window.setTimeout(() => {
            const map = new ymaps.Map('test-geo-map', {
                center: DEFAULT_POINT,
                zoom: 10,
                controls: ['zoomControl', 'geolocationControl'],
            });
            const placemark = new ymaps.Placemark(DEFAULT_POINT, {}, {
                preset: 'islands#redIcon',
            });
            map.geoObjects.add(placemark);

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            function updatePoint(e: any) {
                const coords = e.get('coords');
                setPoint(coords);
                placemark.geometry?.setCoordinates(coords);
            }

            map.events.add(['click'], updatePoint);
            const searchControl = new ymaps.control.SearchControl({
                options: {
                    provider: 'yandex#search',
                    noPopup: true,
                    noPlacemark: true,
                },
            });
            map.controls.add(searchControl);
        });
        return () => {
            _map?.destroy();
        };
    }, []);

    const formattedPoint = point.map(reducePrecision).join(' ');
    const onSubmitCb = React.useCallback(() => {
        onSubmit({ lat: reducePrecision(point[0]), lon: reducePrecision(point[1]) });
    }, [onSubmit, point]);
    return (
        <Modal theme="normal" visible>
            <div {...b()}>
                <div {...b('map')} id="test-geo-map" />
                <div {...b('chosen-point')}>
                    Выбрана точка: <strong>{formattedPoint}</strong>
                </div>
                <div {...b('buttons')}>
                    <Button theme="action" size="s" onClick={onSubmitCb}>OK</Button>
                    <Button theme="normal" size="s" onClick={onCancel}>Отмена</Button>
                </div>
            </div>
        </Modal>
    );
};
