SET jar_dependences=hamcrest-core-1.3.jar;info.kgeorgiy.java.advanced.implementor.jar;info.kgeorgiy.java.advanced.base.jar;jsoup-1.8.1.jar;junit-4.11.jar;quickcheck-0.6.jar
SET salt=%~1
java -p %jar_dependences%;compiled_modules\implementor  --add-modules ru.ifmo.rain.krotkov.implementor -m info.kgeorgiy.java.advanced.implementor jar-class ru.ifmo.rain.krotkov.implementor.Implementor %salt%