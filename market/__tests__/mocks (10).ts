import {BadgesType, Link} from 'main-layout/components/Sidebar/Sidebar'

import {
  RestIcon,
  OrdersIcon,
  MenuIcon,
  HistoryIcon,
  StatisticsIcon,
  PlaceIcon,
  TrendingIcon,
  ChatIcon,
  HomeIcon
} from 'shared-assets/icons/sidebar'

const visible = true
export const headerLinks: Link[] = [
  {
    path: '/places',
    name: 'Рестораны',
    visible: true,
    iconPath: RestIcon
  },
  {
    path: '/active',
    name: 'Заказы',
    badge: {type: BadgesType.NOTIFY_RED},
    visible,
    iconPath: OrdersIcon
  },
  {
    path: '/menu',
    name: 'Меню',
    badge: {type: BadgesType.NOTIFY_RED},
    visible,
    iconPath: MenuIcon
  },
  {
    path: '/metrics',
    name: 'Статистика',
    visible,
    iconPath: StatisticsIcon
  },
  {
    path: '/history',
    name: 'История',
    visible,
    iconPath: HistoryIcon
  },
  {
    path: '/shipping-zone',
    name: 'Зоны доставки',
    visible,
    iconPath: PlaceIcon
  },
  {
    path: '/promo1', // fake path
    name: 'Продвижение',
    visible,
    iconPath: TrendingIcon
  },
  {
    path: '/chats', // fake path
    name: 'Поддержка',
    badge: {type: BadgesType.NOTIFY_COUNT, children: 2},
    visible,
    iconPath: ChatIcon
  }
]

export const headerLinksWithMainPage: Link[] = [
  {path: '/main', name: 'Главная', visible: true, iconPath: HomeIcon},
  ...headerLinks
]
export const footerLinks = [
  {
    path: '/support', // fake path
    name: 'База знаний',
    visible
  },
  {
    path: '/logout',
    name: 'Выход',
    visible
  }
]
