SET jar_dependences=hamcrest-core-1.3.jar;info.kgeorgiy.java.advanced.implementor.jar;info.kgeorgiy.java.advanced.base.jar;jsoup-1.8.1.jar;junit-4.11.jar;quickcheck-0.6.jar

jar -c --file=Implementor.jar --module-path %jar_dependences% --main-class ru.ifmo.rain.krotkov.implementor.Implementor -C compiled_modules\implementor .