#!/bin/bash
javac -cp ../artifacts/*:../lib/* ru/ifmo/rain/krotkov/$1/*.java

java -cp . -p ../artifacts:../lib -m info.kgeorgiy.java.advanced.$1 $2 ru.ifmo.rain.krotkov.$1.$3 $4
