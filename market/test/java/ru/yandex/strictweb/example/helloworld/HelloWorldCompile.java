package ru.yandex.strictweb.example.helloworld;

import ru.yandex.strictweb.scriptjava.CommonCompiler;
import ru.yandex.strictweb.scriptjava.compiler.Compiler;

import java.nio.file.Path;
import java.nio.file.Paths;

// Запускайте этот файл из папочки strict-web: корневой папки проекта
public class HelloWorldCompile extends CommonCompiler {

    public static final String JS = "hello-world.js";

    public static void main(String[] args) throws Exception {
        String wwwRoot = "src/test/resources/ru/yandex/strictweb/example/www-root/";
        compile(Paths.get(wwwRoot).resolve(JS), args);
    }

    public static void compile(Path jsGenPath, String... args) throws Exception {
        new ru.yandex.strictweb.example.simples.Compiler()
                .setJsGenPath(jsGenPath.toString())
                .build(args);
    }

    @Override
    public void addToCompiler(Compiler compiler) throws Exception {
        // здесь перечисляем какие классы надо скомпилить
        compiler.parseClass(HelloWorld.class);
    }
}
