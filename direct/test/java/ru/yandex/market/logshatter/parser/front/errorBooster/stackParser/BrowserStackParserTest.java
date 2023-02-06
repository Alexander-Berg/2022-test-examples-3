package ru.yandex.market.logshatter.parser.front.errorBooster.stackParser;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class BrowserStackParserTest {
    private static final TestData[] ERRORS = {
        // YaBro 19.6 desktop
        new TestData(
            "TypeError: c184255802633 is not a function\n" +
                "    at https://pass.yandex.ru/accounts?callback=c184255802633&yu=9483396071562518328:1:1",
            new StackFrame("(anonymous)", "https://pass.yandex.ru/accounts?callback=c184255802633&yu=9483396071562518328", 1, 1)
        ),

        // YaBro 19.6 desktop
        new TestData(
            "TypeError: Cannot read property 'call' of undefined\n" +
                "    at a (https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:607)\n" +
                "    at Module.25 (https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:27492)\n" +
                "    at a (https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:607)\n" +
                "    at Object.1008 (https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:1666)\n" +
                "    at a (https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:607)\n" +
                "    at r (https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:474)\n" +
                "    at https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:1606\n" +
                "    at https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:1610\n" +
                "    at a (https://yandex.ru/search/?text=%D0%BA%D0%B0%D1%80%D0%BF%D0%BE%D0%B2%D1%8B%D0%B5&lr=213&clid=1955454&win=158:1:551251)\n" +
                "    at r (https://yandex.ru/search/?text=%D0%BA%D0%B0%D1%80%D0%BF%D0%BE%D0%B2%D1%8B%D0%B5&lr=213&clid=1955454&win=158:1:551474)",
            new StackFrame("a", "https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", 1, 607),
            new StackFrame("Module.25", "https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", 1, 27492),
            new StackFrame("a", "https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", 1, 607),
            new StackFrame("Object.1008", "https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", 1, 1666),
            new StackFrame("a", "https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", 1, 607),
            new StackFrame("r", "https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", 1, 474),
            new StackFrame("(anonymous)", "https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", 1, 1606),
            new StackFrame("(anonymous)", "https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", 1, 1610),
            new StackFrame("a", "https://yandex.ru/search/?text=%D0%BA%D0%B0%D1%80%D0%BF%D0%BE%D0%B2%D1%8B%D0%B5&lr=213&clid=1955454&win=158", 1, 551251),
            new StackFrame("r", "https://yandex.ru/search/?text=%D0%BA%D0%B0%D1%80%D0%BF%D0%BE%D0%B2%D1%8B%D0%B5&lr=213&clid=1955454&win=158", 1, 551474)
        ),

        // YaBro 19.6 desktop
        new TestData(
            "TypeError: Cannot read property 'contains' of null\n" +
                "    at z.fitToViewport (https://api-maps.yandex.ru/2.1.74/combine.js?callback_prefix=__jsonp_ymaps_combine&mode=release&flags=&load=101415161718192021222324252627282940424344454663707172737475767778799394959697$0,0.w.l)N8a1_1)1i.D9(9)_R8e),)L)48b)h1$1Q1,)Y(31z6L)l049,2a2d9*9!2b_M8f,x,R)$1j1()r.g)p)w,C006H6M)i)c6G1Z_O1V)!09)o05(6)96q)1(:():9.h6I)2)0,g,o(,;-,u,h,p,r,l.k,s,t9H9E9O9L9$,38g9_9U2c9F069T9P9S1I019M9N.q3c$R9:0Q9R9V(;0S,21W,e(!)80j)5(5.j.i.z2j2)2.2i2z0R0P0,0),a0$($(-(.(_6s!9.T.!.Y.3.7.02D2$);2!2Q2_(Q(z2B2I2V2H2y2J2U)V2T2L2E3d2W2-2F3e2X2G)I,b6w.W.V.U.16!.).(.*.26*.v.6.5.4.$.9.8:F):(82M2R2K)-2Y5d2N1J2S2O2A4J5@2P)Z6))y.;.@.:.,6,.X.u..._.-;Y,P)_)x)X)A_;6N.s6(_J:E,w6:1X1Y_:_I_@.t.m.r;X3b_Q_z(q,y.n.o.p2Z_i)3)@,Y2C;u@u4H4s034P4r4K020-7Z4L4M4N(44R2@2q2;2:(*)P)H)G$)08)D)F)e$!6r6x0d).)E)g)d7A,F7x4v4F_F4X4Y*d_H)m5q0!_N7F(@)z_G*e*c*r_E)C5j_Z1U0a!;*g*l*k*h*p*m*b_K6u7)1T7P7R7U7I7X7L7J7B0z8d7D7C7y6m6y6E,d(j(24i1s:f:g)7,S7O$10(0c0b*n!.!,!)!!*a!J!i!:::$j7z)R:!0_7W7T7V_P)*0:0D7N7Y077M7$7-7_7.7(7,7:7;7@7q7j7i7Q:*(Z6D0@5i6k6l0X1r1c1t1O1u0Z:T:e_S)a,A7G,i,*,!,:,;,j,z,q.c0*!@!_!-:V!(!*:D!Q!z!j!q;*;.;!$*)T)S;9:U6o:.0f0g0l0o0m0B$,:,:764:3320)\n" +
                "    at z._checkAutoFit (https://api-maps.yandex.ru/2.1.74/combine.js?callback_prefix=__jsonp_ymaps_combine&mode=release&flags=&load=101415161718192021222324252627282940424344454663707172737475767778799394959697$0,0.w.l)N8a1_1)1i.D9(9)_R8e),)L)48b)h1$1Q1,)Y(31z6L)l049,2a2d9*9!2b_M8f,x,R)$1j1()r.g)p)w,C006H6M)i)c6G1Z_O1V)!09)o05(6)96q)1(:():9.h6I)2)0,g,o(,;-,u,h,p,r,l.k,s,t9H9E9O9L9$,38g9_9U2c9F069T9P9S1I019M9N.q3c$R9:0Q9R9V(;0S,21W,e(!)80j)5(5.j.i.z2j2)2.2i2z0R0P0,0),a0$($(-(.(_6s!9.T.!.Y.3.7.02D2$);2!2Q2_(Q(z2B2I2V2H2y2J2U)V2T2L2E3d2W2-2F3e2X2G)I,b6w.W.V.U.16!.).(.*.26*.v.6.5.4.$.9.8:F):(82M2R2K)-2Y5d2N1J2S2O2A4J5@2P)Z6))y.;.@.:.,6,.X.u..._.-;Y,P)_)x)X)A_;6N.s6(_J:E,w6:1X1Y_:_I_@.t.m.r;X3b_Q_z(q,y.n.o.p2Z_i)3)@,Y2C;u@u4H4s034P4r4K020-7Z4L4M4N(44R2@2q2;2:(*)P)H)G$)08)D)F)e$!6r6x0d).)E)g)d7A,F7x4v4F_F4X4Y*d_H)m5q0!_N7F(@)z_G*e*c*r_E)C5j_Z1U0a!;*g*l*k*h*p*m*b_K6u7)1T7P7R7U7I7X7L7J7B0z8d7D7C7y6m6y6E,d(j(24i1s:f:g)7,S7O$10(0c0b*n!.!,!)!!*a!J!i!:::$j7z)R:!0_7W7T7V_P)*0:0D7N7Y077M7$7-7_7.7(7,7:7;7@7q7j7i7Q:*(Z6D0@5i6k6l0X1r1c1t1O1u0Z:T:e_S)a,A7G,i,*,!,:,;,j,z,q.c0*!@!_!-:V!(!*:D!Q!z!j!q;*;.;!$*)T)S;9:U6o:.0f0g0l0o0m0B$,:,:764:7677)\n" +
                "    at z.<anonymous> (https://api-maps.yandex.ru/2.1.74/combine.js?callback_prefix=__jsonp_ymaps_combine&mode=release&flags=&load=101415161718192021222324252627282940424344454663707172737475767778799394959697$0,0.w.l)N8a1_1)1i.D9(9)_R8e),)L)48b)h1$1Q1,)Y(31z6L)l049,2a2d9*9!2b_M8f,x,R)$1j1()r.g)p)w,C006H6M)i)c6G1Z_O1V)!09)o05(6)96q)1(:():9.h6I)2)0,g,o(,;-,u,h,p,r,l.k,s,t9H9E9O9L9$,38g9_9U2c9F069T9P9S1I019M9N.q3c$R9:0Q9R9V(;0S,21W,e(!)80j)5(5.j.i.z2j2)2.2i2z0R0P0,0),a0$($(-(.(_6s!9.T.!.Y.3.7.02D2$);2!2Q2_(Q(z2B2I2V2H2y2J2U)V2T2L2E3d2W2-2F3e2X2G)I,b6w.W.V.U.16!.).(.*.26*.v.6.5.4.$.9.8:F):(82M2R2K)-2Y5d2N1J2S2O2A4J5@2P)Z6))y.;.@.:.,6,.X.u..._.-;Y,P)_)x)X)A_;6N.s6(_J:E,w6:1X1Y_:_I_@.t.m.r;X3b_Q_z(q,y.n.o.p2Z_i)3)@,Y2C;u@u4H4s034P4r4K020-7Z4L4M4N(44R2@2q2;2:(*)P)H)G$)08)D)F)e$!6r6x0d).)E)g)d7A,F7x4v4F_F4X4Y*d_H)m5q0!_N7F(@)z_G*e*c*r_E)C5j_Z1U0a!;*g*l*k*h*p*m*b_K6u7)1T7P7R7U7I7X7L7J7B0z8d7D7C7y6m6y6E,d(j(24i1s:f:g)7,S7O$10(0c0b*n!.!,!)!!*a!J!i!:::$j7z)R:!0_7W7T7V_P)*0:0D7N7Y077M7$7-7_7.7(7,7:7;7@7q7j7i7Q:*(Z6D0@5i6k6l0X1r1c1t1O1u0Z:T:e_S)a,A7G,i,*,!,:,;,j,z,q.c0*!@!_!-:V!(!*:D!Q!z!j!q;*;.;!$*)T)S;9:U6o:.0f0g0l0o0m0B$,:,:764:7278)",
            new StackFrame("z.fitToViewport", "https://api-maps.yandex.ru/2.1.74/combine.js?callback_prefix=__jsonp_ymaps_combine&mode=release&flags=&load=101415161718192021222324252627282940424344454663707172737475767778799394959697$0,0.w.l)N8a1_1)1i.D9(9)_R8e),)L)48b)h1$1Q1,)Y(31z6L)l049,2a2d9*9!2b_M8f,x,R)$1j1()r.g)p)w,C006H6M)i)c6G1Z_O1V)!09)o05(6)96q)1(:():9.h6I)2)0,g,o(,;-,u,h,p,r,l.k,s,t9H9E9O9L9$,38g9_9U2c9F069T9P9S1I019M9N.q3c$R9:0Q9R9V(;0S,21W,e(!)80j)5(5.j.i.z2j2)2.2i2z0R0P0,0),a0$($(-(.(_6s!9.T.!.Y.3.7.02D2$);2!2Q2_(Q(z2B2I2V2H2y2J2U)V2T2L2E3d2W2-2F3e2X2G)I,b6w.W.V.U.16!.).(.*.26*.v.6.5.4.$.9.8:F):(82M2R2K)-2Y5d2N1J2S2O2A4J5@2P)Z6))y.;.@.:.,6,.X.u..._.-;Y,P)_)x)X)A_;6N.s6(_J:E,w6:1X1Y_:_I_@.t.m.r;X3b_Q_z(q,y.n.o.p2Z_i)3)@,Y2C;u@u4H4s034P4r4K020-7Z4L4M4N(44R2@2q2;2:(*)P)H)G$)08)D)F)e$!6r6x0d).)E)g)d7A,F7x4v4F_F4X4Y*d_H)m5q0!_N7F(@)z_G*e*c*r_E)C5j_Z1U0a!;*g*l*k*h*p*m*b_K6u7)1T7P7R7U7I7X7L7J7B0z8d7D7C7y6m6y6E,d(j(24i1s:f:g)7,S7O$10(0c0b*n!.!,!)!!*a!J!i!:::$j7z)R:!0_7W7T7V_P)*0:0D7N7Y077M7$7-7_7.7(7,7:7;7@7q7j7i7Q:*(Z6D0@5i6k6l0X1r1c1t1O1u0Z:T:e_S)a,A7G,i,*,!,:,;,j,z,q.c0*!@!_!-:V!(!*:D!Q!z!j!q;*;.;!$*)T)S;9:U6o:.0f0g0l0o0m0B$,:,", 764, 3320),
            new StackFrame("z._checkAutoFit", "https://api-maps.yandex.ru/2.1.74/combine.js?callback_prefix=__jsonp_ymaps_combine&mode=release&flags=&load=101415161718192021222324252627282940424344454663707172737475767778799394959697$0,0.w.l)N8a1_1)1i.D9(9)_R8e),)L)48b)h1$1Q1,)Y(31z6L)l049,2a2d9*9!2b_M8f,x,R)$1j1()r.g)p)w,C006H6M)i)c6G1Z_O1V)!09)o05(6)96q)1(:():9.h6I)2)0,g,o(,;-,u,h,p,r,l.k,s,t9H9E9O9L9$,38g9_9U2c9F069T9P9S1I019M9N.q3c$R9:0Q9R9V(;0S,21W,e(!)80j)5(5.j.i.z2j2)2.2i2z0R0P0,0),a0$($(-(.(_6s!9.T.!.Y.3.7.02D2$);2!2Q2_(Q(z2B2I2V2H2y2J2U)V2T2L2E3d2W2-2F3e2X2G)I,b6w.W.V.U.16!.).(.*.26*.v.6.5.4.$.9.8:F):(82M2R2K)-2Y5d2N1J2S2O2A4J5@2P)Z6))y.;.@.:.,6,.X.u..._.-;Y,P)_)x)X)A_;6N.s6(_J:E,w6:1X1Y_:_I_@.t.m.r;X3b_Q_z(q,y.n.o.p2Z_i)3)@,Y2C;u@u4H4s034P4r4K020-7Z4L4M4N(44R2@2q2;2:(*)P)H)G$)08)D)F)e$!6r6x0d).)E)g)d7A,F7x4v4F_F4X4Y*d_H)m5q0!_N7F(@)z_G*e*c*r_E)C5j_Z1U0a!;*g*l*k*h*p*m*b_K6u7)1T7P7R7U7I7X7L7J7B0z8d7D7C7y6m6y6E,d(j(24i1s:f:g)7,S7O$10(0c0b*n!.!,!)!!*a!J!i!:::$j7z)R:!0_7W7T7V_P)*0:0D7N7Y077M7$7-7_7.7(7,7:7;7@7q7j7i7Q:*(Z6D0@5i6k6l0X1r1c1t1O1u0Z:T:e_S)a,A7G,i,*,!,:,;,j,z,q.c0*!@!_!-:V!(!*:D!Q!z!j!q;*;.;!$*)T)S;9:U6o:.0f0g0l0o0m0B$,:,", 764, 7677),
            new StackFrame("z.<anonymous>", "https://api-maps.yandex.ru/2.1.74/combine.js?callback_prefix=__jsonp_ymaps_combine&mode=release&flags=&load=101415161718192021222324252627282940424344454663707172737475767778799394959697$0,0.w.l)N8a1_1)1i.D9(9)_R8e),)L)48b)h1$1Q1,)Y(31z6L)l049,2a2d9*9!2b_M8f,x,R)$1j1()r.g)p)w,C006H6M)i)c6G1Z_O1V)!09)o05(6)96q)1(:():9.h6I)2)0,g,o(,;-,u,h,p,r,l.k,s,t9H9E9O9L9$,38g9_9U2c9F069T9P9S1I019M9N.q3c$R9:0Q9R9V(;0S,21W,e(!)80j)5(5.j.i.z2j2)2.2i2z0R0P0,0),a0$($(-(.(_6s!9.T.!.Y.3.7.02D2$);2!2Q2_(Q(z2B2I2V2H2y2J2U)V2T2L2E3d2W2-2F3e2X2G)I,b6w.W.V.U.16!.).(.*.26*.v.6.5.4.$.9.8:F):(82M2R2K)-2Y5d2N1J2S2O2A4J5@2P)Z6))y.;.@.:.,6,.X.u..._.-;Y,P)_)x)X)A_;6N.s6(_J:E,w6:1X1Y_:_I_@.t.m.r;X3b_Q_z(q,y.n.o.p2Z_i)3)@,Y2C;u@u4H4s034P4r4K020-7Z4L4M4N(44R2@2q2;2:(*)P)H)G$)08)D)F)e$!6r6x0d).)E)g)d7A,F7x4v4F_F4X4Y*d_H)m5q0!_N7F(@)z_G*e*c*r_E)C5j_Z1U0a!;*g*l*k*h*p*m*b_K6u7)1T7P7R7U7I7X7L7J7B0z8d7D7C7y6m6y6E,d(j(24i1s:f:g)7,S7O$10(0c0b*n!.!,!)!!*a!J!i!:::$j7z)R:!0_7W7T7V_P)*0:0D7N7Y077M7$7-7_7.7(7,7:7;7@7q7j7i7Q:*(Z6D0@5i6k6l0X1r1c1t1O1u0Z:T:e_S)a,A7G,i,*,!,:,;,j,z,q.c0*!@!_!-:V!(!*:D!Q!z!j!q;*;.;!$*)T)S;9:U6o:.0f0g0l0o0m0B$,:,", 764, 7278)
        ),

        // YaBro 19.6 iPhone
        new TestData(
            "global code@https://yandex.ru/search/?text=%D0%BA%D0%BE%D1%80%D0%B8%D0%BB%D1%83%D1%81&lr=21656:1:9",
            new StackFrame("global code", "https://yandex.ru/search/?text=%D0%BA%D0%BE%D1%80%D0%B8%D0%BB%D1%83%D1%81&lr=21656", 1, 9)
        ),

        // Chrome 69 exotic stack
        new TestData(
            "TypeError: Cannot read property 'style' of null\n" +
                "    at <anonymous>:1:46\n" +
                "    at <anonymous>:1:126",
            new StackFrame("(anonymous)", "<anonymous>", 1, 46),
            new StackFrame("(anonymous)", "<anonymous>", 1, 126)
        ),

        // Chrome Mobile 61 exotic stack
        new TestData(
            "RangeError: Maximum call stack size exceeded"
        ),

        // Chrome 36
        new TestData(
            "Error: Default error\n" +
                "    at dumpExceptionError (http://localhost:8080/file.js:41:27)\n" +
                "    at HTMLButtonElement.onclick (http://localhost:8080/file.js:107:146)\n" +
                "    at I.e.fn.(anonymous function) [as index] (http://localhost:8080/file.js:10:3651)",
            new StackFrame("dumpExceptionError", "http://localhost:8080/file.js", 41, 27),
            new StackFrame("HTMLButtonElement.onclick", "http://localhost:8080/file.js", 107, 146),
            new StackFrame("I.e.fn.(anonymous function) [as index]", "http://localhost:8080/file.js", 10, 3651)
        ),

        /*
        // Chrome, generated by Webpack with { devtool: eval }
        new TestData(
            "TypeError: Cannot read property 'error' of undefined\n" +
                "   at Test.eval(webpack:///./src/components/test/test.jsx?:295:108)\n" +
                "   at Test.render(webpack:///./src/components/test/test.jsx?:272:32)\n" +
                "   at Test.tryRender(webpack:///./~/react-transform-catch-errors/lib/index.js?:34:31)\n" +
                "   at Test.proxiedMethod(webpack:///./~/react-proxy/modules/createPrototypeProxy.js?:44:30)",
            new StackFrame("?", "?", 295, 108),
            new StackFrame("?", "?", 272, 32),
            new StackFrame("?", "?", 34, 31),
            new StackFrame("?", "?", 44, 30)
        ),
         */

        // Edge 17
        new TestData(
            "Error: Отказано в доступе.\r\n" +
                "\n" +
                "   at Anonymous function (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:23815)\n" +
                "   at k.prototype.tryCatch (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:8997)\n" +
                "   at k.prototype.exec (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:9522)\n" +
                "   at C.prototype.defaultBody (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:19766)\n" +
                "   at M.prototype.run (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:13520)\n" +
                "   at B.prototype.runOne (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:17897)\n" +
                "   at B.prototype._run (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:16417)\n" +
                "   at A.prototype.runMany (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:20374)\n" +
                "   at B.prototype._run (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:16417)\n" +
                "   at B.prototype.renderContent (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:18460)",
            new StackFrame("Anonymous function", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 23815),
            new StackFrame("k.prototype.tryCatch", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 8997),
            new StackFrame("k.prototype.exec", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 9522),
            new StackFrame("C.prototype.defaultBody", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 19766),
            new StackFrame("M.prototype.run", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 13520),
            new StackFrame("B.prototype.runOne", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 17897),
            new StackFrame("B.prototype._run", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 16417),
            new StackFrame("A.prototype.runMany", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 20374),
            new StackFrame("B.prototype._run", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 16417),
            new StackFrame("B.prototype.renderContent", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 2, 18460)
        ),

        // Edge 17
        new TestData(
            "ReferenceError: Set is not defined\n" +
                "    at https://yastatic.net/react/16.8.4/react-with-dom.min.js:235:37\n" +
                "    at https://yastatic.net/react/16.8.4/react-with-dom.min.js:45:194\n" +
                "    at https://yastatic.net/react/16.8.4/react-with-dom.min.js:45:208",
            new StackFrame("(anonymous)", "https://yastatic.net/react/16.8.4/react-with-dom.min.js", 235, 37),
            new StackFrame("(anonymous)", "https://yastatic.net/react/16.8.4/react-with-dom.min.js", 45, 194),
            new StackFrame("(anonymous)", "https://yastatic.net/react/16.8.4/react-with-dom.min.js", 45, 208)
        ),

        // Safari
        new TestData(
            "global code@https://yandex.ru/search/touch/?text=%D1%81%D0%BC%D1%81&clid=2160746&brorich=1&lr=6:1:9",
            new StackFrame("global code", "https://yandex.ru/search/touch/?text=%D1%81%D0%BC%D1%81&clid=2160746&brorich=1&lr=6", 1, 9)
        ),

        // Safari
        new TestData(
            "https://yastatic.net/s3/web4static/_/UmwZnggdD_5M7ZnbXT1FHUXf-tg.js:1:1931\n" +
                "a@https://yastatic.net/s3/web4static/_/UmwZnggdD_5M7ZnbXT1FHUXf-tg.js:1:701\n" +
                "o@https://yandex.ru/yandsearch?text=priceva&lr=213&numdoc=50:1:936671\n" +
                "global code@https://yastatic.net/s3/web4static/_/UmwZnggdD_5M7ZnbXT1FHUXf-tg.js:1:10",
            new StackFrame("(anonymous)", "https://yastatic.net/s3/web4static/_/UmwZnggdD_5M7ZnbXT1FHUXf-tg.js", 1, 1931),
            new StackFrame("a", "https://yastatic.net/s3/web4static/_/UmwZnggdD_5M7ZnbXT1FHUXf-tg.js", 1, 701),
            new StackFrame("o", "https://yandex.ru/yandsearch?text=priceva&lr=213&numdoc=50", 1, 936671),
            new StackFrame("global code", "https://yastatic.net/s3/web4static/_/UmwZnggdD_5M7ZnbXT1FHUXf-tg.js", 1, 10)
        ),

        // Safari
        new TestData(
            "insertRule@[native code]\n" +
                "f@https://yandex.ru/touchsearch?text=%D0%BF%D0%BE%D1%80%D0%BD%D0%BE&clid=2039514&lr=74&redircnt=1562507273.1:1:341",
            new StackFrame("insertRule", "[native code]", 0, 0),
            new StackFrame("f", "https://yandex.ru/touchsearch?text=%D0%BF%D0%BE%D1%80%D0%BD%D0%BE&clid=2039514&lr=74&redircnt=1562507273.1", 1, 341)
        ),

        // Firefox 67
        new TestData(
            "tms_8942b256_f564_4dc6_90d0_ece09d39680d/</<@moz-extension://6845592d-9575-4905-aab3-577751b824f5/userscript.html?id=8942b256-f564-4dc6-90d0-ece09d39680d:994:13\n" +
                "g</<@eval:2:481\n" +
                "ja/c[d]@eval line 1 > Function:52:483\n",
            new StackFrame("tms_8942b256_f564_4dc6_90d0_ece09d39680d/</<", "moz-extension://6845592d-9575-4905-aab3-577751b824f5/userscript.html?id=8942b256-f564-4dc6-90d0-ece09d39680d", 994, 13),
            new StackFrame("g</<", "eval", 2, 481),
            new StackFrame("ja/c[d]", "eval line 1 > Function", 52, 483)
        ),

        // Firefox 67
        new TestData(
            ".onSetMod.js@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:47198\n" +
                "this.BEM<.decl/</n[t]@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:8779\n" +
                "i/n[r]</o@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:1075\n" +
                "this.BEM<._callModFn@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:8025\n",
            new StackFrame(".onSetMod.js", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 47198),
            new StackFrame("this.BEM<.decl/</n[t]", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 8779),
            new StackFrame("i/n[r]</o", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 1075),
            new StackFrame("this.BEM<._callModFn", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 8025)
        ),

        // Firefox 60
        new TestData(
            "isWithinWindow@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:250060\n" +
                "loadImages@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:249333\n" +
                "decl/</n[t]@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:8781\n" +
                "o@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:1077\n" +
                "_onScroll@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:248936\n" +
                "decl/</n[t]@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:8781\n" +
                "o@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:1077\n" +
                "bindEvents/this.handler<@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:248662\n" +
                "r/<@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:153486\n" +
                "r@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:153543\n" +
                "a@https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:3:14529\n" +
                "dispatch@https://yastatic.net/jquery/2.1.4/jquery.min.js:3:6466\n" +
                "add/r.handle@https://yastatic.net/jquery/2.1.4/jquery.min.js:3:3241\n",
            new StackFrame("isWithinWindow", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 250060),
            new StackFrame("loadImages", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 249333),
            new StackFrame("decl/</n[t]", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 8781),
            new StackFrame("o", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 1077),
            new StackFrame("_onScroll", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 248936),
            new StackFrame("decl/</n[t]", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 8781),
            new StackFrame("o", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 1077),
            new StackFrame("bindEvents/this.handler<", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 248662),
            new StackFrame("r/<", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 153486),
            new StackFrame("r", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 153543),
            new StackFrame("a", "https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", 3, 14529),
            new StackFrame("dispatch", "https://yastatic.net/jquery/2.1.4/jquery.min.js", 3, 6466),
            new StackFrame("add/r.handle", "https://yastatic.net/jquery/2.1.4/jquery.min.js", 3, 3241)
        ),

        // Firefox 60 stack with @ sign in url
        new TestData(
            "who@http://localhost:5000/misc/@stuff/foo.js:3:9\n" +
                "what@http://localhost:5000/misc/@stuff/foo.js:6:3\n" +
                "where@http://localhost:5000/misc/@stuff/foo.js:9:3\n" +
                "why@http://localhost:5000/misc/@stuff/foo.js:12:3\n" +
                "@http://localhost:5000/misc/@stuff/foo.js:15:1\n",
            new StackFrame("who", "http://localhost:5000/misc/@stuff/foo.js", 3, 9),
            new StackFrame("what", "http://localhost:5000/misc/@stuff/foo.js", 6, 3),
            new StackFrame("where", "http://localhost:5000/misc/@stuff/foo.js", 9, 3),
            new StackFrame("why", "http://localhost:5000/misc/@stuff/foo.js", 12, 3),
            new StackFrame("(anonymous)", "http://localhost:5000/misc/@stuff/foo.js", 15, 1)
        ),

        /*
        // Firefox 60 stack with @ sign in url and function name
        new TestData(
            "obj[\"@who\"]@http://localhost:5000/misc/@stuff/foo.js:4:9\n" +
                "what@http://localhost:5000/misc/@stuff/foo.js:8:3\n" +
                "where@http://localhost:5000/misc/@stuff/foo.js:11:3\n" +
                "why@http://localhost:5000/misc/@stuff/foo.js:14:3\n" +
                "@http://localhost:5000/misc/@stuff/foo.js:17:1\n",
            new StackFrame("obj[\"@who\"]", "http://localhost:5000/misc/@stuff/foo.js", 4, 9),
            new StackFrame("what", "http://localhost:5000/misc/@stuff/foo.js", 8, 3),
            new StackFrame("where", "http://localhost:5000/misc/@stuff/foo.js", 11, 3),
            new StackFrame("why", "http://localhost:5000/misc/@stuff/foo.js", 14, 3),
            new StackFrame("(anonymous)", "http://localhost:5000/misc/@stuff/foo.js", 17, 1)
        ),

        // Firefox 43 stack with @ sign in function name
        new TestData(
            "obj[\"@fn\"]@Scratchpad/1:10:29\n" +
                "@Scratchpad/1:11:1\n",
            new StackFrame("obj[\"@fn\"]", "Scratchpad/1", 10, 29),
            new StackFrame("(anonymous)", "Scratchpad/1", 11, 1)
        ),
         */

        // Firefox 43 eval
        new TestData(
            "baz@http://localhost:8080/file.js line 26 > eval line 2 > eval:1:30\n" +
                "foo@http://localhost:8080/file.js line 26 > eval:2:96\n" +
                "@http://localhost:8080/file.js line 26 > eval:4:18\n" +
                "speak@http://localhost:8080/file.js:26:17\n" +
                "@http://localhost:8080/file.js:33:9",
            new StackFrame("baz", "http://localhost:8080/file.js line 26 > eval line 2 > eval", 1, 30),
            new StackFrame("foo", "http://localhost:8080/file.js line 26 > eval", 2, 96),
            new StackFrame("(anonymous)", "http://localhost:8080/file.js line 26 > eval", 4, 18),
            new StackFrame("speak", "http://localhost:8080/file.js", 26, 17),
            new StackFrame("(anonymous)", "http://localhost:8080/file.js", 33, 9)
        )
    };

    @Test
    public void parseStack() {
        for (TestData test : ERRORS) {
            test.assertStack();
        }
    }

    @Test
    public void parseAtNameUrlLineColFormat() {
        StackFrame frame;

        frame = BrowserStackParser.parseAtNameUrlLineColFormat(
            "    at Module.25 (https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:27492)"
        );

        assertNotNull(frame);
        assertEquals("Module.25", frame.getName());
        assertEquals("https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", frame.getUrl());
        assertEquals(1, frame.getLine());
        assertEquals(27492, frame.getCol());

        frame = BrowserStackParser.parseAtNameUrlLineColFormat(
            "   at Anonymous function (https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js:2:23815)"
        );

        assertNotNull(frame);
        assertEquals("Anonymous function", frame.getName());
        assertEquals("https://yastatic.net/s3/web4static/_/nllwpm9JrFkxqQgmsRmldwz2HtM.js", frame.getUrl());
        assertEquals(2, frame.getLine());
        assertEquals(23815, frame.getCol());
    }

    @Test
    public void parseAtUrlLineColFormat() {
        StackFrame frame = BrowserStackParser.parseAtUrlLineColFormat(
            "    at https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js:1:1606"
        );

        assertNotNull(frame);
        assertEquals("(anonymous)", frame.getName());
        assertEquals("https://yastatic.net/s3/web4static/_/tcILZXlwCEeLX4M_irsbUyG-CZQ.js", frame.getUrl());
        assertEquals(1, frame.getLine());
        assertEquals(1606, frame.getCol());
    }

    @Test
    public void parseNameUrlLineColFormat() {
        StackFrame frame;

        frame = BrowserStackParser.parseNameUrlLineColFormat(
            "global code@https://yandex.ru/search/?text=%D0%BA%D0%BE%D1%80%D0%B8%D0%BB%D1%83%D1%81&lr=21656:1:9"
        );

        assertNotNull(frame);
        assertEquals("global code", frame.getName());
        assertEquals("https://yandex.ru/search/?text=%D0%BA%D0%BE%D1%80%D0%B8%D0%BB%D1%83%D1%81&lr=21656", frame.getUrl());
        assertEquals(1, frame.getLine());
        assertEquals(9, frame.getCol());

        frame = BrowserStackParser.parseNameUrlLineColFormat(
            "insertRule@[native code]"
        );

        assertNotNull(frame);
        assertEquals("insertRule", frame.getName());
        assertEquals("[native code]", frame.getUrl());
        assertEquals(0, frame.getLine());
        assertEquals(0, frame.getCol());

        frame = BrowserStackParser.parseNameUrlLineColFormat(
            "@http://localhost:8080/file.js:33:9"
        );

        assertNotNull(frame);
        assertEquals("(anonymous)", frame.getName());
        assertEquals("http://localhost:8080/file.js", frame.getUrl());
        assertEquals(33, frame.getLine());
        assertEquals(9, frame.getCol());
    }

    @Test
    public void parseUrlLineColFormat() {
        StackFrame frame;

        frame = BrowserStackParser.parseUrlLineColFormat(
            "https://yastatic.net/s3/web4static/_/UmwZnggdD_5M7ZnbXT1FHUXf-tg.js:1:1931"
        );

        assertNotNull(frame);
        assertEquals("(anonymous)", frame.getName());
        assertEquals("https://yastatic.net/s3/web4static/_/UmwZnggdD_5M7ZnbXT1FHUXf-tg.js", frame.getUrl());
        assertEquals(1, frame.getLine());
        assertEquals(1931, frame.getCol());
    }

    static class TestData {
        private final String stack;
        private final StackFrame[] frames;

        TestData(String stack, StackFrame... frames) {
            this.stack = stack;
            this.frames = frames;
        }

        void assertStack() {
            StackFrame[] actualFrames = new BrowserStackParser(stack).getStackFrames();
            assertArrayEquals(
                "Error parsing stack. Original stack:\n" +
                    "-----\n" +
                    stack + "\n" +
                    "-----\n" +
                    "Parsed stack:\n" +
                    "-----\n" +
                    StringUtils.join(actualFrames, '\n') + "\n" +
                    "-----\n" +
                    "Assertion message",
                frames,
                actualFrames
            );
        }
    }
}
