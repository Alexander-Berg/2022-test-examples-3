package ru.yandex.market.rg.asyncreport.assortment;

import java.io.IOException;

import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.core.asyncreport.AsyncReports;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.feed.supplier.db.SupplierSuggestDao;
import ru.yandex.market.core.feed.supplier.report.SupplierReportPriceService;
import ru.yandex.market.core.feed.supplier.suggest.SupplierSuggestService;
import ru.yandex.market.core.feed.supplier.suggest.SupplierSuggestServiceImpl;
import ru.yandex.market.core.upload.FileUploadService;
import ru.yandex.market.core.upload.db.FileUploadDao;

public class AssortmentBeanPostProcessor implements BeanPostProcessor {

    private final SupplierXlsHelper supplierXlsHelper = Mockito.spy(new SupplierXlsHelper(new ClassPathResource("supplier/feed/Stock_xls-sku.xls"),
            "." + XSSFWorkbookType.XLSM.getExtension()));

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SupplierSuggestService) {
            SupplierSuggestDao supplierSuggestDao = applicationContext.getBean(SupplierSuggestDao.class);
            FileUploadService fileUploadService = applicationContext.getBean(FileUploadService.class);
            FileUploadDao fileUploadDao = applicationContext.getBean(FileUploadDao.class);
            SupplierReportPriceService supplierReportPriceService = applicationContext.getBean(SupplierReportPriceService.class);
            DataCampService dataCampService = applicationContext.getBean(DataCampService.class);
            AsyncReports<ReportsType> asyncReports = (AsyncReports<ReportsType>) applicationContext.getBean("asyncReportsService");

            try {
                Mockito.doNothing()
                        .when(supplierXlsHelper).fillTemplate(Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(),
                        Mockito.anyBoolean());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return new SupplierSuggestServiceImpl(
                    supplierSuggestDao,
                    fileUploadDao,
                    fileUploadService,
                    supplierReportPriceService,
                    supplierXlsHelper,
                    asyncReports,
                    dataCampService
            );
        }

        if (beanName.equals("supplierXlsHelper")) {
            return supplierXlsHelper;
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
