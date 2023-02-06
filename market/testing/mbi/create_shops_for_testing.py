#!/usr/bin/env python
# coding=utf-8

import mbi_common

def main():
    USER = mbi_common.User(id=515560223, email='market.mbi@yandex.ru')  # password: Alstom_2013

    mbi_partner = mbi_common.MbiPartner(user_id=USER.id)
    mbi_api = mbi_common.MbiApi()

    print "\n\nCREATING SHOP FOR RUSSIA\n\n"
    _, shop_id = mbi_common.register_shop(mbi_partner, user=USER, is_global=False, region=225, local_delivery_region=213) # Russia / Moscow
    print "\n\nCREATING SHOP FOR BELARUS\n\n"
    _, shop_id = mbi_common.register_shop(mbi_partner, user=USER, is_global=False, region=149, local_delivery_region=157) # Belarus / Minsk
    print "\n\nCREATING SHOP FOR UKRAINE\n\n"
    _, shop_id = mbi_common.register_shop(mbi_partner, user=USER, is_global=False, region=187, local_delivery_region=143) # Ukraine / Kiev
    print "\n\nCREATING SHOP FOR GLOBAL\n\n"
    _, shop_id = mbi_common.register_shop(mbi_partner, user=USER, is_global=True) # Global

if __name__ == '__main__':
    main()
