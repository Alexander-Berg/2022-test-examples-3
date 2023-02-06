#!/usr/bin/env bash

HOST="indexer-old-01-sas.test.vertis.yandex.net"
PORT=36445

zlib='python -c "import sys,zlib; sys.stdout.write(zlib.decompress(sys.stdin.read()));"'

curl "$HOST:$PORT/get-data?data-type=geobase.xml&format-version=1" | eval "$zlib" > tours-data/data/geobase.xml
curl "$HOST:$PORT/get-data?data-type=regions.xml&format-version=1" | eval "$zlib" > tours-data/data/regions.xml
curl "$HOST:$PORT/get-data?data-type=pansions&format-version=1" | eval "$zlib" > tours-data/data/pansions
curl "$HOST:$PORT/get-data?data-type=directions&format-version=4" | eval "$zlib" > tours-data/data/directions

#mappings
curl "$HOST:$PORT/get-data?data-type=cities.tsv&format-version=2" | eval "$zlib" > tours-data/data/cities.tsv
curl "$HOST:$PORT/get-data?data-type=countries.tsv&format-version=2" | eval "$zlib" > tours-data/data/countries.tsv
curl "$HOST:$PORT/get-data?data-type=departures.tsv&format-version=2" | eval "$zlib" > tours-data/data/departures.tsv
curl "$HOST:$PORT/get-data?data-type=airports.tsv&format-version=1" | eval "$zlib" > tours-data/data/airports.tsv

curl "$HOST:$PORT/get-data?data-type=search_settings.json&format-version=1" | eval "$zlib" > tours-data/data/search_settings.json
curl "$HOST:$PORT/get-data?data-type=lt_active_countries&format-version=1" | eval "$zlib" > tours-data/data/lt_active_countries

#curl "$HOST:$PORT/get-data?data-type=hotels_ratings&format-version=1" | eval "$zlib" > tours-data/data/hotels_ratings
#curl "$HOST:$PORT/get-data?data-type=operators&format-version=1" | eval "$zlib" > tours-data/data/operators
#curl "$HOST:$PORT/get-data?data-type=hotel_providers&format-version=1" | eval "$zlib" > tours-data/data/hotel_providers

#wizard stuff
curl "$HOST:$PORT/get-data?data-type=dates.tsv&format-version=1" | eval "$zlib" > tours-data/data/wizard/dates.tsv
curl "$HOST:$PORT/get-data?data-type=geo.tsv&format-version=2" | eval "$zlib" > tours-data/data/wizard/geo.tsv
curl "$HOST:$PORT/get-data?data-type=hotels.tsv&format-version=1" | eval "$zlib" > tours-data/data/wizard/hotels.tsv
curl "$HOST:$PORT/get-data?data-type=hotel_name_parts.tsv&format-version=3" | eval "$zlib" > tours-data/data/wizard/hotel_name_parts.tsv
curl "$HOST:$PORT/get-data?data-type=ignore_words.tsv&format-version=1" | eval "$zlib" > tours-data/data/wizard/ignore_words.tsv
curl "$HOST:$PORT/get-data?data-type=operators.tsv&format-version=1" | eval "$zlib" > tours-data/data/wizard/operators.tsv
curl "$HOST:$PORT/get-data?data-type=stop_words.txt&format-version=1" | eval "$zlib" > tours-data/data/wizard/stop_words.txt
curl "$HOST:$PORT/get-data?data-type=wizard.markers&format-version=1" | eval "$zlib" > tours-data/data/wizard/wizard.markers

#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.0.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.0.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.1.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.1.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.2.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.2.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.3.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.3.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.4.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.4.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.5.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.5.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.6.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.6.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.7.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.7.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.8.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.8.index
#curl "$HOST:$PORT/get-data?data-type=sharded_hotels.9.index&format-version=1" | eval "$zlib" | gunzip > tours-data/data/sharded_hotels.9.index
