package ru.yandex.strictweb.example.sampleajax;

import org.jetbrains.annotations.NotNull;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import ru.yandex.strictweb.ajaxtools.AjaxService;
import ru.yandex.strictweb.ajaxtools.BeanProvider;
import ru.yandex.strictweb.scriptjava.CommonCompiler;
import ru.yandex.strictweb.scriptjava.base.ajax.Ajax;
import ru.yandex.strictweb.scriptjava.compiler.Compiler;
import ru.yandex.strictweb.scriptjava.plugins.AjaxServiceHelperCompilerPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;

// Запускайте этот файл из папочки strict-web: корневой папки проекта
public class SampleAjaxCompile extends CommonCompiler {

    public static final String JS = "sample-ajax.js";

    public static void main(String[] args) throws Exception {
        String wwwRoot = "src/test/resources/ru/yandex/strictweb/example/www-root/";
        compile(Paths.get(wwwRoot).resolve(JS), args);

        Server server = createServer(3128, wwwRoot);

        server.start();
        server.join();
    }

    public static void compile(Path jsGenPath, String... args) throws Exception {
        new ru.yandex.strictweb.example.simples.Compiler()
                .setJsGenPath(jsGenPath.toString())
                .build(args);
    }

    @NotNull
    public static Server createServer(int port, String wwwRoot) {
        Server server = new Server(port);
        Context context = new Context();
        context.setContextPath("/");
        context.setResourceBase(wwwRoot);
        context.addServlet(DefaultServlet.class, "/");

        AjaxService ajaxService = new AjaxService();
        ajaxService.setBeanProvider(beanName -> {
            try {
                beanName = Character.toUpperCase(beanName.charAt(0)) + beanName.substring(1);
                return Class.forName(SampleHelperBean.class.getPackage().getName() + "." + beanName).newInstance();
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }
        });
        context.addServlet(new ServletHolder(ajaxService), "/ajax");

        server.addHandler(context);
        return server;
    }

    @Override
    public void addToCompiler(Compiler compiler) throws Exception {
        Ajax.prepareCompiler(compiler);

        compiler.addPlugin(new AjaxServiceHelperCompilerPlugin());

        compiler
                .parseClass(SampleHelperBean.class)
                .parseClass(SampleUiForm.class)
                .parseClass(SomeModel.class)
        ;
    }
}
