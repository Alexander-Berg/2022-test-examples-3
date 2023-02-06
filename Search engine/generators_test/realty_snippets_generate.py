#!/usr/bin/env python
#  -*- coding: utf-8 -*-

import os
import sys
import json
import argparse
import yt.wrapper as yt
from xml.sax.saxutils import escape
from xml.sax.saxutils import quoteattr

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc
from realty_consts import *


@yt.with_context
def gen_snippet_text(key, rows, context):
    row = None
    rows = list(rows)
    for r in rows:
        if not row:
            row = r
        else:
            row.update(r)
    row['@table_index'] = 0

    id = row['realty_id']
    redirect = gen_redirect(row)
    phases = gen_phases(row)
    offers = gen_offers(row)
    phone_button = gen_phonebutton(row)

    if redirect != '' or phases != '' or offers != '':
        snippet_text = (u'<Realty xmlns="http://maps.yandex.ru/snippets/realty/1.x" xmlns:atom="http://www.w3.org/2005/Atom">'
                            u'{developers}'
                            u'{phone}'
                            u'{callback}'
                            u'{construction_type}'
                            u'{construction_types}'
                            u'{realty_url}'
                            u'{redirect}'
                            u'{phone_button}'
                            u'{phases}'
                            u'{offers}'
                            u'{links}'
                            u'<description>'
                                u'{description}'
                            u'</description>'
                            u'{mortgage}'
                            u'<classType>{class_type}</classType>'
                            u'{parkings}'
                            u'<SimilarPlaces>'
                                u'{similars}'
                            u'</SimilarPlaces>'
                            u'<paid>{paid}</paid>'
                            u'{photos_data}'
                            u'{special_proposal}'
                        u'</Realty>').format(developers=gen_developers(row.get('developers', [])),
                                            phone=gen_nullable(row, 'redirect_phone', u'<phone>{:}</phone>'),
                                            callback=gen_nullable(row, 'phone_show_callback', u'<Callback><atom:link href="{:}"/></Callback>'),
                                            # todo для обратной совместимости, нужно будет выпилить
                                            construction_type=gen_single_construction_type(row),
                                            construction_types=gen_construction_types(row),
                                            realty_url=gen_nullable(row, 'realty_url', u'<RealtyUrl><atom:link href="{:}"/></RealtyUrl>'),
                                            redirect=redirect,
                                            phone_button=phone_button,
                                            phases=phases,
                                            offers=offers,
                                            links=gen_links(row['links']),
                                            #description=quoteattr(misc.xml_escape(row['data']['description'])),
                                            description=misc.xml_escape(row['data']['description']),
                                            mortgage=gen_mortgage(row['data']),
                                            class_type=class_types[row['data']['buildingFeatures']['class']],
                                            parkings=gen_parkings(row['data']['buildingFeatures'].get('parkings', [])),
                                            similars=gen_similars(row.get('similars', [])),
                                            paid=row['paid'],
                                            photos_data=gen_photos_data(row),
                                            special_proposal=gen_special_proposal(row))
        yield {'key': 'realty_yandex~{str_id}'.format(str_id=str(id)),
               'value': snippet_text,
               'permalink': row.get('permalink', None)}


def gen_special_proposal(row):
    main_special_proposal = None
    for p in row['data'].get('siteSpecialProposals', {}).get('allSpecialProposals', []):
        if p['mainProposal']:
            main_special_proposal = p
            break
    if not main_special_proposal:
        return u''
    else:
        return (u'<SpecialProposal>'
                    u'{full_description}'
                    u'{short_description}'
                    u'{special_proposal_type}'
                    u'{initial_payment_percents}'
                    u'{duration_months}'
                    u'{free_installment}'
                u'</SpecialProposal>').format(full_description=gen_nullable(main_special_proposal, 'fullDescription',
                                                                           u'<fullDescription>{:}</fullDescription>'),
                                             short_description=gen_nullable(main_special_proposal, 'shortDescription',
                                                                            u'<shortDescription>{:}</shortDescription>'),
                                             special_proposal_type=gen_nullable(main_special_proposal, 'specialProposalType',
                                                                                u'<specialProposalType>{:}</specialProposalType>'),
                                             initial_payment_percents=gen_nullable(main_special_proposal, 'initialPaymentPercents',
                                                                                   u'<initialPaymentPercents>{:}</initialPaymentPercents>'),
                                             duration_months=gen_nullable(main_special_proposal, 'durationMonths',
                                                                          u'<durationMonths>{:}</durationMonths>'),
                                             free_installment=gen_nullable(main_special_proposal, 'freeInstallment',
                                                                           u'<freeInstallment>{:}</freeInstallment>'))


def gen_photos_data(row):
    photos = row.get('photos', [])
    if len(photos) == 0:
        return u''
    else:
        return (u'<PhotosData>'
                    u'<total>{total}</total>'
                    u'<Photos>'
                        u'{photos}'
                    u'</Photos>'
                u'</PhotosData>').format(total=len(photos),
                                        photos=''.join('<Photo><atom:link href="{:}"/></Photo>'.format(photo) for photo in photos))


