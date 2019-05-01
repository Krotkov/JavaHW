SET jar_dependences=hamcrest-core-1.3.jar;info.kgeorgiy.java.advanced.implementor.jar;info.kgeorgiy.java.advanced.base.jar;jsoup-1.8.1.jar;junit-4.11.jar;quickcheck-0.6.jar
SET module_dir=..\modules\ru.ifmo.rain.krotkov.implementor

copy ru\ifmo\rain\krotkov\implementor\Implementor.java %module_dir%\ru\ifmo\rain\krotkov\implementor

javac -d compiled_modules\implementor -p %jar_dependences% %module_dir%\module-info.java %module_dir%\ru\ifmo\rain\krotkov\implementor\Implementor.java