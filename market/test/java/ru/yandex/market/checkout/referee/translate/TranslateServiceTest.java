package ru.yandex.market.checkout.referee.translate;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.referee.impl.TranslateService;

class TranslateServiceTest {

    public static final String LANG = "ru";

    @Test
    public void translate() {
        TranslateService translate = new TranslateService();
        String chinese = "欢迎光临！本款衬衫采用标准欧码，参见尺码图。长60厘米，腰围80厘米。如有任何疑问请咨询本店客服，谢谢";
        System.out.println(translate.translate(Arrays.asList(chinese), LANG));
    }

    @Test
    public void multiple() {
        TranslateService service = new TranslateService();
        List<String> text = Arrays.asList(
                "From global, load-balanced, resilient services to flexible single-instance VMs, we provide a scalable range of computing options you can tailor to match your needs. ",
                "Google Compute Engine provides highly customizable virtual machines with best-of-breed features, friendly pay-for-what-you-use pricing, and the option to deploy your code directly or via containers. ",
                "Google Kubernetes Engine lets you use fully-managed Kubernetes clusters to deploy, manage, and orchestrate containers at scale. ",
                "Google App Engine is a flexible platform-as-a-service that lets you focus on your code, freeing you from the operational details of deployment and infrastructure management. ",
                "Container Registry is a single place for your team to manage Docker images, perform vulnerability analysis, and decide who can access what with fine-grained access control. ",
                "Existing CI/CD integrations let you set up fully automated Docker pipelines to get fast feedback.");

        long st = System.currentTimeMillis();
        service.translate(text, "ru");
        System.out.println("time " + (System.currentTimeMillis() - st));
    }
}
