from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search import building_info_pb2


validator = Validator('search')


def validate_building_info_metadata(data, example_name):
    metadata = building_info_pb2.BuildingInfoMetadata()

    metadata.built_year = data['built_year']
    metadata.floors = data['floors']
    metadata.apartments = data['apartments']
    metadata.emergency_condition = data['emergency_condition']
    metadata.cadastral_number = data['cadastral_number']
    metadata.capital_repair_year = data['capital_repair_year']
    metadata.managing_company = data['managing_company']

    for attribution in data['attribution']:
        metadata_attribution = metadata.attribution.add()
        attribution_author = attribution.get('author')
        if attribution_author:
            metadata_attribution.author.name = attribution_author.get('name')
            metadata_attribution.author.uri = attribution_author.get('uri')
        attribution_link = attribution.get('link')
        if attribution_link:
            metadata_attribution.link.href = attribution_link.get('href')


def test_building_info():
    building_info_ru = {
        'built_year': 1973,
        'floors': 2,
        'apartments': 12,
        'emergency_condition': False,
        'cadastral_number': '76:09:160101',
        'capital_repair_year': 2022,
        'managing_company': 'УК «РКЦ ЖКУ» с 01.08.2008',
        'attribution': [
            {
                'author': {
                    'name': 'Реформа ЖКХ',
                    'uri': 'https://www.reformagkh.ru/'
                }
            },
            {
                'author': {
                    'name': 'Дом.МинЖКХ.РУ',
                    'uri': 'http://dom.mingkh.ru/'
                },
                'link': {
                    'href': 'http://dom.mingkh.ru/yaroslavskaya-oblast/nekrasovskoe/334054'
                }
            }
        ]
    }

    validate_building_info_metadata(building_info_ru, 'building_info_ru')
