package ru.yandex.strictweb.example.simples;

import ru.yandex.strictweb.scriptjava.CommonCompiler;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Compiler extends CommonCompiler {

    public static final String JS = "simples.js";

    public static void main(String[] args) throws Exception {
        Path wwwRoot = Paths.get("src/test/resources/ru/yandex/strictweb/example/www-root/");
        compile(wwwRoot.resolve(JS), args);
    }

    public static void compile(Path jsGenPath, String... args) throws Exception {
        new Compiler()
                .setJsGenPath(jsGenPath.toString())
                .build(args);
    }

    @Override
    public void addToCompiler(ru.yandex.strictweb.scriptjava.compiler.Compiler compiler) throws Exception {
        compiler
                .parseClass(HelloWorld.class)
                .parseClass(FirstDiv.class)
                .parseClass(EventExample.class)
                .parseClass(TableTimerExample.class)
        ;
    }
}