def gen_construction_types(row):

    def gen_construction_type(wall_type):
        return u'<constructionType>{:}</constructionType>'.format(misc.xml_escape(construction_types[wall_type['type']]))

    if row['data'].get('buildingFeatures') is not None and len(row['data']['buildingFeatures'].get('wallTypes', [])) > 0:
        constr_types = [gen_construction_type(wall_type) for wall_type in row['data']['buildingFeatures']['wallTypes']]
        return u'<ConstructionTypes>{:}</ConstructionTypes>'.format(''.join(constr_types))
    return u''


def gen_single_construction_type(row):
    # todo оставлено для обратной совместимости, нужно будет выпилить
    if row.get('construction_type') is not None and construction_types.get(row['construction_type']) is not None:
        return u'<constructionType>{:}</constructionType>'.format(misc.xml_escape(construction_types[row['construction_type']]))
    return u''


def gen_nullable(row, field, tag):
    if row.get(field) is not None:
        return tag.format(misc.xml_escape(row[field]))
    return u''


def gen_pattern(name):
    return (u'<' + name + '>'
               u'<name>'
                 u'{name}'
               u'</name>'
               u'<phone>'
                 u'{phone}'
               u'</phone>'
               u'<Callback>'
                 u'<atom:link href="{callback}"/>'
               u'</Callback>'
               u'<Logo>'
                 u'<atom:link href="{logo}"/>'
               u'</Logo>'
           u'</' + name + '>')


def gen_redirect(row):
    if not row.get('auction_winner_name') or not row.get('redirect_phone') or row.get('with_tycoon'):
        return u''
    else:
        return (gen_pattern(u'SalesDepartment')).format(name=misc.xml_escape(row['auction_winner_name']),
                                             phone=misc.xml_escape(row['redirect_phone']),
                                             callback=misc.xml_escape(row['phone_show_callback']),
                                             logo=misc.xml_escape(row['logo']))

def gen_phonebutton(row):
    if not row.get('auction_winner_name') or not row.get('redirect_phone'):
        return u''
    else:
        return (gen_pattern(u'PhoneButton')).format(name=misc.xml_escape(row['auction_winner_name']),
                                             phone=misc.xml_escape(row['redirect_phone']),
                                             callback=misc.xml_escape(row['phone_show_callback']),
                                             logo=misc.xml_escape(row['logo']))

def gen_phases(row):
    if row.get('phases') is not None:
        phases = [gen_phase(phase) for phase in row['phases']]
        return u'<phases>{:}</phases>'.format(u''.join(phases))
    return u''


def gen_phase(phase):
    return (u'<phase>'
                u'<finished>'
                    u'{finish}'
                u'</finished>'
                u'<name>'
                    u'{name}'
                u'</name>'
                u'<quarter>'
                    u'{quarter}'
                u'</quarter>'
                u'<year>'
                    u'{year}v'
                u'</year>'
            u'</phase>').format(finish=misc.xml_escape(phase.get('finished', 'true')),
                               name=misc.xml_escape(phase.get('name')),
                               quarter=misc.xml_escape(phase.get('quarter', '')), # todo fix
                               year=misc.xml_escape(phase.get('year', ''))) # todo fix


def gen_offers(row):

    def gen_offer(offer, size):

        def gen_price(price):
            return (u'<Price>'
                        u'<value>'
                            u'{value}'
                        u'</value>'
                        u'<text>'
                            u'{formatted} &#8381;'
                        u'</text>'
                        u'<currency>RUB</currency>'
                    u'</Price>').format(value=misc.xml_escape(price),
                                       formatted=u'{:,.0f}'.format(misc.xml_escape(price)))

        def gen_plan(plan):
            if plan is not None:
                return u'<Plan><atom:link href="{:}"/></Plan>'.format(misc.xml_escape(str(plan)))
            return u''

        return (u'<Offer>'
                    u'<type>'
                        u'{size}'
                    u'</type>'
                    u'<area-from>'
                        u'{area}'
                    u'</area-from>'
                    u'<formatted>'
                        u'{size_description} от {formatted_size}м²'
                    u'</formatted>'
                    u'{price}'
                    u'{plan}'
                    u'<Url>'
                        u'<Desktop>'
                            u'<atom:link href="{url}"/>'
                        u'</Desktop>'
                        u'<Mobile>'
                            u'<atom:link href="{m_url}"/>'
                        u'</Mobile>'
                    u'</Url>'
                u'</Offer>').format(size=size,
                                    area=offer.get('area-from', 0), #todo fix
                                    size_description=sizes_description[size],
                                    formatted_size=u'{:.1f}'.format(offer.get('area-from', 0)), #todo fix
                                    price=gen_price(offer.get('price-from', 0)), # todo fix
                                    plan=gen_plan(offer.get('plan')),
                                    url=misc.xml_escape(offer['url']),
                                    m_url=misc.xml_escape(offer['m-url']))

    if row.get('prices') is not None:
        offers = [gen_offer(row['prices'][size], size) for size in sizes_in_order if row['prices'].get(size) is not None]
        return u'<Offers>{:}</Offers>'.format(u''.join(offers))
    return u''


