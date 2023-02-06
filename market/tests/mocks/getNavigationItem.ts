import {CatalogNavigationNode, CatalogTarget} from '~/app/entities/catalogNavigationNode/types';

export default (id: number, name: string, navnodes?: CatalogNavigationNode[]): CatalogNavigationNode => ({
    category: {
        entity: 'category',
        id: 198119,
        isLeaf: false,
        modelsCount: 15524,
        name,
        nid: id,
        offersCount: 170868,
        type: 'gurulight',
        viewType: 'list',
    },
    childrenType: 'mixed',
    entity: 'navnode',
    fullName: name,
    hasPromo: false,
    icons: [
        {
            entity: 'picture',
            url: 'path/to/icon',
        },
    ],
    id,
    isLeaf: false,
    link: {params: {hid: ['198119'], nid: [String(id)]}, target: CatalogTarget.department},
    name,
    navnodes,
    pictures: [
        {
            entity: 'picture',
            height: 400,
            thumbnails: [
                {
                    densities: [
                        {
                            entity: 'density',
                            id: '1',
                            url: 'path/to/icon',
                        },
                    ],
                    entity: 'thumbnail',
                    height: 50,
                    id: '50x50',
                    width: 50,
                },
            ],
            url: 'path/to/icon',
            width: 400,
        },
    ],
    rootNavnode: {entity: 'navnode', id},
    slug: 'elektronika',
    type: 'category',
});
