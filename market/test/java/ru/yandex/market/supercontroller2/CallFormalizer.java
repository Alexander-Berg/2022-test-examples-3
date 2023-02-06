package ru.yandex.market.supercontroller2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.FormalizerService;

import static org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

public class CallFormalizer implements Runnable {
    private static final Logger LOG = LogManager.getLogger();

    private static final String[] contextPath = new String[]{"bean.xml"};

    private FormalizerService formalizerService;

    public static void main(String[] args) throws Exception {
        //initLogger();
        try {
            ApplicationContext ctx = new ClassPathXmlApplicationContext(contextPath);
            AutowireCapableBeanFactory factory = ctx.getAutowireCapableBeanFactory();
            Runnable app = (Runnable) factory.createBean(CallFormalizer.class, AUTOWIRE_BY_NAME, true);
            app.run();

        } catch (Exception ex) {
            LOG.fatal("Application crushed", ex);
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        System.out.println("start");
        LOG.debug("ping: " + formalizerService.ping());

        Formalizer.Offer[] os = new Formalizer.Offer[]{
                Formalizer.Offer.newBuilder()
                        .setCategoryId(1003092)
                        .setTitle("Ортопедический матрас КОНСУЛ Империал 160х195")
                        .setDescription("Жесткий матрас-сэндвич. Состоит из чередующихся слоев натурального латекса, конского волоса и кокосовой койры.")
                        .build(),
                Formalizer.Offer.newBuilder()
                        .setCategoryId(1003092)
                        .setTitle("Ортопедический матрас КОНСУЛ Империал 160х195")
                        .setDescription("Жесткий матрас-сэндвич. Состоит из чередующихся слоев натурального латекса, конского волоса и кокосовой койры.")
                        .build(),
                Formalizer.Offer.newBuilder()
                        .setCategoryId(91011)
                        .setTitle("RestR Хороший компьютер, процессор Intel® Core i7-940, оперативная память DDR3 4 Гб, " +
                                "Видеокарта GF GTX295 1792Mb, CR, жесткий диск (HDD) 750 Гб, BD-ROM, Thermaltake Soprano VX, " +
                                "операционная система Windows Vista Business (471198)")
                        .setDescription("|Описание: 3 года полной гарантии! Замена на другой компьютер в течении 2-х недель по любой причине! " +
                                "Все компьютеры RestR имеют сертификат соответствия ГОСТ Р и санитарно-эпидемиологическое заключение.")
                        .build()
        };
        try {
            for (Formalizer.Offer o : os)
                printRes(formalizerService.formalizeSingleOffer(o));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("done.");
    }


    private void printRes(Formalizer.FormalizedOffer params) {
        StringBuilder b = new StringBuilder();
        b.append("params[");
        for (int i = 0; i < params.getPositionCount(); i++) {
            FormalizerParam.FormalizedParamPosition p = params.getPosition(i);
            b.append(" Param[id=" + p.getParamId() + " valueId=" + p.getValueId() + " " +
                    "numberVal=" + p.getNumberValue() + " weight=" + p.getWeight() + " " +
                    "paramStart=" + p.getParamStart() + " paramEnd=" + p.getParamEnd() + " " +
                    "valueStart=" + p.getValueStart() + " valueEnd=" + p.getValueEnd() + " " +
                    "unitStart=" + p.getUnitStart() + " unitEnd=" + p.getUnitEnd());
        }
        b.append("]");
        LOG.debug("ExtractedParams[" +
                "hyperCategoryId=" + params.getCategoryId() + " " +
                "resultType=" + params.getType() + " " +
                "params=" + b.toString());
    }

    public void setFormalizerService(FormalizerService formalizerService) {
        this.formalizerService = formalizerService;
    }
}
