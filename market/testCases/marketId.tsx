import type {Ctx} from '..';

// маркет айди для "AUTOTEST DS светофор COMPLETED"
const MARKET_ID = 2012972;

const ORGINFO_BY_MARKET_ID = {
    ogrn: '1045900831745',
    name: 'AUTOTEST DS светофор COMPLETED',
    jurAddress: '127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3',
    actualAddress: '127081, г. Москва, улица Чермянская, дом 3, строение 2, помещение 3 ',
    kpp: '771501001',
};

const EMPTY_ORGINFO = {
    inn: '',
    jurAddress: '',
    actualAddress: '',
};

export default async (ctx: Ctx) => {
    await ctx.step('Выбираем организацию из списка и ожидаем, что данные загрузятся в форму', async () => {
        await ctx.app.editableForm.clickModifyApplication();

        const initialSelected = await ctx.app.editableForm.marketID.getSelected();

        /*
        Иногда форма может сохраниться в кривом состоянии, когда данные в полях не от выбранного marketId
        Разруливаем этот кейс, переподгружая данные по нужному marketId
        */
        if (String(initialSelected) === String(MARKET_ID)) {
            await ctx.app.editableForm.marketID.useNewData();
        }

        await ctx.app.editableForm.marketID.setByID(MARKET_ID);
        await ctx.app.editableForm.preloader.waitForLoading();

        const orgInfoFormValues = await ctx.app.editableForm.orgInfoForm.getMarketIdValues();

        await ctx.expect(orgInfoFormValues).deep.equal(ORGINFO_BY_MARKET_ID, 'Данные по маркет айди загружены верно');
    });

    await ctx.step('Выбираем "Использовать на Беру новые данные"', async () => {
        await ctx.app.editableForm.marketID.useNewData();

        const {actualPostalCode, ...orgInfoFormValues} = await ctx.app.editableForm.orgInfoForm.getValues();

        await ctx.expect(orgInfoFormValues).deep.equal(EMPTY_ORGINFO, 'Данные организации сброшены');
    });
};
