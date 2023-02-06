import subprocess
import netaddr.eui

nucadmin_vlan542 = "http://nucadmin01sof-542.wh.market.yandex.net/"
nucadmin_vlan572 = "https://nucadmin.mast.yandex-team.ru/"


def select_nucadmin_addr(*args):
    nucadmin_url = ''
    for url in args:
        for item in url.split('/'):
            if 'yandex' in item:
                nucadmin_url = item
                break
        print(f'Checking availability for {nucadmin_url}...')
        ping = subprocess.call("ping6 -c 3 " + nucadmin_url, shell=True, stdout = subprocess.PIPE, stderr = subprocess.PIPE)
        nucadmin_url = url
        if ping == 0:
            print(f'Ok, using {nucadmin_url}')
            break
        else:
            print(f'{nucadmin_url} installation is not avaiable from here. Trying the next one...')
            if args.index(url) == (len(args) - 1):
                nucadmin_url = "NONE"
                print(f'Alarm! No nucadmin available from here! Check your port VLAN, should be 542 or 572')
    return nucadmin_url

#nucadmin_url = select_nucadmin_addr(nucadmin_vlan542, nucadmin_vlan572)
    # return nucadmin_url


def eui(mac):
    subnet = '2a02:6b8:0:d20e:'
    eui = netaddr.eui.EUI(mac.lower())
    ll = eui.ipv6_link_local()

    return subnet + str(ll).split('::')[1]

if __name__ == "__main__":
    printers = '/Users/strkate/la'
    with open(printers) as f:
        content = f.readlines()
    for item in content:
        a = item.split(' ')
        addr = eui(a[1])
        name = a[0]
        print(f" dns-monkey --zone-update --expression \"add {name} {addr}\"")
