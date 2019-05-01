SET jar_dependences=hamcrest-core-1.3.jar;Implementor.jar;info.kgeorgiy.java.advanced.base.jar;info.kgeorgiy.java.advanced.implementor.jar;jsoup-1.8.1.jar;junit-4.11.jar;quickcheck-0.6.jar
SET link=https://docs.oracle.com/en/java/javase/11/docs/api
SET class=ru\ifmo\rain\krotkov\implementor\Implementor.java
SET module_path=..\implementor

javadoc -cp %jar_dependences%;C:\Users\krotk\JavaHW\HW2\src -author -version -link %link% -d docs\implementor\public %class% %module_path%\Impler.java %module_path%\JarImpler.java %module_path%\ImplerException.java

javadoc -cp %jar_dependences%;C:\Users\krotk\JavaHW\HW2\src -author -version -link %link% -d docs\implementor\private -private %class% %module_path%\Impler.java %module_path%\JarImpler.java %module_path%\ImplerException.java