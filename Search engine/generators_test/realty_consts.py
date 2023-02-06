#  -*- coding: utf-8 -*-
from collections import OrderedDict

sizes_in_order = ['STUDIO', '1', '2', '3', 'PLUS_4', 'OPEN_PLAN']

sizes_description = OrderedDict()
sizes_description.update({'STUDIO': u'Студия'})
sizes_description.update({'1': u'1-комн.'})
sizes_description.update({'2': u'2-комн.'})
sizes_description.update({'3': u'3-комн.'})
sizes_description.update({'PLUS_4': u'4-комн. и более'})
sizes_description.update({'OPEN_PLAN': u'Свободная планировка'})

construction_types = OrderedDict()
construction_types.update({'BRICK': u'кирпичный'})
construction_types.update({'MONOLIT_BRICK': u'кирпично-монолитный'})
construction_types.update({'MONOLIT': u'монолитный'})
construction_types.update({'PANEL': u'панельный'})
construction_types.update({'WOOD': u'деревянный'})
construction_types.update({'BLOCK': u'блочный'})
construction_types.update({'METAL': u'металлический'})
construction_types.update({'FERROCONCRETE': u'железобетонный'})

parking_types = OrderedDict()
parking_types.update({'SEPARATE': u'отдельная парковка'})
parking_types.update({'SECURE': u'охраняемая парковка'})
parking_types.update({'NEARBY': u'парковка рядом'})
parking_types.update({'UNDERGROUND': u'подземная парковка'})
parking_types.update({'MULTILEVEL': u'многоуровневая парковка'})
parking_types.update({'GROUND': u'наземная парковка'})
parking_types.update({'OPEN': u'открытая парковка'})
parking_types.update({'CLOSED': u'закрытая парковка'})

decoration_types = OrderedDict()
decoration_types.update({'ROUGH': u'черновая'})
decoration_types.update({'CLEAN': u'чистовая'})
decoration_types.update({'TURNKEY': u'под ключ'})

class_types = OrderedDict()
class_types.update({'ECONOM': u'Эконом'})
class_types.update({'BUSINESS': u'Бизнес'})
class_types.update({'ELITE': u'Элитный'})
class_types.update({'COMFORT': u'Комфорт'})
class_types.update({'COMFORT_PLUS': u'Комфорт+'})