def gen_developers(developers):

    def gen_developer(developer):
        return (u'<Developer>'
                    u'{name}'
                    u'{legal_name}'
                    u'{url}'
                u'</Developer>').format(name=gen_nullable(developer, 'name', u'<name>{:}</name>'),
                                       legal_name=gen_nullable(developer, 'legal-name', u'<legalName>{:}</legalName>'),
                                       url=gen_nullable(developer, 'url', u'<Url><atom:link href="{:}"/></Url>'))

    if developers:
        return u'<Developers>{:}</Developers>'.format(gen_developer(developers[0]))
    else:
        return u'<Developers></Developers>'


def gen_links(links):
    available_link_types = {'images': 'Images',
                            'map': 'Map',
                            'map-route': 'MapRoute'}

    def gen_link(type, data):
        return (u'<{type}>'
                    u'<Desktop>'
                        u'<atom:link href="{desktop}"/>'
                    u'</Desktop>'
                    u'<Mobile>'
                        u'<atom:link href="{mobile}"/>'
                    u'</Mobile>'
                u'</{type}>').format(type=misc.xml_escape(type),
                                    desktop=misc.xml_escape(data['desktop']),
                                    mobile=misc.xml_escape(data['mobile']))

    links = [gen_link(available_link_types[links_type], links[links_type])
             for links_type in links
             if available_link_types.get(links_type) is not None]
    return u'<Links>{:}</Links>'.format(''.join(links))


def gen_mortgage(data):
    if data.get('siteSpecialProposals', {}).get('allMortgages') is None:
        return u''
    min_mortgage = None
    min_rate = None
    all_mortgages = []
    for mortgage in data['siteSpecialProposals']['allMortgages']:
        res = (u'<bankName>{bank_name}</bankName>'
               u'<minRate>{min_rate}</minRate>'
               u'<maxDuration>{max_duration}</maxDuration>').format(bank_name=misc.xml_escape(mortgage['bankName']),
                                                                   min_rate=mortgage.get('minRate', ''),
                                                                   max_duration=mortgage.get('maxDuration', ''))
        if mortgage.get('minRate') is not None and (min_rate is None or mortgage.get('minRate') < min_rate):
            min_rate = mortgage['minRate']
            min_mortgage = res
        all_mortgages.append(u'<Mortgage>{:}</Mortgage>'.format(res))

    return (u'<Mortgages>'
                u'<All>'
                    u'{all}'
                u'</All>'
                u'<Minimal>'
                    u'{minimal}'
                u'</Minimal>'
            u'</Mortgages>').format(all=''.join(all_mortgages),
                                   minimal=min_mortgage)


def gen_parkings(parkings):

    def gen_parking(parking):
        return (u'<Parking>'
                    u'{spaces}'
                    u'<type>{type}</type>'
                u'</Parking>').format(spaces=gen_nullable(parking, 'parkingSpaces', '<spaces>{:}</spaces>'),
                                     type=parking_types[parking['type']])

    if len(parkings) == 0:
        return u''
    return u'<Parkings>{:}</Parkings>'.format(''.join([gen_parking(parking) for parking in parkings]))


def gen_similars(similars):

    def gen_similar(similar):
        return (u'<SimilarPlace>'
                    u'<realtyId>{realty_id}</realtyId>'
                    u'<name>{name}</name>'
                    u'<priceFrom>{price_from}</priceFrom>'
                    u'<address>{address}</address>'
                    u'<Photos>'
                        u'{photos}'
                    u'</Photos>'
                u'</SimilarPlace>').format(realty_id=similar['realty_id'],
                                          name=quoteattr(misc.xml_escape(similar['name'])),
                                          price_from=similar['price_from'],
                                          address=quoteattr(misc.xml_escape(similar['address'])),
                                          photos=''.join('<Photo><atom:link href="{:}"/></Photo>'.format(photo) for photo in similar['photos']))

    return u''.join([gen_similar(similar) for similar in similars])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prepare realty snippets data')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL', ''),
                                args.cluster)
    params = json.loads(args.parameters)

    tmp_input_dir = '//tmp/tm_realty'
    tmp_input_table = os.path.join(tmp_input_dir, 'yandex-realty-finish')
    try:
        yt_client.create('map_node', tmp_input_dir)
    except Exception:
        pass
    tmp_prepared_table = '%s_temp' % params['temp_tables'].get('prepared_table')
    yt_client.copy(params.get('input_table'), tmp_input_table, force=True)
    yt_client.copy(params['temp_tables'].get('prepared_table'), tmp_prepared_table, force=True)
    yt_client.run_sort(tmp_input_table, sort_by=['realty_id'])
    yt_client.run_sort(tmp_prepared_table, sort_by=['realty_id'])

    yt_client.run_reduce(gen_snippet_text,
                         [tmp_input_table, tmp_prepared_table],
                         params.get('pre_processing_out'),
                         format=yt.JsonFormat(control_attributes_mode='row_fields',
                                              attributes={'encode_utf8': False}),
                         reduce_by=['realty_id'])
