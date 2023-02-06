def test_minsk(upper):
    upper.set_query(dump='request')

    for query in ('минск', 'минску', 'МИНСК'):
        r = upper.get_pb(text=query)
        assert len(r.geo_objects) > 0
        assert r.experimental_items['QueryGeoPartAux'] == 'минск'

        doc = r.geo_objects[0]
        assert doc.is_toponym()
        assert doc._geo_metadata().address.country_code == 'BY'
        assert doc.name == 'Минск'
